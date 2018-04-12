package spatial.node

import forge.tags._
import spatial.lang._

@op case class Mux[A:Bits](s: Bit, a: Bits[A], b: Bits[A]) extends Primitive[A]
@op case class OneHotMux[A:Bits](sels: Seq[Bit], vals: Seq[Bits[A]]) extends Primitive[A]

@op case class DataAsBits[A](A: Bits[A])(implicit val tV: Vec[Bit]) extends Primitive[Vec[Bit]] {
  override val isEphemeral: Boolean = true
}
@op case class BitsAsData[A:Bits](v: Vec[Bit], A: Bits[A]) extends Primitive[A] {
  override val isEphemeral: Boolean = true
}
