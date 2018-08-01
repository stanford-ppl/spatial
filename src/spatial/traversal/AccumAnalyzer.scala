package spatial.traversal

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
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
    val warCycles = cycles.collect{case cycle:WARCycle => cycle }

    var disjointCycles: Set[(WARCycle,Int)] = Set.empty

    // Find sets of cycles which are entirely disjoint
    warCycles.zipWithIndex.foreach{case (c1, i) =>
      val overlapping = disjointCycles.filter{case (c2,j) => (c1.symbols intersect c2.symbols).nonEmpty }
      val isClosedCycle = c1.symbols.forall{s =>
        // Only use data dependencies
        val consumers = s.consumers.filter(_.nonBlockInputs.contains(s))
        val outsideConsumers = if (s.isVoid) Nil else consumers diff c1.symbols
        outsideConsumers.isEmpty
      }
      val isDisjoint = overlapping.isEmpty && isClosedCycle

      if (isDisjoint) disjointCycles += ((c1,i))
      if (overlapping.nonEmpty) disjointCycles --= overlapping
    }

    disjointCycles.foreach{case (c,id) =>
      val marker = AccumMark.unapply(c.writer).getOrElse(AccumMarker.Unknown)
      val cycle = c.copy(marker = marker, cycleID = id)

      dbgs(s"Candidate cycle #$id: ")
      c.symbols.foreach{s => dbgs(s"  ${stm(s)}") }

      dbgs(s"Marking cycle #$id on ${cycle.memory} for specialization: $marker")
      c.symbols.foreach{s => s.reduceCycle = cycle }
    }
  }


  private object Mul {
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

  object AccumMark {
    def unapply(writer: Sym[_]): Option[AccumMarker] = writer match {
      case Op(RegWrite(reg,update,ens)) =>
        dbgs(s"$writer matched as a RegWrite")
        update match {
          // Specializing sums
          case RegAdd(`reg`,data)  => Some(AccumMarker.Reg.Op(reg,data,false,ens,Accum.Add,invert=false))
          case RegMul(`reg`,data)  => Some(AccumMarker.Reg.Op(reg,data,false,ens,Accum.Mul,invert=false))
          case RegMin(`reg`,data)  => Some(AccumMarker.Reg.Op(reg,data,false,ens,Accum.Min,invert=false))
          case RegMax(`reg`,data)  => Some(AccumMarker.Reg.Op(reg,data,false,ens,Accum.Max,invert=false))
          case RegFMA(`reg`,m0,m1) => Some(AccumMarker.Reg.FMA(reg,m0,m1,false,ens,invert=false))

          case Op(Mux(sel,x1,x2)) =>
            dbgs(s"$update matched on mux (sel: $sel, x1: $x1, x2: $x2)")
            (x1,x2) match {
              case (`x1`, RegAdd(`reg`,`x1`)) => Some(AccumMarker.Reg.Op(reg,x1,sel,ens,Accum.Add,invert=false))
              case (`x1`, RegMul(`reg`,`x1`)) => Some(AccumMarker.Reg.Op(reg,x1,sel,ens,Accum.Mul,invert=false))
              case (`x1`, RegMin(`reg`,`x1`)) => Some(AccumMarker.Reg.Op(reg,x1,sel,ens,Accum.Min,invert=false))
              case (`x1`, RegMax(`reg`,`x1`)) => Some(AccumMarker.Reg.Op(reg,x1,sel,ens,Accum.Max,invert=false))
              case (RegAdd(`reg`,`x2`), `x2`) => Some(AccumMarker.Reg.Op(reg,x2,sel,ens,Accum.Add,invert=true))
              case (RegMul(`reg`,`x2`), `x2`) => Some(AccumMarker.Reg.Op(reg,x2,sel,ens,Accum.Mul,invert=true))
              case (RegMin(`reg`,`x2`), `x2`) => Some(AccumMarker.Reg.Op(reg,x2,sel,ens,Accum.Min,invert=true))
              case (RegMax(`reg`,`x2`), `x2`) => Some(AccumMarker.Reg.Op(reg,x2,sel,ens,Accum.Max,invert=true))
              // It'd be really nice if Scala allowed use of bound names within the same case pattern
              // Note: the multiplication of m0 and m1 will be dropped upon transforming
              case (Mul(m0,m1), RegFMA(`reg`,a0,a1)) if m0==a0 && m1==a1 => Some(AccumMarker.Reg.FMA(reg,m0,m1,sel,ens,invert=false))
              case (RegFMA(`reg`,a0,a1), Mul(m0,m1)) if m0==a0 && m1==a1 => Some(AccumMarker.Reg.FMA(reg,m0,m1,sel,ens,invert=true))
              case _ => None
            }
          case _ => Some(AccumMarker.Reg.Lambda)
        }
      case _ => None
    }
  }


}
