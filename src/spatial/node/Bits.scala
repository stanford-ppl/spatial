package spatial.node

import argon._
import forge.tags._
import spatial.lang._
import emul.{FixedPoint,FloatPoint, Bool, FALSE, TRUE}
import utils.implicits.collections._

@op case class Mux[A:Bits](s: Bit, a: Bits[A], b: Bits[A]) extends Primitive[A] {

  @rig override def rewrite: A = s match {
    case Literal(true)  => a.unbox
    case Literal(false) => b.unbox
    case _ if a == b    => a.unbox
    case _ => super.rewrite
  }

}
@op case class OneHotMux[A:Bits](sels: Seq[Bit], vals: Seq[Bits[A]]) extends Primitive[A] {

  @rig override def rewrite: A = {
    if (sels.length == 1) vals.head.unbox
    else if (sels.exists{case Literal(true) => true; case _ => false}) {
      val trues = sels.zipWithIndex.filter{case (Literal(true), _) => true; case _ => false }
      if (trues.lengthMoreThan(1)) {
        warn(ctx, "One-hot mux has multiple statically true selects")
        warn(ctx)
      }
      val idx = trues.head._2
      vals(idx).unbox
    }
    else if (vals.distinct.lengthIs(1)) {
      vals.head.unbox
    }
    else super.rewrite
  }
}

@op case class DataAsBits[A](data: Bits[A])(implicit val tV: Vec[Bit]) extends Primitive[Vec[Bit]] {
  override val isTransient: Boolean = true

  @rig override def rewrite: Vec[Bit] = data match {
    case Op(BitsAsData(vec,tp)) if data.tp =:= tp => vec
    case Const(c: FixedPoint) =>
      val bits = c.bits ++ Seq.fill(tV.nbits - data.nbits){ FALSE }
      Vec.fromSeq(bits.map(Bit.apply))

    case Const(c: FloatPoint) =>
      val bits = c.bits ++ Seq.fill(tV.nbits - data.nbits){ FALSE }
      Vec.fromSeq(bits.map(Bit.apply))

    case Const(c: Bool) =>
      val bits = c +: Seq.fill(tV.nbits - 1){ FALSE }
      Vec.fromSeq(bits.map(Bit.apply))

    case _ => super.rewrite
  }
}
@op case class BitsAsData[A:Bits](v: Vec[Bit], A: Bits[A]) extends Primitive[A] {
  override val isTransient: Boolean = true

  @rig override def rewrite: A = (v, A) match {
    case (Op(DataAsBits(data)), _) if data.tp =:= A => data.asInstanceOf[A]
    case (Op(VecAlloc(elems)), _:Bit) => elems.head.asInstanceOf[A]

    case (Op(VecAlloc(elems)), tp:Fix[_,_,_]) if elems.forall(_.isConst) =>
      val bits: Array[Bool] = elems.map{case Const(c) => c }.toArray
      val value = FixedPoint.fromBits(bits, tp.fmt.toEmul)
      A.from(value)

    case (Op(VecAlloc(elems)), tp: Flt[_,_]) if elems.forall(_.isConst) =>
      val bits: Array[Bool] = elems.map{case Const(c) => c }.toArray
      val value = FloatPoint.fromBits(bits, tp.fmt.toEmul)
      A.from(value)

    case _ => super.rewrite
  }
}
