package spatial.lang

import argon._
import forge.tags._
import spatial.node._
import spatial.lang.types._

abstract class DRAM[A:Bits,C[T]](implicit val evMem: C[A] <:< DRAM[A,C]) extends Top[C[A]] with RemoteMem[A,C] with TensorMem[A] {
  val A: Bits[A] = Bits[A]

  protected def M1: Type[DRAM1[A]] = implicitly[Type[DRAM1[A]]]
  protected def M2: Type[DRAM2[A]] = implicitly[Type[DRAM2[A]]]
  protected def M3: Type[DRAM3[A]] = implicitly[Type[DRAM3[A]]]
  protected def M4: Type[DRAM4[A]] = implicitly[Type[DRAM4[A]]]
  protected def M5: Type[DRAM5[A]] = implicitly[Type[DRAM5[A]]]
  protected def M6: Type[DRAM6[A]] = implicitly[Type[DRAM6[A]]]
  def rank: Seq[Int]

  @api def dims: Seq[I32] = Seq.tabulate(rank.length){d => stage(MemDim(this,rank(d))) }

  /** Returns dim0 of this DRAM, or else 1 if DRAM is lower dimensional */
  @api override def dim0: I32 = dims.head // Why is this not indexOrElse?

  /** Returns the 64-bit address of this DRAM */
  @api def address: I64 = stage(DRAMAddress(me))

  @api def isAlloc: Bit = stage(DRAMIsAlloc(this))
  @api def dealloc: Void = stage(DRAMDealloc(this))

  @api override def neql(that: C[A]): Bit = {
    error(this.ctx, "Native comparison of DRAMs is unsupported. Use getMem to extract data.")
    error(this.ctx)
    super.neql(that)
  }
  @api override def eql(that: C[A]): Bit = {
    error(this.ctx, "Native comparision of DRAMs is unsupported. Use getMem to extract data.")
    error(this.ctx)
    super.eql(that)
  }
}
object DRAM {
  /** Allocates a 1-dimensional [[DRAM1]] with capacity of `length` elements of type A. */
  @api def apply[A:Bits](length: I32): DRAM1[A] = stage(DRAMHostNew[A,DRAM1](Seq(length),zero[A]))

  /** Allocates a 2-dimensional [[DRAM2]] with `rows` x `cols` elements of type A. */
  @api def apply[A:Bits](rows: I32, cols: I32): DRAM2[A] = stage(DRAMHostNew[A,DRAM2](Seq(rows,cols),zero[A]))

  /** Allocates a 3-dimensional [[DRAM3]] with the given dimensions and elements of type A. */
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32): DRAM3[A] = stage(DRAMHostNew[A,DRAM3](Seq(d0,d1,d2),zero[A]))

  /** Allocates a 4-dimensional [[DRAM4]] with the given dimensions and elements of type A. */
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32, d3: I32): DRAM4[A] = stage(DRAMHostNew[A,DRAM4](Seq(d0,d1,d2,d3),zero[A]))

  /** Allocates a 5-dimensional [[DRAM5]] with the given dimensions and elements of type A. */
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32, d3: I32, d4: I32): DRAM5[A] = stage(DRAMHostNew[A,DRAM5](Seq(d0,d1,d2,d3,d4),zero[A]))
  
  /** Allocates a 6-dimensional [[DRAM6]] with the given dimensions and elements of type A. */
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32, d3: I32, d4: I32, d5: I32): DRAM6[A] = stage(DRAMHostNew[A,DRAM6](Seq(d0,d1,d2,d3,d4,d5),zero[A]))
}

/** A 1-dimensional [[DRAM]] with elements of type A. */
@ref class DRAM1[A:Bits] extends DRAM[A,DRAM1] with Ref[Array[Any],DRAM1[A]] with Mem1[A,DRAM1] {
  def rank: Seq[Int] = Seq(0)
  @api def length: I32 = dims.head
  @api override def size: I32 = dims.head

  @api def alloc(len: I32): Void = stage(DRAMAlloc(this, Seq(len)))

  /** Creates a view of a sparse region of this DRAM1 for use in scatter and gather transfers. */
  @api def apply[W:INT](addrs: SRAM1[Ind[W]]): DRAMSparseTile[A] = apply(addrs, addrs.length, zero[Ind[W]])
  @api def apply[W:INT](addrs: SRAM1[Ind[W]], size: Int): DRAMSparseTile[A] = apply(addrs, size.to[I32], zero[Ind[W]])
  @api def apply[W:INT,W2:INT](addrs: SRAM1[Ind[W]], size: Ind[W2]): DRAMSparseTile[A] = apply(addrs, size, zero[Ind[W]])
  /** Creates a view of a sparse region of this DRAM1 for use in scatter and gather transfers, with number of addresses to operate on. */
  @api def apply[W:INT,W2:INT](addrs: SRAM1[Ind[W]], size: Ind[W2], origin: Ind[W]): DRAMSparseTile[A] = {
    stage(MemSparseAlias[A,SRAM1,W,DRAM1,DRAMSparseTile,W2](this,addrs,size, origin))
  }
  /** Creates a view of a sparse region of this DRAM1 for use in scatter and gather transfers. */
  @api def apply[W:INT](addrs: FIFO[Ind[W]]): DRAMSparseTile[A] = apply(addrs, addrs.numel, zero[Ind[W]])
  @api def apply[W:INT](addrs: FIFO[Ind[W]], size: Int): DRAMSparseTile[A] = apply(addrs, size.to[I32], zero[Ind[W]])
  @api def apply[W:INT,W2:INT](addrs: FIFO[Ind[W]], size: Ind[W2]): DRAMSparseTile[A] = apply(addrs, size, zero[Ind[W]])
  /** Creates a view of a sparse region of this DRAM1 for use in scatter and gather transfers, with number of addresses to operate on. */
  @api def apply[W:INT,W2:INT](addrs: FIFO[Ind[W]], size: Ind[W2], origin: Ind[W]): DRAMSparseTile[A] = {
    stage(MemSparseAlias[A,FIFO,W,DRAM1,DRAMSparseTile,W2](this,addrs,size,origin))
  }
  /** Creates a view of a sparse region of this DRAM1 for use in scatter and gather transfers. */
  @api def apply[W:INT](addrs: LIFO[Ind[W]]): DRAMSparseTile[A] = apply(addrs, addrs.numel, zero[Ind[W]])
  @api def apply[W:INT](addrs: LIFO[Ind[W]], size: Int): DRAMSparseTile[A] = apply(addrs, size.to[I32], zero[Ind[W]])
  @api def apply[W:INT,W2:INT](addrs: LIFO[Ind[W]], size: Ind[W2]): DRAMSparseTile[A] = apply(addrs, size, zero[Ind[W]])
  /** Creates a view of a sparse region of this DRAM1 for use in scatter and gather transfers, with number of addresses to operate on. */
  @api def apply[W:INT,W2:INT](addrs: LIFO[Ind[W]], size: Ind[W2], origin: Ind[W]): DRAMSparseTile[A] = {
    stage(MemSparseAlias[A,LIFO,W,DRAM1,DRAMSparseTile,W2](this,addrs,size,origin))
  }

  /** Creates a dense, burst transfer from the on-chip `local` to this DRAM's region of main memory. */
  @api def store[Local[T]<:LocalMem1[T,Local]](local: Local[A])(implicit tp: Type[Local[A]]): Void = {
    stage(DenseTransfer(this,local,isLoad = false))
  }

  /** Creates a dense, burst transfer from the on-chip `local` to this DRAM's region of main memory.
    * Restricted to the first `len` elements in local.
    **/
  @api def store(local: SRAM1[A], len: I32): Void = {
    stage(DenseTransfer(this,local.apply(0::len),isLoad = false))
  }

  /** Creates an aligned dense, burst transfer from the on-chip `local` to this DRAM's region of main memory. */
  @api def alignstore[Local[T]<:LocalMem1[T,Local]](local: Local[A])(implicit tp: Type[Local[A]]): Void = {
    stage(DenseTransfer(this,local,isLoad = false, forceAlign=true))
  }

  /** Creates a coalescing transfer from an on-chip data/valid memory with a 
   *  defined maximum size
    **/
  @api def coalesce[Local[T]<:LocalMem1[T,Local]](base: I32, data: Local[A], valid: Local[Bit], len: I32)(implicit tp: Type[Local[A]]): Void = {
    // Since the only use of len and base are the Coalesce node itself, this lowering rule kills that node and DCE kills len and base.
    //   Hacking this for now by catching it in a Unique node
    val params = stage(CoalesceStoreParams(len, base))
    stage(CoalesceStore(this, data, valid, params, 1))
  }
  @api def coalesce_vec[Local[T]<:LocalMem1[T,Local]](base: I32, data: Local[A], valid: Local[Bit], len: I32)(implicit tp: Type[Local[A]]): Void = {
    // Since the only use of len and base are the Coalesce node itself, this lowering rule kills that node and DCE kills len and base.
    //   Hacking this for now by catching it in a Unique node
    val params = stage(CoalesceStoreParams(len, base))
    stage(CoalesceStore(this, data, valid, params, 16))
  }
  @api def stream_store_vec[Local[T]<:LocalMem1[T,Local]](base: I32, data: Local[A], len: I32)(implicit tp: Type[Local[A]]): Void = {
    // Since the only use of len and base are the Coalesce node itself, this lowering rule kills that node and DCE kills len and base.
    //   Hacking this for now by catching it in a Unique node
    val params = stage(CoalesceStoreParams(len, base))
    stage(StreamStore(this, data, params, 16))
  }
  @api def stream_store_scal[Local[T]<:LocalMem1[T,Local]](base: I32, data: Local[A], len: I32)(implicit tp: Type[Local[A]]): Void = {
    // Since the only use of len and base are the Coalesce node itself, this lowering rule kills that node and DCE kills len and base.
    //   Hacking this for now by catching it in a Unique node
    val params = stage(CoalesceStoreParams(len, base))
    stage(StreamStore(this, data, params, 1))
  }
  //@api def dynstore[Local[T]<:LocalMem1[T,Local]](base: I32, data: Local[A], last: Local[Bit])(implicit tp: Type[Local[A]]): Void = {
    //// Since the only use of len and base are the Coalesce node itself, this lowering rule kills that node and DCE kills len and base.
    ////   Hacking this for now by catching it in a Unique node
    //val params = stage(CoalesceStoreParams(base, base))
    //stage(DynamicStore(this, data, last, params, 1))
  //}
  @api def dynstore_vec[Local[T]<:LocalMem1[T,Local]](base: I32, data: Local[A], last: Local[Bit], num_stored: Local[I32])(implicit tp: Type[Local[A]]): Void = {
    // Since the only use of len and base are the Coalesce node itself, this lowering rule kills that node and DCE kills len and base.
    //   Hacking this for now by catching it in a Unique node
    val params = stage(CoalesceStoreParams(base, base))
    stage(DynamicStore(this, data, last, params, num_stored, 16))
  }
  @api def stream_load_vec[Local[T]<:LocalMem1[T,Local]](base: I32, len: I32, out: Local[I32], pad:Int=(-1000))(implicit tp: Type[Local[A]]): Void = {
    // Since the only use of len and base are the Coalesce node itself, this lowering rule kills that node and DCE kills len and base.
    //   Hacking this for now by catching it in a Unique node
    val params = stage(CoalesceStoreParams(base, len))
    stage(StreamLoad(this, params, out, 16, false, pad))
  }
  @api def stream_load_vec_comp[Local[T]<:LocalMem1[T,Local]](base: I32, len: I32, out: Local[I32], pad:Int=(-1000))(implicit tp: Type[Local[A]]): Void = {
    // Since the only use of len and base are the Coalesce node itself, this lowering rule kills that node and DCE kills len and base.
    //   Hacking this for now by catching it in a Unique node
    val params = stage(CoalesceStoreParams(base, len))
    stage(StreamLoad(this, params, out, 16, true, pad))
  }
}

object DRAM1 {
  @api def apply[A:Bits]: DRAM1[A] = stage(DRAMAccelNew[A,DRAM1](1))
}

/** A 2-dimensional [[DRAM]] with elements of type A. */
@ref class DRAM2[A:Bits] extends DRAM[A,DRAM2] with Ref[Array[Any],DRAM2[A]] with Mem2[A,DRAM1,DRAM2] {
  def rank: Seq[Int] = Seq(0,1)
  @api def rows: I32 = dims.head
  @api def cols: I32 = dim1

  @api def alloc(rows: I32, cols: I32): Void = stage(DRAMAlloc(this, Seq(rows, cols)))


  /** Creates a dense, burst transfer from the SRAM2 `data` to this region of main memory. */
  @api def store(data: SRAM2[A]): Void = {
    stage(DenseTransfer(this, data, isLoad = false))
  }

  /** Creates a dense, burst transfer from the RegFile2 `data` to this region of main memory. */
  @api def store(data: RegFile2[A]): Void = {
    stage(DenseTransfer(this, data, isLoad = false))
  }

  /** Creates an aligned dense, burst transfer from the SRAM2 `data` to this region of main memory. */
  @api def alignstore(data: SRAM2[A]): Void = {
    stage(DenseTransfer(this, data, isLoad = false, forceAlign=true))
  }

}

object DRAM2 {
  @api def apply[A:Bits]: DRAM2[A] = stage(DRAMAccelNew[A,DRAM2](2))
}

/** A 3-dimensional [[DRAM]] with elements of type A. */
@ref class DRAM3[A:Bits] extends DRAM[A,DRAM3] with Ref[Array[Any],DRAM3[A]] with Mem3[A,DRAM1,DRAM2,DRAM3] {
  def rank: Seq[Int] = Seq(0,1,2)

  @api def alloc(d0: I32, d1: I32, d2: I32): Void = stage(DRAMAlloc(this, Seq(d0, d1, d2)))

  /** Creates a dense, burst transfer from the SRAM3 `data` to this region of main memory. */
  @api def store(data: SRAM3[A]): Void = {
    stage(DenseTransfer(this, data, isLoad = false))
  }

  /** Creates a dense, burst transfer from the RegFile3 `data` to this region of main memory. */
  @api def store(data: RegFile3[A]): Void = {
    stage(DenseTransfer(this, data, isLoad = false))
  }

  /** Creates an aligned dense, burst transfer from the SRAM3 `data` to this region of main memory. */
  @api def alignstore(data: SRAM3[A]): Void = {
    stage(DenseTransfer(this, data, isLoad = false, forceAlign=true))
  }
}

object DRAM3 {
  @api def apply[A:Bits]: DRAM3[A] = stage(DRAMAccelNew[A,DRAM3](3))
}

/** A 4-dimensional [[DRAM]] with elements of type A. */
@ref class DRAM4[A:Bits] extends DRAM[A,DRAM4] with Ref[Array[Any],DRAM4[A]] with Mem4[A,DRAM1,DRAM2,DRAM3,DRAM4] {
  def rank: Seq[Int] = Seq(0,1,2,3)

  @api def alloc(d0: I32, d1: I32, d2: I32, d3: I32): Void = stage(DRAMAlloc(this, Seq(d0, d1, d2, d3)))

  /** Creates a dense, burst transfer from the SRAM4 `data` to this region of main memory. */
  @api def store(data: SRAM4[A]): Void = {
    stage(DenseTransfer(this, data, isLoad = false))
  }
  /** Creates an aligned dense, burst transfer from the SRAM4 `data` to this region of main memory. */
  @api def alignstore(data: SRAM4[A]): Void = {
    stage(DenseTransfer(this, data, isLoad = false, forceAlign=true))
  }
}

object DRAM4 {
  @api def apply[A:Bits]: DRAM4[A] = stage(DRAMAccelNew[A,DRAM4](4))
}

/** A 5-dimensional [[DRAM]] with elements of type A. */
@ref class DRAM5[A:Bits] extends DRAM[A,DRAM5] with Ref[Array[Any],DRAM5[A]] with Mem5[A,DRAM1,DRAM2,DRAM3,DRAM4,DRAM5] {
  def rank: Seq[Int] = Seq(0,1,2,3,4)

  @api def alloc(d0: I32, d1: I32, d2: I32, d3: I32, d4: I32): Void = stage(DRAMAlloc(this, Seq(d0, d1, d2, d3, d4)))

  /** Creates a dense, burst transfer from the SRAM5 `data` to this region of main memory. */
  @api def store(data: SRAM5[A]): Void = {
    stage(DenseTransfer(this, data, isLoad = false))
  }
  /** Creates an aligned dense, burst transfer from the SRAM5 `data` to this region of main memory. */
  @api def alignstore(data: SRAM5[A]): Void = {
    stage(DenseTransfer(this, data, isLoad = false, forceAlign=true))
  }
}

object DRAM5 {
  @api def apply[A:Bits]: DRAM5[A] = stage(DRAMAccelNew[A,DRAM5](5))
}



/** A 5-dimensional [[DRAM]] with elements of type A. */
@ref class DRAM6[A:Bits] extends DRAM[A,DRAM6] with Ref[Array[Any],DRAM6[A]] with Mem6[A,DRAM1,DRAM2,DRAM3,DRAM4,DRAM5,DRAM6] {
  def rank: Seq[Int] = Seq(0,1,2,3,4,5)

  @api def alloc(d0: I32, d1: I32, d2: I32, d3: I32, d4: I32, d5: I32): Void = stage(DRAMAlloc(this, Seq(d0, d1, d2, d3, d4, d5)))

  /** Creates a dense, burst transfer from the SRAM6 `data` to this region of main memory. */
  @api def store(data: SRAM6[A]): Void = {
    stage(DenseTransfer(this, data, isLoad = false))
  }
  /** Creates an aligned dense, burst transfer from the SRAM6 `data` to this region of main memory. */
  @api def alignstore(data: SRAM6[A]): Void = {
    stage(DenseTransfer(this, data, isLoad = false, forceAlign=true))
  }
}

object DRAM6 {
  @api def apply[A:Bits]: DRAM6[A] = stage(DRAMAccelNew[A,DRAM6](6))
}







/** A sparse, 1-dimensional region of DRAM with elements of type A. */
@ref class DRAMSparseTile[A:Bits] extends DRAM[A,DRAMSparseTile] with Ref[Array[Any],DRAMSparseTile[A]] {
  def rank: Seq[Int] = Seq(0)

  /** Creates a sparse transfer from the on-chip `data` to this sparse region of main memory. */
  @api def scatter[Local[T]<:LocalMem1[T,Local]](local: Local[A])(implicit L: Type[Local[A]]): Void = {
    stage(SparseTransfer(this,local,isGather = false))
  }

}




// /** A sparse, 1-dimensional region of DRAM with elements of type A. */
// @ref class DRAMCoalTile[A:Bits] extends DRAM[A,DRAMCoalTile] with Ref[Array[Any],DRAMCoalTile[A]] {
  // def rank: Seq[Int] = Seq(0)
// 
  // /** Creates a coalescing transfer from the on-chip `data` to this sparse region of main memory. */
  // @api def coalesce[Local[T]<:LocalMem1[T,Local]](data: Local[A], valBits: Local[Bit])(implicit L: Type[Local[A]]): Void = {
    // stage(CoalesceStore(this,data,valBits))
  // }
// }


