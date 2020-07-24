package spatial.lang.api

import argon._
import forge.tags._
import spatial.node.{DataScannerNew, ScannerNew, SplitterEnd, SplitterStart}

trait MiscAPI {
  def void: Void = Void.c

  def * = new Wildcard

  // Deprecated: use the new Scan() syntax instead
  /* @api def Scan(bv: U32) = stage(ScannerNew(bv, 1))
  @api def Scan(bvs: U32*): List[Counter[I32]] = {
    bvs.map{ bv => stage(ScannerNew(bv, 1)) }.toList
  } */
  @api def Scan(par: scala.Int, count: I32, mode: String, bvs: U32*): List[Counter[I32]] = {
    val n = bvs.size
    bvs.zipWithIndex.map{ case (bv, i) => 
      if (i == n-1) {
        List(stage(ScannerNew(count, bv, par, true)), stage(ScannerNew(count, bv, 1, false)))
      } else { 
        List(stage(ScannerNew(count, bv, 1, true)), stage(ScannerNew(count, bv, 1, false)))
      }
    }.toList.flatten
  }
  @api def DataScan(dat: I32) : List[Counter[I32]] = {
    List(stage(DataScannerNew(dat, false)), stage(DataScannerNew(dat, true)))
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
