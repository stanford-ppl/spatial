package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.retiming._
import spatial.metadata.access._
import spatial.traversal.AccelTraversal
import spatial.util.spatialConfig

case class AccumTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inAccel{ transformControl(lhs,rhs) }
    case _:BlackboxImpl[_,_,_] => inBox{ transformControl(lhs,rhs) }
    case _ => transformControl(lhs,rhs)
  }

  def transformControl[A:Type](lhs: Sym[A], rhs: Op[A]): Sym[A] = {
    if (inHw && rhs.blocks.nonEmpty) dbgs(stm(lhs))

    rhs match {
      case ctrl: Control[_] if !(lhs.isSwitch && lhs.isInnerControl) && !spatialConfig.enablePIR =>
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
    // Partition stms into those that are part of cycles to-be-replaced and those that are not
    val (cycles, nonCycles) = stms.partition{s => s.isInCycle && s.reduceCycle.cycleID > -1 }
    // Find the writer nodes in the cycles (i.e. nodes that mark the end of an atomic reduction chain to-be-replaced)
    val cycleWriters = cycles.filter(_.isWriter)
    // Find memories that are being accumulated into
    val cycleMems = cycleWriters.map(_.writtenMem.get)
    // Build mapping between each memory and the index in the IR where its accumulating write happens
    val cycleEnds = cycleWriters.zip(cycleMems).map{case (w,m) => (m -> stms.indexOf(w))}.toMap
    // Initialize Seq of nodes that will be placed before and after specialization replacements
    var beforeCycles: Seq[Sym[_]] = Nil
    var afterCycles: Seq[Sym[_]] = Nil
    dbgs(s"Placing nodes as either before or after cycles: $cycles")
    stms.zipWithIndex.collect{case (s,i) if nonCycles.contains(s) =>
      val usesCycle = (s.inputs intersect cycles).nonEmpty
      val readsAfterAccum = (s.inputs intersect cycleMems).nonEmpty && (s.inputs.exists{case inp if cycleEnds.contains(inp) => i > cycleEnds(inp); case _ => false})
      val downstreamFromCycle = (s.inputs intersect afterCycles).nonEmpty
      dbgs(s"  - ${stm(s)}: Uses nodes in cycle: ${usesCycle}, Reads after accumulating write: $readsAfterAccum, Is downstream of cycle: $downstreamFromCycle")
      if (usesCycle || readsAfterAccum || downstreamFromCycle) afterCycles = afterCycles :+ s
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
