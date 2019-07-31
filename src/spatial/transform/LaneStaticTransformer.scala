package spatial.transform

import argon._
import argon.node._
import emul.ResidualGenerator._
import argon.transform.MutateTransformer
import spatial.metadata.bounds._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.metadata.rewrites._
import spatial.metadata.retiming._
import spatial.metadata.math._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal
import forge.tags._
import emul.FixedPoint

import utils.math.{isPow2,log2,gcd}
import spatial.util.math._

/** This transformer looks at the pre-unrolled, pre-pipe-insertion IR for situations where
  * FixMod nodes will statically resolve only AFTER unrolling.  It replaces them with a LaneStatic
  * node, which is transient, and therefore skips wrapping the original FixMod node in a unit pipe.
  * This is also necessary for banking analysis lockstep/iter offset analysis
  */
case class LaneStaticTransformer(IR: State) extends MutateTransformer with AccelTraversal {


  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {

    case _:AccelScope => inAccel{ super.transform(lhs,rhs) }

    case FixNeg(F(Op(LaneStatic(iter, elems)))) =>              
      stageWithFlow( LaneStatic[FixPt[TRUE,_32,_0]](iter.asInstanceOf[FixPt[TRUE,_32,_0]], elems.map(_ * -1)) ){lhs2 => transferData(lhs, lhs2) }.asInstanceOf[Sym[A]]
    case FixAdd(F(Final(a)), F(Op(LaneStatic(iter, elems)))) => 
      stageWithFlow( LaneStatic[FixPt[TRUE,_32,_0]](iter.asInstanceOf[FixPt[TRUE,_32,_0]], elems.map(_ +  a)) ){lhs2 => transferData(lhs, lhs2) }.asInstanceOf[Sym[A]]
    case FixAdd(F(Op(LaneStatic(iter, elems))), F(Final(a))) => 
      stageWithFlow( LaneStatic[FixPt[TRUE,_32,_0]](iter.asInstanceOf[FixPt[TRUE,_32,_0]], elems.map(_ +  a)) ){lhs2 => transferData(lhs, lhs2) }.asInstanceOf[Sym[A]]
    case FixSub(F(Final(a)), F(Op(LaneStatic(iter, elems)))) => 
      stageWithFlow( LaneStatic[FixPt[TRUE,_32,_0]](iter.asInstanceOf[FixPt[TRUE,_32,_0]], elems.map(a -  _)) ){lhs2 => transferData(lhs, lhs2) }.asInstanceOf[Sym[A]]
    case FixSub(F(Op(LaneStatic(iter, elems))), F(Final(a))) => 
      stageWithFlow( LaneStatic[FixPt[TRUE,_32,_0]](iter.asInstanceOf[FixPt[TRUE,_32,_0]], elems.map(_ -  a)) ){lhs2 => transferData(lhs, lhs2) }.asInstanceOf[Sym[A]]
    case FixMul(F(Final(a)), F(Op(LaneStatic(iter, elems)))) => 
      stageWithFlow( LaneStatic[FixPt[TRUE,_32,_0]](iter.asInstanceOf[FixPt[TRUE,_32,_0]], elems.map(_ *  a)) ){lhs2 => transferData(lhs, lhs2) }.asInstanceOf[Sym[A]]
    case FixMul(F(Op(LaneStatic(iter, elems))), F(Final(a))) => 
      stageWithFlow( LaneStatic[FixPt[TRUE,_32,_0]](iter.asInstanceOf[FixPt[TRUE,_32,_0]], elems.map(_ *  a)) ){lhs2 => transferData(lhs, lhs2) }.asInstanceOf[Sym[A]]
    case FixSLA(F(Op(LaneStatic(iter, elems))), F(Final(a))) => 
      stageWithFlow( LaneStatic[FixPt[TRUE,_32,_0]](iter.asInstanceOf[FixPt[TRUE,_32,_0]], elems.map(_ *  scala.math.pow(2,a).toInt)) ){lhs2 => transferData(lhs, lhs2) }.asInstanceOf[Sym[A]]

    case FixMod(F(x), F(Final(y))) if inHw => 
      val (iter, add, mul) = x match {
        case Op(FixAdd(F(xx), Final(ofs))) => (xx, ofs, 1)
        case Op(FixAdd(Final(ofs), F(xx))) => (xx, ofs, 1)
        case Op(FixSub(F(xx), Final(ofs))) => (xx, -ofs, 1)
        case Op(FixMul(Final(lin), F(xx))) => (xx, 0, lin)
        case Op(FixMul(F(xx), Final(lin))) => (xx, 0, lin)
        case Op(FixFMA(Final(lin), F(xx), Final(ofs))) => (xx, ofs, lin)
        case Op(FixFMA(F(xx), Final(lin), Final(ofs))) => (xx, ofs, lin)
        case _ => (x, 0, 1)
      }

      if (iter.getCounter.isDefined && iter.counter.lanes.size > 1 && staticMod(mul, iter, y)) {
        // Convert FixMod node into a LaneStatic vector
        val posMod = getPosMod(mul, iter, add, y)
        // implicit val A: Bits[A] = iter.selfType
        stageWithFlow( LaneStatic[FixPt[TRUE,_32,_0]](iter.asInstanceOf[FixPt[TRUE,_32,_0]], posMod) ){lhs2 => transferData(lhs, lhs2) }.asInstanceOf[Sym[A]]
      } else super.transform(lhs,rhs)

    case _ => super.transform(lhs,rhs)
  }

}
