package spatial.lang
package memories

import core._

abstract class Mem[A:Bits,C[_<:A]](implicit ev: C[A] <:< Mem[A,C]) extends Ref[C[A]] {
  val tA: Bits[A] = tbits[A]
  type AI = tA.I

  final override def isPrimitive: Boolean = false
  override def typeArgs = Seq(tA)
}

abstract class RemoteMem[A:Bits,C[_<:A]](implicit ev: C[A] <:< RemoteMem[A,C]) extends Mem[A,C]
abstract class LocalMem[A:Bits,C[_<:A]](implicit ev: C[A] <:< LocalMem[A,C]) extends Mem[A,C]
