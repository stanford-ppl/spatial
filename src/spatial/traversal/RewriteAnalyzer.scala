package spatial.traversal

import argon._
import argon.node._
import spatial.metadata.rewrites._
import spatial.metadata.memory._
import spatial.node._
import spatial.util.spatialConfig


/** Flags whether hardware rewrites should be allowed on given nodes.
  *
  * This is required because RewriteTransformer is a MutateTransformer, so not all metadata
  * will be up to date (e.g. consumers) during the transformation.
  */
case class RewriteAnalyzer(IR: State) extends AccelTraversal {

  /** Check if the RegWrite and RegRead are on the same register or the registers were duplicates of the same original sym */
  private def sameReg(wr: Sym[_], rd: Sym[_]): Boolean = {
    (wr, rd) match {
      case (Op(RegWrite(reg, _, _)), Op(RegRead(reg2))) => reg2 == reg || reg2.originalSym == reg.originalSym
      case _ => false
    }
  }

  /** Checks if Add(_,Mul) is part of a Reduce pattern, where the Mul has two consumers that 
    * fit a specific pattern
    */
  private def specializationFusePattern(lhs: Sym[_], mul: Sym[_], a: Sym[_]): Boolean = {
    mul.consumers.filter(_ != lhs).forall{s => 
      s match {
        case mx @ Op(Mux(_, `lhs`, `mul`)) => mx.consumers.forall{w => sameReg(w, a)}
        case mx @ Op(Mux(_, `mul`, `lhs`)) => mx.consumers.forall{w => sameReg(w, a)}
        case _ => false
      }
    } && mul.consumers.size == 2
  }

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case AccelScope(_) => inAccel{ super.visit(lhs,rhs) }
    case _:BlackboxImpl[_,_,_] => inBox{ super.visit(lhs,rhs) }
    case FixAdd(a, mul @ Op(FixMul(_,_))) =>
      val forwardOnlyPattern = mul.consumers.size == 1
      val consumerPattern = specializationFusePattern(lhs, mul, a)
      lhs.canFuseAsFMA = inHw && (forwardOnlyPattern || consumerPattern) && lhs.isInnerReduceOp == mul.isInnerReduceOp && spatialConfig.fuseAsFMA
    case FixAdd(mul @ Op(FixMul(_,_)), a) => 
      val forwardOnlyPattern = mul.consumers.size == 1
      val consumerPattern = specializationFusePattern(lhs, mul, a)
      lhs.canFuseAsFMA = inHw && (forwardOnlyPattern || consumerPattern) && lhs.isInnerReduceOp == mul.isInnerReduceOp && spatialConfig.fuseAsFMA

    case FltAdd(a, mul @ Op(FltMul(_,_))) => 
      val forwardOnlyPattern = mul.consumers.size == 1
      val consumerPattern = specializationFusePattern(lhs, mul, a)
      lhs.canFuseAsFMA = inHw && (forwardOnlyPattern || consumerPattern) && lhs.isInnerReduceOp == mul.isInnerReduceOp && spatialConfig.fuseAsFMA
    case FltAdd(mul @ Op(FltMul(_,_)), a) => 
      val forwardOnlyPattern = mul.consumers.size == 1
      val consumerPattern = specializationFusePattern(lhs, mul, a)
      lhs.canFuseAsFMA = inHw && (forwardOnlyPattern || consumerPattern) && lhs.isInnerReduceOp == mul.isInnerReduceOp && spatialConfig.fuseAsFMA

    case _ => super.visit(lhs,rhs)
  }

}
