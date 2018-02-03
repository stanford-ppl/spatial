package pcc.node

import pcc.core._
import pcc.lang._

/** Allocation of any black box **/
abstract class Alloc[T:Sym] extends AccelOp[T]
object Alloc {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_: Alloc[_]) => Some(x)
    case _ => None
  }
}

/** Memory allocation **/
abstract class Memory[T:Sym] extends Alloc[T] {
  def dims: Seq[I32]
  def rank: Int = dims.length
}
object Memory {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_: Memory[_]) => Some(x)
    case _ => None
  }
}
