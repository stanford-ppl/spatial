package spatial.rewrites

import argon._
import emul.{FALSE, FixedPoint, FloatPoint, Bool}
import forge.tags._
import spatial.lang._
import spatial.node._
import spatial.util._

trait BitsRewrites extends RewriteRules {

  @rewrite def bits_as_data(op: BitsAsData[_]): Sym[_] = {
    case BitsAsData(vec @ Op(VecAlloc(elems)), tp: Bit) => elems.head

    case BitsAsData(vec @ Op(VecAlloc(elems)), tp: Fix[_,_,_]) if elems.forall(_.isConst) =>
      val bits: Array[emul.Bool] = elems.map{case Const(c) => c }.toArray
      val c = FixedPoint.fromBits(bits, tp.fmt.toEmul)
      tp.from(c).asInstanceOf[Fix[_,_,_]]

    case BitsAsData(vec @ Op(VecAlloc(elems)), tp: Flt[_,_]) if elems.forall(_.isConst) =>
      val bits: Array[emul.Bool] = elems.map{case Const(c) => c }.toArray
      val c = FloatPoint.fromBits(bits, tp.fmt.toEmul)
      tp.from(c).asInstanceOf[Flt[_,_]]
  }

  @rewrite def data_as_bits(op: DataAsBits[_]): Sym[_] = {
    case op @ DataAsBits(x) =>

      op match {
        case op @ DataAsBits(x @ Const(c: FixedPoint)) =>
          implicit val tV: Vec[Bit] = op.tV
          val bits = c.bits ++ Seq.fill(tV.nbits - x.nbits){ FALSE }
          Vec.fromSeq(bits.map(Bit.apply))

        case op @ DataAsBits(x @ Const(c: FloatPoint)) =>
          implicit val tV: Vec[Bit] = op.tV
          val bits = c.bits ++ Seq.fill(tV.nbits - x.nbits){ FALSE }
          Vec.fromSeq(bits.map(Bit.apply))

        case op @ DataAsBits(Const(c: Bool)) =>
          implicit val tV: Vec[Bit] = op.tV
          val bits = c +: Seq.fill(tV.nbits - 1){ FALSE }
          Vec.fromSeq(bits.map(Bit.apply))

        case _ => Invalid
      }
  }

}
