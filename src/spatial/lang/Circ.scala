package spatial.lang

import argon._
import forge.tags._
import spatial.node._

@ref class Circ[A:Bits,B:Bits] extends Ref[Any,Circ[A,B]] {
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
  @api def apply[A:Bits,B:Bits](func: A => B): Circ[A,B] = {
    val arg = boundVar[A]
    val blk = stageLambda1(arg){ func(arg) }
    stage(CircNew(blk))
  }
}
