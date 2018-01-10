package pcc
package ir
package memories

/** Symbols **/
abstract class Mem[A,C[_<:A]](eid: Int)(implicit ev: C[A] <:< Mem[A,C]) extends Sym[C[A]](eid) {
  final override def isPrimitive: Boolean = false
}

abstract class RemoteMem[A,C[_<:A]](eid: Int)(implicit ev: C[A] <:< RemoteMem[A,C]) extends Mem[A,C](eid)
abstract class LocalMem[A,C[_<:A]](eid: Int)(implicit ev: C[A] <:< LocalMem[A,C]) extends Mem[A,C](eid)
