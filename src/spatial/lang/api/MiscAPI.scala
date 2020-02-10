package spatial.lang.api

import argon._
import forge.tags._
import spatial.node.{ScannerNew, SplitterEnd, SplitterStart}

trait MiscAPI {
  def void: Void = Void.c

  def * = new Wildcard

  @api def Scan(bv: U512) = stage(ScannerNew(bv))

  @api def splitter(addr: I32)(func: => Any): Unit = {
    stage(SplitterStart(addr))
    func
    stage(SplitterEnd(addr))
  }

  implicit class TextOps(t: Text) {
    @api def map[R:Type](f: U8 => R): Tensor1[R] = Tensor1.tabulate(t.length){i => f(t(i)) }

    @api def toCharArray: Tensor1[U8] = t.map{c => c}
  }

}
