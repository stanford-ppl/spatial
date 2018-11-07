package spatial.node

import argon._
import forge.tags._

import spatial.lang._
import spatial.metadata.memory._
import spatial.util.memops._
import spatial.util.modeling.target

/** A sparse transfer between on-chip and off-chip memory.
  * If isGather is true, this is a gather from off-chip memory to on-chip.
  * Otherwise, this is a scatter from on-chip memory to off-chip memory.
  *
  * @tparam A The type of elements being loaded/stored
  * @tparam Local The type of the on-chip memory.
  * @param dram A sparse view of the off-chip memory
  * @param local The instance of the on-chip memory
  * @param isGather If true, this is a gather from off-chip. Otherwise, this is a scatter to off-chip.
  * @param ens Explicit enable signals for this transfer
  * @param bA Type evidence for the element
  * @param tL Type evidence for the on-chip memory.
  */
@op case class SparseTransfer[A,Local[T]<:LocalMem[T,Local]](
    dram:     DRAMSparseTile[A],
    local:    Local[A],
    isGather: Boolean,
    ens:      Set[Bit] = Set.empty,
  )(implicit
    val bA:   Bits[A],
    val tL:   Type[Local[A]])
  extends EarlyBlackBox[Void] {
  def isScatter: Boolean = !isGather

  override def effects: Effects = if (isScatter) Effects.Writes(dram) else Effects.Writes(local)
  @rig def lower(): Void = SparseTransfer.transfer(dram,local,ens,isGather)
}

object SparseTransfer {

  @virtualize
  @rig def transfer[A,Local[T]<:LocalMem[T,Local]](
    dram:    DRAMSparseTile[A],
    local:   Local[A],
    ens:     Set[Bit],
    isLoad:  Boolean
  )(implicit
    A:     Bits[A],
    Local: Type[Local[A]]
  ): Void = {
    val addrs = dram.addrs()
    val p = addrs.sparsePars().values.head
    val requestLength = dram.sparseLens().values.head

    val bytesPerWord = A.nbits / 8 + (if (A.nbits % 8 != 0) 1 else 0)

    // TODO[2]: Bump up request to nearest multiple of 16 because of fringe
    val iters: Reg[I32] = Reg[I32]
    Pipe{
      iters := mux(requestLength < 16.to[I32], 16.to[I32],
               mux(requestLength % 16.to[I32] === 0.to[I32], requestLength, requestLength + 16.to[I32] - (requestLength % 16.to[I32]) ))
    }

    Stream {
      // Gather
      if (isLoad) {
        val addrBus = StreamOut[I64](GatherAddrBus)
        val dataBus = StreamIn[A](GatherDataBus[A]())

        // Save complicated streaming control logic by padding FIFO by 1 here, so that the 
        //   controller doesn't see backpressure while the fifo is full but more data is 
        //   waiting to be drained on the input data stream
        if (local.asInstanceOf[Sym[_]] match {case Op(_:FIFONew[_]) => true; case _ => false}) local.padding = Seq(1)

        // If we are reading addrs from FIFO, make sure that FIFO has enough elements to fill the sparse
        //   command or else the controller will stall
        if (addrs.asInstanceOf[Sym[_]] match {case Op(_:FIFONew[_]) => true; case _ => false}) {
          Foreach(requestLength until iters){i => addrs.__write(0.to[I32],Seq(i),Set())}
          // Send
          Foreach(iters par p){i =>
            val addr: I64 = (addrs.__read(Seq(i),Set()) * bytesPerWord).to[I64] + dram.address
            val addr_bytes = addr
            addrBus := (addr_bytes, dram.isAlloc)
          }
        }
        else {
          Foreach(iters par p){i =>
            val cond = i >= requestLength
            val addr: I64 = mux(cond, dram.address, (addrs.__read(Seq(i),Set(!cond)) * bytesPerWord).to[I64] + dram.address)
            val addr_bytes = addr
            addrBus := (addr_bytes, dram.isAlloc)
          }
        }
        // Fringe
        Fringe.sparseLoad(dram, addrBus, dataBus)
        // Receive
        Foreach(iters par p){i =>
          val data = dataBus.value()
          local.__write(data, Seq(i), Set(i < requestLength))
        }
      }
      // Scatter
      else {
        val cmdBus = StreamOut[Tup2[A,I64]](ScatterCmdBus[A]())
        val ackBus = StreamIn[Bit](ScatterAckBus)

        // If we are reading addrs from FIFO, make sure that FIFO has enough elements to fill the sparse
        //   command or else the controller will stall
        if (addrs.asInstanceOf[Sym[_]] match {case Op(_:FIFONew[_]) => true; case _ => false}) {
          Foreach(requestLength until iters){i => addrs.__write(0.to[I32],Seq(i),Set())}
          // Send
          Foreach(iters par p){i =>
            val curAddr  = addrs.__read(Seq(i), Set())
            val data     = local.__read(Seq(i), Set())
            val addr     = (curAddr * bytesPerWord).to[I64] + dram.address
            val addr_bytes = addr

            cmdBus := (pack(data, addr_bytes), dram.isAlloc)
          }
        }
        else {
          // Send
          Foreach(iters par p){i =>
            val pad_addr = max(requestLength - 1, 0.to[I32])
            val cond     = i >= requestLength
            val curAddr  = mux(cond, addrs.__read(Seq(pad_addr), Set(cond)), addrs.__read(Seq(i), Set(!cond)))
            val data     = mux(cond, local.__read(Seq(pad_addr), Set(cond)), local.__read(Seq(i), Set(!cond)))
            val addr     = (curAddr * bytesPerWord).to[I64] + dram.address
            val addr_bytes = addr

            cmdBus := (pack(data, addr_bytes), dram.isAlloc)
          }
        }
        // Fringe
        Fringe.sparseStore(dram, cmdBus, ackBus)
        // Receive
        // TODO[4]: Assumes one ack per address
        Foreach(iters by 1){i =>
          val ack = ackBus.value()
        }
      }
    }

  }

}
