package spatial.transform

import argon._
import argon.node._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.retiming._
import spatial.traversal.AccelTraversal

case class AccumTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inAccel{ transformControl(lhs,rhs) }
    case _ => transformControl(lhs,rhs)
  }

  def transformControl[A:Type](lhs: Sym[A], rhs: Op[A]): Sym[A] = {
    if (inHw && rhs.blocks.nonEmpty) dbgs(stm(lhs))

    rhs match {
      case ctrl: Control[_] if !(lhs.isSwitch && lhs.isInnerControl) =>
        ctrl.bodies.foreach{body =>
          if (body.isInnerStage || lhs.isInnerControl) {
            body.blocks.foreach{case (_, block) =>
              register(block -> optimizeAccumulators(block))
            }
          }
        }
        super.transform(lhs,rhs)
      case _ => super.transform(lhs,rhs)
    }
  }


  def regAccumOp[A](reg: Reg[_], data: Bits[A], first: Bit, ens: Set[Bit], op: Accum, invert: Boolean): A = {
    implicit val A: Bits[A] = data.selfType
    val trueFirst = if (invert) !first else first
    stage(RegAccumOp(reg.asInstanceOf[Reg[A]],data,ens,op,trueFirst))
  }
  def regAccumFMA[A](reg: Reg[_], m0: Bits[A], m1: Bits[_], first: Bit, ens: Set[Bit], invert: Boolean): A = {
    implicit val A: Bits[A] = m0.selfType
    val trueFirst = if (invert) !first else first
    stage(RegAccumFMA(reg.asInstanceOf[Reg[A]],m0,m1.asInstanceOf[Bits[A]],ens,trueFirst))
  }

  def optimizeAccumulators[R](block: Block[R]): Block[R] = {
    val stms = block.stms
    val (cycles, nonCycles) = stms.partition{s => s.isInCycle && s.reduceCycle.cycleID > -1 }

    var beforeCycles: Seq[Sym[_]] = Nil
    var afterCycles: Seq[Sym[_]] = Nil
    nonCycles.foreach{s =>
      val usesCycle = (s.inputs intersect cycles).nonEmpty
      val downstreamFromCycle = (s.inputs intersect afterCycles).nonEmpty
      if (usesCycle || downstreamFromCycle) afterCycles = afterCycles :+ s
      else beforeCycles = beforeCycles :+ s
    }

    // We assume here that the analysis has marked only closed accumulation cycles for specialization
    // We define a cycle as closed if:
    //   1. It overlaps with no other accumulation cycles
    //   2. No partial outputs are used outside of the cycle
    //
    // This allows us to treat the graph as one chunk of compute with zero or more consumer cycles
    // effectively at the end.
    isolateSubst(block.result) {
      stageScope(f(block.inputs), block.options) {
        beforeCycles.foreach(visit)

        val cycleGroups = cycles.groupBy(_.reduceCycle.cycleID)
        dbgs(s"Cycle groups: ${cycleGroups.keySet.mkString(", ")}")

        cycleGroups.filterNot(_._2.isEmpty).foreach{case (id, syms: Seq[Sym[_]]) =>
          dbgs(s"  Cycle #$id: ")
          syms.foreach{s => dbgs(s"    ${stm(s)}")}
          val cycle = syms.head.reduceCycle

          cycle match {
            case cycle @ WARCycle(reader, writer, _, csyms, _, _, _) =>
              dbgs(s"  WAR CYCLE: Writer: $writer, Reader: $reader")
              cycle.marker match {
                case AccumMarker.Reg.Op(reg, data, written, first, ens, op, invert) =>
                  dbgs(s"Specializing the accumulation on $reg:")
                  dbgs(s"  Cycle: ")
                  csyms.foreach(s => dbgs("    " + stm(s)))
                  dbgs(s"  Operation: $op")
                  dbgs(s"  First: $first")
                  dbgs(s"  Invert: $invert")
                  dbgs(s"  Data: $data")

                  val result = regAccumOp(f(reg), f(data), f(first), f(ens), op, invert)
                  register(writer -> void)
                  register(written -> result)
                  result

                case AccumMarker.Reg.FMA(reg, m0, m1, written, first, ens, invert) =>
                  dbgs(s"Specializing the accumulation on $reg as an FMA:")
                  dbgs(s"  Cycle: ")
                  csyms.foreach(s => dbgs("    " + stm(s)))
                  dbgs(s"  First: $first")
                  dbgs(s"  Invert: $invert")
                  dbgs(s"  M0: $m0")
                  dbgs(s"  M1: $m1")

                  val result = regAccumFMA(f(reg), f(m0), f(m1), f(first), f(ens), invert)
                  register(writer -> void)
                  register(written -> result)
                  result

                case _ => syms.foreach(visit)
              }
            case _ => syms.foreach(visit)
          }
        }

        afterCycles.foreach(visit)

        f(block.result)
      }
    }
  }

}
