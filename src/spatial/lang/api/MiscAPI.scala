package spatial.lang.api

import argon._
import forge.tags._
import spatial.node._
// import spatial.node.{DataScannerNew, ScannerNew, SplitterEnd, SplitterStart}
import spatial.lang.types._

trait MiscAPI {
  def void: Void = Void.c

  def * = new Wildcard

  // Deprecated: use the new Scan() syntax instead
  /* @api def Scan(bv: U32) = stage(ScannerNew(bv, 1))
  @api def Scan(bvs: U32*): List[Counter[I32]] = {
    bvs.map{ bv => stage(ScannerNew(bv, 1)) }.toList
  } */
  @api def gen_bitvector[Local[T]<:LocalMem1[T,Local]](shift: scala.Int, // Static config params
    max: I32, len: I32, indices: Local[I32], // Runtime config and two outputs
    bv: Local[U32])  : Unit = {
      val params = stage(CoalesceStoreParams(max, len))
      stage(BitVecGeneratorNoTree(shift, indices, bv, params))
    }
  @api def gen_bitvector_tree[Local[T]<:LocalMem1[T,Local]](shift: scala.Int, // Static config params
    len: I32, indices: Local[I32], // Runtime config and two outputs
    bv: Local[U32], prevLen: Local[I32], last: Local[Bit])  : Unit = {
      val params = stage(CoalesceStoreParams(len, len))
      stage(BitVecGeneratorTree(shift, indices, bv, prevLen, last, params))
    }

  @api def Scan(par: scala.Int, count: I32, mode: String, bvs: U32*): List[Counter[I32]] = {
    val n = bvs.size
    bvs.zipWithIndex.map{ case (bv, i) => 
      if (i == n-1) {
        List(stage(ScannerNew(count, bv, 1, true, mode)), stage(ScannerNew(count, bv, par, false, mode)))
      } else { 
        List(stage(ScannerNew(count, bv, 1, true, mode)), stage(ScannerNew(count, bv, 1, false, mode)))
      }
    }.toList.flatten
  }
  @api def DataScan(count: I32, dat: I32) : List[Counter[I32]] = {
    List(stage(DataScannerNew(count, dat, false)), stage(DataScannerNew(count, dat, true)))
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
