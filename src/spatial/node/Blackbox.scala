package spatial.node

import argon._
import forge.tags._
import argon.node.{Alloc, DSLOp, Primitive, StructAlloc}
import spatial.lang._

abstract class FunctionBlackbox[R:Type] extends Control[R]

/** Black box which must be expanded early in compiler (after initial analyses). */
abstract class EarlyBlackbox[R:Type] extends FunctionBlackbox[R] {
  override def cchains = Nil
  override def iters = Nil
  override def bodies = Nil
  @rig def lower(old:Sym[R]): R
}

@op case class GEMMBox[T:Num](
  cchain: CounterChain,
  y:     SRAM2[T],
  a:     SRAM2[T],
  b:     SRAM2[T],
  c:     T,
  alpha: T,
  beta:  T,
  i:     I32,
  j:     I32,
  mt:    I32,
  nt:    I32,
  iters: Seq[I32]
) extends FunctionBlackbox[Void] {
  override def cchains = Seq(cchain -> iters)
  override def bodies = Nil
  override def effects: Effects = Effects.Writes(y)
}

abstract class PrimitiveBlackboxUse[A:Struct,B:Struct] extends Primitive[B] {
  override def effects = Effects.Unique
}
abstract class CtrlBlackboxUse[A:StreamStruct,B:StreamStruct](ens: Set[Bit]) extends EnControl[B] {
  override def iters: Seq[I32] = Seq()
  override def cchains = Seq()
  override def bodies = Seq()
  override def effects = Effects.Unique andAlso Effects.Mutable
}
abstract class BlackboxImpl[T:Type,A:Type,B:Type](func: Lambda1[A,B]) extends Alloc[T] {
  override def effects = Effects.Unique
  override def binds = super.binds + func.input
}

@op case class VerilogBlackbox[A:Struct,B:Struct](in: Bits[A]) extends PrimitiveBlackboxUse[A,B]
@op case class SpatialBlackboxUse[A:Struct,B:Struct](bbox: SpatialBlackbox[A,B], in: Bits[A]) extends PrimitiveBlackboxUse[A,B]

@op case class VerilogCtrlBlackbox[A:StreamStruct,B:StreamStruct](ens: Set[Bit], in: Bits[A]) extends CtrlBlackboxUse[A,B](ens)
@op case class SpatialCtrlBlackboxUse[A:StreamStruct,B:StreamStruct](ens: Set[Bit], bbox: SpatialCtrlBlackbox[A,B], in: Bits[A]) extends CtrlBlackboxUse[A,B](ens)

@op case class SpatialBlackboxImpl[A:Struct,B:Struct](func: Lambda1[A,B])(implicit val tA: Type[A], val tB: Type[B]) extends BlackboxImpl[SpatialBlackbox[A,B],A,B](func)
@op case class SpatialCtrlBlackboxImpl[A:StreamStruct,B:StreamStruct](func: Lambda1[A,B])(implicit val tA: Type[A], val tB: Type[B]) extends BlackboxImpl[SpatialCtrlBlackbox[A,B],A,B](func)






