package argon.node

import argon._
import argon.lang._
import emul.{Bool, FALSE, FixedPoint, FloatPoint}
import forge.tags._

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

@op case class VecAsData[A:Bits,B:Bits](v: Vec[B], A: Bits[A]) extends Primitive[A] {
  override val isTransient: Boolean = true

  @rig override def rewrite: A = (v, A) match {
    case (Op(DataAsVec(data)), _) if data.tp =:= A => data.asInstanceOf[A]
    case (Op(VecAlloc(elems)), _:Bit) => elems.head.asInstanceOf[A]

//    case (Op(VecAlloc(elems: Seq[Bit])), tp:Fix[_,_,_]) if v =:= Bit && elems.forall(_.isConst) =>
//      val bits: Array[Bool] = elems.map{case Const(c) => c }.toArray
//      val value = FixedPoint.fromBits(bits, tp.fmt.toEmul)
//      A.from(value)
//
//    case (Op(VecAlloc(elems: Seq[Bit])), tp: Flt[_,_]) if v =:= Bit && elems.forall(_.isConst) =>
//      val bits: Array[Bool] = elems.map{case Const(c) => c }.toArray
//      val value = FloatPoint.fromBits(bits, tp.fmt.toEmul)
//      A.from(value)

    case _ => super.rewrite
  }
}

@op case class DataAsVec[A:Bits,B:Bits](data: Bits[A])(implicit val tV: Vec[B]) extends Primitive[Vec[B]] {
  override val isTransient: Boolean = true

  @rig override def rewrite: Vec[B] = data match {
    case Op(VecAsData(vec,tp)) if data.tp =:= tp => vec.asInstanceOf[Vec[B]]
    case Const(c: FixedPoint) =>
      val bits: Seq[Bool] = c.bits ++ Seq.fill(tV.nbits - data.nbits){ FALSE }
      Vec.fromSeq(bits.grouped(Bits[A].nbits/Bits[B].nbits).map{x => Vec.fromSeq[Bit](x.map(Bit.apply)).as[B]}.toSeq)

    case Const(c: FloatPoint) =>
      val bits: Seq[Bool] = c.bits ++ Seq.fill(tV.nbits - data.nbits){ FALSE }
      Vec.fromSeq(bits.grouped(Bits[A].nbits/Bits[B].nbits).map{x => Vec.fromSeq[Bit](x.map(Bit.apply)).as[B]}.toSeq)

    case Const(c: Bool) =>
      val bits: Seq[Bool] = c +: Seq.fill(tV.nbits - 1){ FALSE }
      Vec.fromSeq(bits.grouped(Bits[A].nbits/Bits[B].nbits).map{x => Vec.fromSeq[Bit](x.map(Bit.apply)).as[B]}.toSeq)

    case _ => super.rewrite
  }
}

@op case class BitsPopcount(data: Seq[Bit]) extends Primitive[U8] {
  override val isTransient: Boolean  = true 
}