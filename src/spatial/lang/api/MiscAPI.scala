package spatial.lang.api

import argon._
import forge.tags._
import spatial.metadata.retiming._
import spatial.node.{DelayLine, RetimeGate}

trait MiscAPI {
  def void: Void = Void.c

  def * = new Wildcard

  implicit class TextOps(t: Text) {
    @api def map[R:Type](f: U8 => R): Tensor1[R] = Tensor1.tabulate(t.length){i => f(t(i)) }

    @api def toCharArray: Tensor1[U8] = t.map{c => c}
  }

  @api def retimeGate(): Void = {
    stage(RetimeGate())
  }

  @api def retime[T:Bits](delay: scala.Int, payload: Bits[T]): T = {
    if (delay < 0) {
      throw new IllegalArgumentException("Attempted to create a delayline with delay < 0")
    } else if (delay == 0) {
      payload.unbox
    } else {
      val x = stage(DelayLine(delay, payload))
      x.asInstanceOf[Sym[_]].userInjectedDelay = true
      x
    }
  }
}
