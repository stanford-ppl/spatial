package spatial.node

import forge.tags._
import spatial.lang._

@op case class Mux[A:Bits](s: Bit, a: Bits[A], b: Bits[A]) extends Primitive[A]

@op case class DataAsBits[A](a: Bits[A])(implicit val tV: Vec[Bit]) extends Primitive[Vec[Bit]] {
  override val isTransient: Boolean = true
}
@op case class BitsAsData[A:Bits](v: Vec[Bit], tA: Bits[A]) extends Primitive[A] {
  override val isTransient: Boolean = true
}
