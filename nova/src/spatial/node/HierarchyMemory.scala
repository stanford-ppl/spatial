package spatial.node

import core._
import spatial.lang._

/** Allocation of any black box **/
abstract class Alloc[T:Type] extends AccelOp[T]
object Alloc {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_: Alloc[_]) => Some(x)
    case _ => None
  }
}

/** Memory allocation **/
abstract class MemAlloc[T:Type] extends Alloc[T] {
  def dims: Seq[I32]
  def rank: Int = dims.length
}
object MemAlloc {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_: MemAlloc[_]) => Some(x)
    case _ => None
  }
}
