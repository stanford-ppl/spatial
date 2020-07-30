package spatial.node

import argon._
import argon.node._
import forge.tags._
import spatial.lang._
import spatial.metadata.memory._
import spatial.metadata.control._
import spatial.metadata.bounds._
import spatial.util.memops._
import spatial.util.spatialConfig
import spatial.util.modeling.target

//@op case class BitVecGenerator[Local[T]<:LocalMem1[T,Local]](tree: Boolean, shift: scala.Int,
  //max: I32, len: Local[I32], indices: Local[I32],
  //bv: Local[U32], last: Local[Bit])  extends FringeNode[I32,Void] {
    //override def effects = Effects.Writes(bv, last) andAlso Effects.Sticky andAlso Effects.Reads(indices)
    // override def effects = Effects.Writes(bv, last) andAlso Effects.Reads(len) andAlso Effects.Reads(indices) andAlso Effects.Sticky

  //}
@op case class BitVecGenerator[Local[T]<:LocalMem[T,Local]](
    tree:    Boolean,
    shift:   Int,
    indices: Local[I32],
    bv:      Local[U32],
    last:    Local[Bit],
    params:  Tup2[I32,I32], // max and len
    ens:     Set[Bit] = Set.empty,
  )//(implicit
    //val A:     Bits[A],
    //val Local: Type[Local[A]],
    //val Dram:  Type[Dram[A]])
  extends EarlyBlackBox[Void] {

  override def effects: Effects = Effects.Writes(bv, last) 
  @rig def lower(old:Sym[Void]): Void = BitVecGenerator.transfer(old, tree, shift, indices, bv, last, params, ens)
  //@rig def pars: Seq[I32] = {
    //val normalCounting: Boolean = dram.rawRank.last == dram.sparseRank.last
    //(dram.sparsePars().map(_._2) ++ {if (!normalCounting) Seq(I32(1)) else Nil }).toSeq
  //}
}

object BitVecGenerator {

  @virtualize
  @rig def transfer[Local[T]<:LocalMem[T,Local]](
    old:     Sym[Void],
    tree:    Boolean,
    shift:   Int,
    indices: Local[I32],
    bv:      Local[U32],
    last:    Local[Bit],
    params:  Tup2[I32,I32], // max and len
    ens:     Set[Bit],
  )//(implicit
    //A:     Bits[A],
    //Local: Type[Local[A]],
    //Dram:  Type[Dram[A]]
    // A:     Bits[A],
    // Local: Type[Local[A]])
  : Void = {
    // val addrs = dram.addrs[_32]()
    // val origin = dram.sparseOrigins[_32]().values.head
    // val p = addrs.sparsePars().values.head
    // val requestLength = dram.sparseLens().values.head

    // val rawRank: Int = dram.rawRank.length
    // val normalCounting: Boolean = dram.rawRank.last == dram.sparseRank.last
    // val pars: Map[Int,I32] = dram.sparsePars() ++ {if (!normalCounting) Seq(rawRank -> I32(1)) else Nil }
    // val p = pars.toSeq.maxBy(_._1)._2
    // TODO: fixthis
    // val p = 1
    // val bytesPerWord = A.nbits / 8 + (if (A.nbits % 8 != 0) 1 else 0)

    assert(spatialConfig.enablePIR)
    val max = params._1
    val len = params._2
    // val iters = requestLength

    val top = Stream {
      val indexFIFO = indices.asInstanceOf[Sym[_]] match {case Op(_:FIFONew[_]) => true; case _ => false}
      val bvFIFO    = bv.asInstanceOf[Sym[_]]      match {case Op(_:FIFONew[_]) => true; case _ => false}
      val lastFIFO  = last.asInstanceOf[Sym[_]]    match {case Op(_:FIFONew[_]) => true; case _ => false}

      // Coalesce
      //val setupBus = StreamOut[Tup2[I32,I32]](BVGenSetupBus())
      //val idxBus = StreamOut[I32](BVGenIdxBus())
      //val retBus = StreamIn[Tup2[U32,Bit]](BVGenRetBus)
      val setupBus = StreamOut[Tup2[I32,I32]](BlackBoxBus[Tup2[I32,I32]]("setupBus"))
      val idxBus = StreamOut[I32](BlackBoxBus[I32]("idxBus"))
      val retBus = StreamIn[Tup2[U32,Bit]](BlackBoxBus[Tup2[U32,Bit]]("retBus"))
      // val setupBus = StreamOut[Tup2[I32,I32]](DRAMBus[Tup2[I32,I32]])
      // val idxBus = StreamOut[I32](DRAMBus[I32])
      // val retBus = StreamIn[Tup2[U32,Bit]](DRAMBus[Tup2[U32,Bit]])

      setupBus := pack(max.unbox, len.unbox)
      Foreach(len.unbox par 16){i =>
        val idx       = indices.__read(Seq(i), Set.empty)
        idxBus := idx
      }
      // Fringe
      val op = Fringe.bvBuild(tree, shift, setupBus, idxBus, retBus)
      // transferSyncMeta(old, store)
      // Receive
      // Foreach(len.unbox by p par 1){i =>
      Stream (*) {
        Foreach(16 by 1 par 16){i =>
          val ret = retBus.value()
          // val bitvec = retBus.value()._1
          // val l = retBus.value()._2
          bv.__write(ret._1, Seq(i), Set.empty)
          last.__write(ret._2, Seq(i), Set.empty)
        }
      }
    }
    // top.loweredTransfer = if (isLoad) SparseLoad else SparseStore // TODO: Work around @virtualize to set this metadata
  }

}
