package spatial.lang

import argon._
import forge.tags._
import spatial.node._

@ref class Circ[_A:Bits,_B:Bits] extends Ref[Any,Circ[_A,_B]] {
  type A = _A
  type B = _B
  val aEv: Bits[A] = implicitly[Bits[A]]
  val bEv: Bits[B] = implicitly[Bits[B]]

  override protected val __neverMutable: Boolean = true

  private var numApps = 0
  def getNumApps: Int = numApps

  @api def apply(x: A): B = {
    val id = numApps
    numApps += 1
    stage(CircApply(this,id,x))
  }

  //@api def duplicate(): Circ[A,B] = Circ(func)
}

object Circ {
  @api def apply[A:Bits,B:Bits](func: A => B) = stage(CircNew(func))
}
