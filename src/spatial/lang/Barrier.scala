package spatial.lang

import argon._
import forge.Ptr
import forge.tags._
import spatial.node._

@ref class Barrier[A:Bits] extends Ref[Ptr[Any],Barrier[A]] {
  override def __neverMutable: Boolean = true
  //  val Void: Bits[Void] = Bits[Void]
//  private implicit val evA: Token <:< Bits[Token] = Bits[Token].box
//  override val evMem: Barrier <:< LocalMem[Token,Barrier] = implicitly[Barrier <:< LocalMem[Token,Barrier]]

  // --- Infix Methods
  @api def push: BarrierTransaction = {
//    implicit val tV: BarrierTransaction = BarrierTransaction
    stage(BarrierPush(this))
  }
  @api def pop: BarrierTransaction = {
//    implicit val tV: BarrierTransaction = BarrierTransaction
    stage(BarrierPop(this))
  }
}

object Barrier {
  @api def apply[A:Bits](init: scala.Int, depth: scala.Int = 65536): Barrier[A] = stage(BarrierNew[A](init, depth))
}

@ref class BarrierTransaction extends Top[BarrierTransaction] with Ref[Void,BarrierTransaction] {override def __neverMutable: Boolean = true}
