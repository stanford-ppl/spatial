package spatial.node

import argon._
import forge.tags._

import spatial.lang._
import spatial.metadata.memory._
import spatial.metadata.control._
import spatial.metadata.bounds._
import spatial.util.memops._
import spatial.util.spatialConfig
import spatial.util.modeling.target

/** A dynamically-computed store from on-chip to off-chip memory.
  *
  * @tparam A The type of elements being loaded/stored
  * @tparam Local The type of the on-chip memory.
  * @param dram A sparse view of the off-chip memory
  * @param data The instance of the on-chip memory
  * @param done Whether the vector/scalar value is the last one
  * @param ens Explicit enable signals for this transfer
  * @param bA Type evidence for the element
  * @param tL Type evidence for the on-chip memory.
  */
@op case class DynamicStore[A,Dram[T]<:DRAM[T,Dram],Local[T]<:LocalMem[T,Local]](
    dram:     Dram[A],
    data:     Local[A],
    done:     Local[Bit],
    params:   Tup2[I32,I32],
    p:        scala.Int,
    ens:      Set[Bit] = Set.empty,
  )(implicit
    val A:     Bits[A],
    val Local: Type[Local[A]],
    val Dram:  Type[Dram[A]])
    // val bA:   Bits[A],
    // val tL:   Type[Local[A]])
  extends EarlyBlackBox[Void] {

  override def effects: Effects = Effects.Writes(dram) 
  @rig def lower(old:Sym[Void]): Void = DynamicStore.transfer(old, dram,data,done,params,p,ens)
  // @rig def pars: Seq[I32] = {
    // Seq(dram.addrs[_32]().sparsePars().values.head)
  // }
  @rig def pars: Seq[I32] = {
    val normalCounting: Boolean = dram.rawRank.last == dram.sparseRank.last
    (dram.sparsePars().map(_._2) ++ {if (!normalCounting) Seq(I32(1)) else Nil }).toSeq
  }
}

object DynamicStore {

  @virtualize
  @rig def transfer[A,Dram[T]<:DRAM[T,Dram],Local[T]<:LocalMem[T,Local]](
    old:     Sym[Void],
    dram:    Dram[A],
    local:   Local[A],
    done:    Local[Bit],
    params:  Tup2[I32,I32],
    p:       scala.Int,
    ens:     Set[Bit],
  )(implicit
    A:     Bits[A],
    Local: Type[Local[A]],
    Dram:  Type[Dram[A]]
    // A:     Bits[A],
    // Local: Type[Local[A]]
  ): Void = {
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
    val bytesPerWord = A.nbits / 8 + (if (A.nbits % 8 != 0) 1 else 0)

    assert(spatialConfig.enablePIR)
    val base = params._1
    val discard = params._2
    // val iters = requestLength

    val top = Stream {
      val localFIFO = local.asInstanceOf[Sym[_]] match {case Op(_:FIFONew[_]) => true; case _ => false}
      val doneFIFO = done.asInstanceOf[Sym[_]] match {case Op(_:FIFONew[_]) => true; case _ => false}

      // Coalesce
      val setupBus = StreamOut[I64](DynStoreCmdBus[A]())
      val cmdBus = StreamOut[Tup2[A,Bit]](DynStoreSetupBus[A]())
      val ackBus = StreamIn[Bit](DynStoreAckBus)

      setupBus := (base.unbox.to[I64]+dram.address, dram.isAlloc)
      // Foreach (128 by 1 par p) { i =>
      Stream(*) {
        Foreach (16 by 1 par 16) { i =>
          // val addr: I64  = if (i == 0) { base.to[I64] + dram.address } else { -1.to[I64] }
          val data       = local.__read(Seq(i), Set.empty)
          val dat_done    = done.__read(Seq(i), Set.empty)
          cmdBus := (pack(data, dat_done), dram.isAlloc)
        }
      }
      // Fringe
      val store = Fringe.dynStore(dram, setupBus, cmdBus, ackBus, p)
      transferSyncMeta(old, store)
      // Receive
      Foreach(1 by 1 par 1){i =>
        val ack = ackBus.value()
      }
    }
    // top.loweredTransfer = if (isLoad) SparseLoad else SparseStore // TODO: Work around @virtualize to set this metadata
  }

}
