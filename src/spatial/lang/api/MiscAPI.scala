package spatial.lang.api

import argon._
import forge.tags._
import spatial.node.{ScannerNew, SplitterEnd, SplitterStart}

trait MiscAPI {
  def void: Void = Void.c

  def * = new Wildcard

  @api def Scan(bv: U512) = stage(ScannerNew(bv, 1))
  @api def Scan(bvs: U512*): List[Counter[I32]] = {
    bvs.map{ bv => stage(ScannerNew(bv, 1)) }.toList
  }
  @api def Scan(par: scala.Int, bvs: U512*): List[Counter[I32]] = {
    bvs.map{ bv => stage(ScannerNew(bv, par)) }.toList
  }

  @api def splitter(addr: I32)(func: => Any): Unit = {
    stage(SplitterStart(Seq(addr)))
    func
    stage(SplitterEnd(Seq(addr)))
  }

  implicit class TextOps(t: Text) {
    @api def map[R:Type](f: U8 => R): Tensor1[R] = Tensor1.tabulate(t.length){i => f(t(i)) }

    @api def toCharArray: Tensor1[U8] = t.map{c => c}
  }

}
