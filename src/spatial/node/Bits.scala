package spatial.node

import argon._
import forge.tags._
import spatial.lang._

@op case class Mux[A:Bits](s: Bit, a: Bits[A], b: Bits[A]) extends Primitive[A]
@op case class OneHotMux[A:Bits](sels: Seq[Bit], vals: Seq[Bits[A]]) extends Primitive[A]

@op case class DataAsBits[A](data: Bits[A])(implicit val tV: Vec[Bit]) extends Primitive[Vec[Bit]] {
  override val isEphemeral: Boolean = true

  @rig override def rewrite: Vec[Bit] = data match {
    case Op(BitsAsData(vec,tp)) if data.tp =:= tp => vec
    case _ => super.rewrite
  }
}
@op case class BitsAsData[A:Bits](v: Vec[Bit], A: Bits[A]) extends Primitive[A] {
  override val isEphemeral: Boolean = true

  @rig override def rewrite: A = v match {
    case Op(DataAsBits(data)) if data.tp =:= A => data.asInstanceOf[A]
    case _ => super.rewrite
  }
}
