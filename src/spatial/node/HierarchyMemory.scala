package spatial.node

import core._
import forge.tags._
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
abstract class MemAlloc[T:Type](mutable: Boolean = true) extends Alloc[T] {
  def dims: Seq[I32]
  def rank: Int = dims.length
  override def effects: Effects = if (mutable) Effects.Mutable else super.effects
}
object MemAlloc {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_: MemAlloc[_]) => Some(x)
    case _ => None
  }
}

@op case class MemDim(mem: Sym[_], d: Int) extends Primitive[I32] {
  override val isTransient: Boolean = true
}

@op case class MemRank(mem: Sym[_]) extends Primitive[I32] {
  override val isTransient: Boolean = true
}
