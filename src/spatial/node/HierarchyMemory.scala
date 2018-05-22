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
  def rank: Seq[Int] = Seq.tabulate(dims.length){i => i}
  override def effects: Effects = if (mutable) Effects.Mutable else super.effects
}
object MemAlloc {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_: MemAlloc[_,_]) => Some(x)
    case _ => None
  }
}

abstract class MemAlias[A, Src[T], Alias[T]](implicit Alias: Type[Alias[A]]) extends Alloc[Alias[A]] {
  def mem: Seq[Src[A]]
  def rank: Seq[Int]
  def rawRank: Seq[Int]
  def mutable: Boolean
  def A: Type[A]
  def Src: Type[Src[A]]
  override def effects: Effects = if (mutable) Effects.Mutable else super.effects
}

/** A dense alias of an allocated memory.
  *
  * @param mem The memory being aliased.
  * @param ranges View ranges for this alias.
  * @param A The type of the element in the memory.
  * @param Src The type of the memory being aliased.
  * @param Alias The type of the alias (can be a different rank than the target).
  */
@op case class MemDenseAlias[A,Src[T],Alias[T]](
    cond:    Seq[Bit],
    mem:     Seq[Src[A]],
    ranges:  Seq[Seq[Series[Idx]]]
  )(implicit
    val A:     Type[A],
    val Src:   Type[Src[A]],
    val Alias: Type[Alias[A]])
  extends MemAlias[A,Src,Alias] {

  def rank: Seq[Int] = ranges.head.zipWithIndex.collect{case(r,i) if (!r.isUnit) => i}
  def rawRank: Seq[Int] = Seq.tabulate(ranges.head.length){i => i}
  val mutable = true

  override def aliases: Set[Sym[_]] = syms(mem)
}
object MemDenseAlias {
  @rig def apply[A,Src[T],Alias[T]](mem: Src[A], ranges: Seq[Series[Idx]])(implicit
    A: Type[A],
    Src: Type[Src[A]],
    Alias: Type[Alias[A]]
  ): MemDenseAlias[A,Src,Alias] = MemDenseAlias[A,Src,Alias](Seq(Bit(true)),Seq(mem),Seq(ranges))
}

/** A sparse alias of an allocated memory
  *
  * @param mem The memory being aliased.
  * @param addr The sparse addresses for this alias.
  * @param A The type of element in this memory.
  * @param Addr The type of the memory holding the addresses.
  * @param Src The type of the memory being aliased.
  * @param Alias The type of the alias (can be different rank than the target, should be rank 1).
  */
@op case class MemSparseAlias[A,Addr[T],Src[T],Alias[T]](
    cond: Seq[Bit],
    mem:  Seq[Src[A]],
    addr: Seq[Addr[I32]],
    size: Seq[I32]
  )(implicit
    val A:     Type[A],
    val Addr:  Type[Addr[I32]],
    val Src:   Type[Src[A]],
    val Alias: Type[Alias[A]])
  extends MemAlias[A,Src,Alias] {
  def rank: Seq[Int] = Seq(0)
  def rawRank: Seq[Int] = Seq(0)
  val mutable = true

  override def aliases: Set[Sym[_]] = syms(mem)
}
object MemSparseAlias {
  @rig def apply[A,Addr[T],Src[T],Alias[T]](mem: Src[A], addr: Addr[I32], size: I32)(implicit
    A:     Type[A],
    Addr:  Type[Addr[I32]],
    Src:   Type[Src[A]],
    Alias: Type[Alias[A]]
  ): MemSparseAlias[A,Addr,Src,Alias] = {
    MemSparseAlias[A,Addr,Src,Alias](Seq(Bit(true)),Seq(mem),Seq(addr),Seq(size))
  }
}

@op case class MemStart(mem: Sym[_], d: Int)extends Transient[I32]
@op case class MemStep(mem: Sym[_], d: Int) extends Transient[I32]
@op case class MemEnd(mem: Sym[_], d: Int) extends Transient[I32]
@op case class MemPar(mem: Sym[_], d: Int) extends Transient[I32]
@op case class MemLen(mem: Sym[_], d: Int) extends Transient[I32]

@op case class MemDim(mem: Sym[_], d: Int) extends Transient[I32]
@op case class MemRank(mem: Sym[_]) extends Transient[I32]
