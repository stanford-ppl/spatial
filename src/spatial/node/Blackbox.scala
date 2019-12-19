package spatial.node

import argon._
import forge.tags._
import argon.node.{Alloc, DSLOp, Primitive, StructAlloc}
import spatial.lang._

abstract class Blackbox[R:Type] extends Control[R]

/** Black box which must be expanded early in compiler (after initial analyses). */
abstract class EarlyBlackbox[R:Type] extends Blackbox[R] {
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
) extends Blackbox[Void] {
  override def cchains = Seq(cchain -> iters)
  override def bodies = Nil
  override def effects: Effects = Effects.Writes(y)
}


//@op case class GEMVBox() extends BlackBox
//@op case class CONVBox() extends BlackBox
//@op case class SHIFTBox(validAfter: Int) extends BlackBox

//@op case class VerilogBlackBox[A:Bits](ins: Seq[A])(implicit val tV: Vec[A]) extends Primitive[Vec[A]] {
@op case class VerilogBlackbox[A:Struct,B:Struct](in: Bits[A]) extends Primitive[B] {
  override def effects = Effects.Unique
}

@op case class VerilogCtrlBlackbox[A:StreamStruct,B:StreamStruct](in: Bits[A]) extends EnControl[B] {
  override def iters: Seq[I32] = Seq()
  var ens = Set()
  override def cchains = Seq()
  override def bodies = Seq()
  override def effects = Effects.Unique andAlso Effects.Mutable
}

@op case class SpatialBlackboxImpl[A:Struct,B:Struct](func: Lambda1[A,B])(implicit val tA: Type[A], val tB: Type[B]) extends Alloc[SpatialBlackbox[A,B]] {
  override def effects = Effects.Unique //andAlso Effects.Sticky
  override def binds = super.binds + func.input
}

@op case class SpatialBlackboxUse[A:Struct,B:Struct](bbox: SpatialBlackbox[A,B], in: A) extends Primitive[B] {
  override def effects = Effects.Unique //andAlso Effects.Sticky
}

@op case class SpatialCtrlBlackboxImpl[A:StreamStruct,B:StreamStruct](func: Lambda1[A,B])(implicit val tA: Type[A], val tB: Type[B]) extends Alloc[SpatialCtrlBlackbox[A,B]] {
  override def effects = Effects.Unique //andAlso Effects.Sticky
  override def binds = super.binds + func.input
}

@op case class SpatialCtrlBlackboxUse[A:StreamStruct,B:StreamStruct](bbox: SpatialCtrlBlackbox[A,B], in: A) extends Primitive[B] {
  override def effects = Effects.Unique //andAlso Effects.Sticky
}



