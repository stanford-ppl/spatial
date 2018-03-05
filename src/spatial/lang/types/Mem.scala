package spatial.lang.types

import core._
import spatial.lang.Top

trait Mem[A,C[_]] extends Top[C[A]] with Ref[Any,C[A]] {
  val evMem: C[A] <:< Mem[A,C]
  implicit val tA: Bits[A]

  override def isPrimitive: Boolean = false
}

trait RemoteMem[A,C[_]] extends Mem[A,C] {
  val evMem: C[A] <:< RemoteMem[A,C]

}

trait LocalMem[A,C[_]] extends Mem[A,C] {
  val evMem: C[A] <:< LocalMem[A,C]
  private implicit val evv: C[A] <:< Mem[A,C] = evMem

}
