package spatial.traversal

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.retiming._
import spatial.metadata.types._
import spatial.util.modeling._

case class AccumAnalyzer(IR: State) extends AccelTraversal {

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    if (rhs.blocks.nonEmpty) {
      dbgs(stm(lhs))
      state.logTab += 1
    }
    rhs match {
      case AccelScope(_) => inAccel{ markBlocks(lhs,rhs) }
      case _ => markBlocks(lhs,rhs)
    }
    if (rhs.blocks.nonEmpty) state.logTab -= 1
  }

  def markBlocks[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    if (inHw && lhs.isControl && lhs.blocks.nonEmpty) {
      val (inner,outer) = lhs.innerAndOuterBlocks
      inner.iterator.map(_._2).foreach(markBlock)
      outer.iterator.map(_._2).foreach(blk => visitBlock(blk))
    }
    else super.visit(lhs,rhs)
  }

  def markBlock(block: Block[_]): Unit = {
    // Find standard write-after-read accumulation candidates
    // TODO: May want to keep timing metadata from a different common traversal
    val (_, cycles) = latenciesAndCycles(block)
    val warCycles = cycles.collect{case cycle:WARCycle => cycle }.zipWithIndex

    def accumControl(first: Bit, writer: Sym[_]): Option[Ctrl] = {
      val iters = first match {
        case IterAnd(is) => is
        case _ => writer.scopes.flatMap(_.iters).toSet
      }
      if (iters.size > 0) Some(LCA(iters.map(_.parent))) else None
    }

    // Find sets of cycles which are entirely disjoint
    warCycles.foreach{case (c1, i) =>
      val overlapping = warCycles.filter{case (c2,j) => i != j && (c1.symbols intersect c2.symbols).nonEmpty }

      def externalUses(s: Sym[_]): Set[Sym[_]] = if (s.isVoid) Set.empty else {
        // Only use data dependencies
        val consumers = s.consumers.filter(_.nonBlockInputs.contains(s))
        consumers diff c1.symbols
      }

      // Intermediate accumulator values are allowed to be consumed as long by writes
      // as long as the value is not actually visible until the end of the accumulation
      val intermediates = c1.writer match {
        case Op(RegWrite(_,data,_)) => Seq(data)
        case _ => Nil
      }

      val isDisjoint      = overlapping.isEmpty

      val isClosedCycle   = (c1.symbols diff intermediates.toSet).forall{s => externalUses(s).isEmpty }
      val noIntermediates = intermediates.forall{s => externalUses(s).isEmpty }
      val noEscaping      = c1.memory.accumType == AccumType.Reduce || noIntermediates
      val noVisibleIntermediates = isClosedCycle && noEscaping
      val isLocalMem      = !c1.memory.isRemoteMem
      val numWriters      = c1.memory.writers.size
      val outerReduce     = c1.memory.writers.head.parent.isUnitPipe

      dbgs(s"Cycle #$i on ${c1.memory}: ")
      dbgs(s"  ${stm(c1.memory)} [${c1.memory.name.getOrElse(c1.toString)}]")
      dbgs(s"  disjoint:     $isDisjoint")
      dbgs(s"  no visible intermediates:")
      dbgs(s"    closed cycle:     $isClosedCycle")
      dbgs(s"    no intermediates: $noIntermediates")
      dbgs(s"    is local mem:     $isLocalMem")
      dbgs(s"    number of writers:  $numWriters")
      dbgs(s"    outer reduce:     $outerReduce")
      dbgs(s"    accum type:       ${c1.memory.accumType} (if reduce, overrides no intermediates)")

      if (isDisjoint && noVisibleIntermediates && isLocalMem && numWriters == 1 && !outerReduce) {
        val marker = c1.writer match {
          case AssociateReduce(m) =>
            m.control = accumControl(m.first,c1.writer)
            if (m.control.isDefined) Some(m) else None
          case _ => None
        }
        marker.foreach{m =>
          val cycle = c1.copy(marker = m, cycleID = i)
          dbgs(s"Marking cycle #$i on ${cycle.memory} for specialization: $marker")
          c1.symbols.foreach{s => dbgs(s"  ${stm(s)}") }
          c1.symbols.foreach{s => s.reduceCycle = cycle }
        }
      }
    }
  }

  private object IterAnd {
    def unapply(b: Bit): Option[Set[Idx]] = b match {
      case Op(FixEql(i,v)) if i.isIdx && i.isBound && i.counter.ctr.start == v => Some(Set(i.asInstanceOf[Idx]))
      case Op(And(IterAnd(i1), IterAnd(i2))) => Some(i1 ++ i2)
      case _ => None
    }
  }


  private object Times {
    def unapply(s: Sym[_]): Option[(Bits[_],Bits[_])] = s match {
      case Op(FixMul(a,b)) => Some((a,b))
      case Op(FltMul(a,b)) => Some((a,b))
      case _ => None
    }
  }

  private object RegAdd {
    def unapply(s: Sym[_]): Option[(Reg[_], Bits[_])] = s match {
      case Op(FixAdd(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FixAdd(data, Op(RegRead(reg)))) => Some((reg,data))
      case Op(FltAdd(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FltAdd(data, Op(RegRead(reg)))) => Some((reg,data))
      case _ => None
    }
  }

  private object RegMul {
    def unapply(s: Sym[_]): Option[(Reg[_], Bits[_])] = s match {
      case Op(FixMul(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FixMul(data, Op(RegRead(reg)))) => Some((reg,data))
      case Op(FltMul(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FltMul(data, Op(RegRead(reg)))) => Some((reg,data))
      case _ => None
    }
  }

  private object RegMin {
    def unapply(s: Sym[_]): Option[(Reg[_], Bits[_])] = s match {
      case Op(FixMin(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FixMin(data, Op(RegRead(reg)))) => Some((reg,data))
      case Op(FltMin(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FltMin(data, Op(RegRead(reg)))) => Some((reg,data))
      case _ => None
    }
  }

  private object RegMax {
    def unapply(s: Sym[_]): Option[(Reg[_], Bits[_])] = s match {
      case Op(FixMax(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FixMax(data, Op(RegRead(reg)))) => Some((reg,data))
      case Op(FltMax(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FltMax(data, Op(RegRead(reg)))) => Some((reg,data))
      case _ => None
    }
  }

  private object RegFMA {
    def unapply(s: Sym[_]): Option[(Reg[_], Bits[_], Bits[_])] = s match {
      case Op(FixFMA(m0, m1, Op(RegRead(reg)))) => Some((reg,m0,m1))
      case Op(FltFMA(m0, m1, Op(RegRead(reg)))) => Some((reg,m0,m1))
      case _ => None
    }
  }

  object AssociateReduce {
    def unapply(writer: Sym[_]): Option[AccumMarker] = writer match {
      case Op(RegWrite(reg,written,ens)) =>
        dbgs(s"$writer matched as a RegWrite")
        written match {
          // Specializing sums
          case RegAdd(`reg`,data)  => Some(AccumMarker.Reg.Op(reg,data,written,false,ens,AccumAdd,invert=false))
          case RegMul(`reg`,data)  => Some(AccumMarker.Reg.Op(reg,data,written,false,ens,AccumMul,invert=false))
          case RegMin(`reg`,data)  => Some(AccumMarker.Reg.Op(reg,data,written,false,ens,AccumMin,invert=false))
          case RegMax(`reg`,data)  => Some(AccumMarker.Reg.Op(reg,data,written,false,ens,AccumMax,invert=false))
          case RegFMA(`reg`,m0,m1) => Some(AccumMarker.Reg.FMA(reg,m0,m1,written,false,ens,invert=false))

          case Op(Mux(sel,x1,x2)) =>
            dbgs(s"$written matched on mux (sel: $sel, x1: $x1, x2: $x2)")
            (x1,x2) match {
              case (`x1`, RegAdd(`reg`,`x1`)) => Some(AccumMarker.Reg.Op(reg,x1,written,sel,ens,AccumAdd,invert=false))
              case (`x1`, RegMul(`reg`,`x1`)) => Some(AccumMarker.Reg.Op(reg,x1,written,sel,ens,AccumMul,invert=false))
              case (`x1`, RegMin(`reg`,`x1`)) => Some(AccumMarker.Reg.Op(reg,x1,written,sel,ens,AccumMin,invert=false))
              case (`x1`, RegMax(`reg`,`x1`)) => Some(AccumMarker.Reg.Op(reg,x1,written,sel,ens,AccumMax,invert=false))
              case (RegAdd(`reg`,`x2`), `x2`) => Some(AccumMarker.Reg.Op(reg,x2,written,sel,ens,AccumAdd,invert=true))
              case (RegMul(`reg`,`x2`), `x2`) => Some(AccumMarker.Reg.Op(reg,x2,written,sel,ens,AccumMul,invert=true))
              case (RegMin(`reg`,`x2`), `x2`) => Some(AccumMarker.Reg.Op(reg,x2,written,sel,ens,AccumMin,invert=true))
              case (RegMax(`reg`,`x2`), `x2`) => Some(AccumMarker.Reg.Op(reg,x2,written,sel,ens,AccumMax,invert=true))
              // It'd be really nice if Scala allowed use of bound names within the same case pattern
              // Note: the multiplication of m0 and m1 will be dropped upon transforming
              case (Times(m0,m1), RegFMA(`reg`,a0,a1)) if m0==a0 && m1==a1 => Some(AccumMarker.Reg.FMA(reg,m0,m1,written,sel,ens,invert=false))
              case (RegFMA(`reg`,a0,a1), Times(m0,m1)) if m0==a0 && m1==a1 => Some(AccumMarker.Reg.FMA(reg,m0,m1,written,sel,ens,invert=true))
              case _ => None
            }
          case _ => None
        }
      case _ => None
    }
  }


}
