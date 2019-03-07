package spatial.dse

import argon._
import argon.node._
import spatial.metadata.params._
import spatial.lang._
import spatial.node._
import spatial.lang.I32
import spatial.metadata.bounds._
import spatial.metadata.memory._
import spatial.metadata.control._
import spatial.metadata.access._
import spatial.metadata.types._
import spatial.traversal._
import spatial.util.spatialConfig

case class ScalarAnalyzer(IR: State) extends RerunTraversal { 

  private var insideLoop = false
  def maybeLoop[T](isLoop: Boolean)(x: => T): T = {
    if (isLoop) {
      val prevLoop = insideLoop
      insideLoop = true
      val result = x
      insideLoop = prevLoop
      result
    }
    else x
  }

  /**
    * In Spatial, a "global" is any value which is solely a function of input arguments
    * and constants. These are computed prior to starting the main computation, and
    * therefore appear constant to the majority of the program.
    *
    * Note that this is only true for stateless nodes. These rules should not be generated
    * for stateful hardware (e.g. accumulators, pseudo-random generators)
    **/
  def checkForGlobals(lhs: Sym[_], rhs: Op[_]): Unit = lhs match {
    case Impure(_,_) =>
    case Op(RegRead(reg)) if reg.isArgIn => lhs.isGlobal = true
    case _ =>
      if (lhs.isPrimitive && rhs.inputs.nonEmpty && rhs.inputs.forall(_.isGlobal))
        lhs.isGlobal = true
  }

  object IterMin {
    def unapply(x: Num[_]): Option[Int] = x.getCounter match {
      case Some(Def(CounterNew(start,end,Expect(step),_))) => if (step > 0) start.getBound.map(_.toInt) else end.getBound.map(_.toInt)
      case _ => None
    }
  }
  object IterMax {
    def unapply(x: Num[_]): Option[Int] = x.getCounter match {
      case Some(Def(CounterNew(start,end,Expect(step),_))) => if (step > 0) end.getBound.map(_.toInt) else start.getBound.map(_.toInt)
      case _ => None
    }
  }

  /**
    * Propagates symbol maximum bounds. Generally assumes non-negative values, e.g. for index calculation
    */
  def analyzeBounds(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case RegRead(Bounded(b)) =>
      dbgs(s"Bounded register read $lhs: $b")
      lhs.bound = b

    case RegRead(reg) =>
      dbgs(s"Register read of $reg")

    case RegWrite(reg@Bounded(b1), Bounded(b2), _) if (lhs.accumType == AccumType.Unknown) && !reg.isHostIO =>
      dbgs(s"Reg write outside loop")
      reg.bound = UpperBound((b1 meet b2).toInt)
    case RegWrite(reg, Bounded(b), _) if (lhs.accumType == AccumType.Unknown) && !reg.isHostIO =>
      dbgs(s"Reg write outside loop")
      reg.bound = UpperBound(b.toInt)

    case IfThenElse(c, thenp, elsep) => (thenp.result, elsep.result) match {
      case (Bounded(a), Bounded(b)) => lhs.bound = a meet b
      case _ => // No bound defined otherwise
    }

    case FixNeg(Final(a)) => lhs.bound = Final(-a)
    case FixNeg(Expect(a)) => lhs.bound = Expect(-a)
    case FixNeg(Upper(a)) => lhs.bound = UpperBound(-a) // TODO: Not really correct

    case FixAdd(Final(a),Final(b)) => lhs.bound = Final(a + b)
    case FixAdd(Expect(a),Expect(b)) => lhs.bound = Expect(a + b)
    case FixAdd(Upper(a),Upper(b)) => lhs.bound = UpperBound((a + b).toInt)
    case FixAdd(Upper(a), IterMax(i)) => lhs.bound = UpperBound((a + i).toInt)
    case FixAdd(IterMax(i),Upper(a)) => lhs.bound = UpperBound((a + i).toInt)
    case FixAdd(IterMax(i),IterMax(j)) => lhs.bound = UpperBound((i + j).toInt)

    case FixSub(Final(a),Final(b)) => lhs.bound = Final(a - b)
    case FixSub(Expect(a),Expect(b)) => lhs.bound = Expect(a - b)
    case FixSub(Upper(a),Upper(b)) => lhs.bound = UpperBound((a - b).toInt)
    case FixSub(Upper(a),IterMin(i)) => lhs.bound = UpperBound((a - i).toInt)
    case FixSub(IterMax(i),Upper(a)) => lhs.bound = UpperBound((i - a).toInt)
    case FixSub(IterMax(i),IterMin(j)) => lhs.bound = UpperBound((i - j).toInt)

    case FixMul(Final(a),Final(b)) => lhs.bound = Final(a * b)
    case FixMul(Expect(a),Expect(b)) => lhs.bound = Expect(a * b)
    case FixMul(Upper(a),Upper(b)) => lhs.bound = UpperBound((a * b).toInt)
    case FixMul(Upper(a),IterMax(i)) => lhs.bound = UpperBound((a * i).toInt)
    case FixMul(IterMax(i),Upper(a)) => lhs.bound = UpperBound((i * a).toInt)
    case FixMul(IterMax(i),IterMax(j)) => lhs.bound = UpperBound((i * j).toInt)

    case FixDiv(Final(a),Final(b)) => lhs.bound = Expect(a / b + (if ( (a % b) > 0) 1 else 0))
    case FixDiv(Expect(a),Expect(b)) => lhs.bound = Expect(a / b + (if ( (a % b) > 0) 1 else 0))
    case FixDiv(Upper(a),Upper(b)) => lhs.bound = UpperBound(a / b + (if ( (a % b) > 0) 1 else 0))
    case FixMod(_, Upper(b)) => lhs.bound = UpperBound(b - 1)

    case FixMin(Final(a),Final(b)) => lhs.bound = Final((a min b).toInt)
    case FixMin(Expect(a),Expect(b)) => lhs.bound = UpperBound((a min b).toInt)
    case FixMin(Upper(a),Upper(b)) => lhs.bound = UpperBound((a min b).toInt)
    case FixMin(Upper(a),IterMax(i)) => lhs.bound = UpperBound((a min i.toInt).toInt)
    case FixMin(IterMax(i),Upper(a)) => lhs.bound = UpperBound((i.toInt min a).toInt)
    case FixMin(IterMax(i),IterMax(j)) => lhs.bound = UpperBound((i min j).toInt)

    case FixMax(Final(a),Final(b)) => lhs.bound = Final((a max b).toInt)
    case FixMax(Expect(a),Expect(b)) => lhs.bound = UpperBound((a max b).toInt)
    case FixMax(Upper(a),Upper(b)) => lhs.bound = UpperBound((a max b).toInt)
    case FixMax(Upper(a),IterMax(i)) => lhs.bound = UpperBound((a max i.toInt).toInt)
    case FixMax(IterMax(i),Upper(a)) => lhs.bound = UpperBound((i.toInt max a).toInt)
    case FixMax(IterMax(i),IterMax(j)) => lhs.bound = UpperBound((i max j).toInt)

    case FIFONumel(fifo,_) => lhs.bound = UpperBound(fifo.constDims.head)
    case LIFONumel(filo,_) => lhs.bound = UpperBound(filo.constDims.head)

    case FixSub(Op(FixAdd(Op(FixToFix(b,_)), Bounded(x))), a) if a == b => lhs.bound = x
    case _ =>
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]) = {
    checkForGlobals(lhs,rhs)
    analyzeBounds(lhs,rhs)
    dbgs(s"Visiting $lhs = $rhs [isLoop: ${rhs.isLoop}]")
    maybeLoop(rhs.isLoop){ rhs.blocks.foreach(blk => visitBlock(blk)) }
  }
}
