package spatial.node

import argon._
import forge.tags._
import spatial.lang._

/** Allocation of any black box */
abstract class Alloc[T:Type] extends AccelOp[T]
object Alloc {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_: Alloc[_]) => Some(x)
    case _ => None
  }
}

/** Memory allocation */
abstract class MemAlloc[A:Bits,C[T]](
    mutable: Boolean = true
    )(implicit C: Type[C[A]])
  extends Alloc[C[A]] {

  val A: Bits[A] = Bits[A]

  def dims: Seq[I32]
  def rank: Int = dims.length
  override def effects: Effects = if (mutable) Effects.Mutable else super.effects
}
object MemAlloc {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_: MemAlloc[_,_]) => Some(x)
    case _ => None
  }
}

abstract class MemAlias[A, Src[T], Alias[T]](implicit Alias: Type[Alias[A]]) extends Alloc[Alias[A]] {
  def mem: Src[A]
  def mutable: Boolean
  def A: Type[A]
  def Src: Type[Src[A]]
  override def effects: Effects = if (mutable) Effects.Mutable else super.effects
}

/** A dense alias of an allocated memory.
  *
  * @param mem The memory being aliased.
  * @param ranges View ranges for this alias.
  * @param mutable True if this alias is mutable.
  * @param A The type of the element in the memory.
  * @param Src The type of the memory being aliased.
  * @param Alias The type of the alias (can be a different rank than the target).
  */
@op case class MemDenseAlias[A,Src[T],Alias[T]](
    mem:     Src[A],
    ranges:  Seq[Series[Idx]],
    mutable: Boolean = true
  )(implicit
    val A:     Type[A],
    val Src:   Type[Src[A]],
    val Alias: Type[Alias[A]])
  extends MemAlias[A,Src,Alias] {
  def rank: Int = ranges.count(r => !r.isUnit)
}

/** A sparse alias of an allocated memory
  *
  * @param mem The memory being aliased.
  * @param addr The sparse addresses for this alias.
  * @param mutable True if this alias is mutable.
  * @param A The type of element in this memory.
  * @param Addr The type of the memory holding the addresses.
  * @param Src The type of the memory being aliased.
  * @param Alias The type of the alias (can be different rank than the target, should be rank 1).
  */
@op case class MemSparseAlias[A,Addr[T],Src[T],Alias[T]](
    mem:  Src[A],
    addr: Addr[Idx],
    mutable: Boolean = true
  )(implicit
    val A: Type[A],
    val Addr: Type[Addr[Idx]],
    val Src: Type[Src[A]],
    val Alias: Type[Alias[A]])
  extends MemAlias[A,Src,Alias] {
  def rank: Int = 1
}


@op case class MemDim(mem: Sym[_], d: Int) extends Primitive[I32] {
  override val isTransient: Boolean = true
}

@op case class MemRank(mem: Sym[_]) extends Primitive[I32] {
  override val isTransient: Boolean = true
}
