package spatial.node

import argon._
import forge.tags._

import spatial.lang._
import spatial.data._
import spatial.util._
import spatial.util.memops._

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
    val p = dram.pars().head
    val requestLength = dram.size

    val bytesPerWord = A.nbits / 8 + (if (A.nbits % 8 != 0) 1 else 0)

    // TODO[2]: Bump up request to nearest multiple of 16 because of fringe
    val iters = Reg[I32](0)
    Pipe{
      iters := mux(requestLength < 16.to[I32], 16.to[I32],
               mux(requestLength % 16.to[I32] === 0.to[I32], requestLength, requestLength + 16.to[I32] - (requestLength % 16.to[I32]) ))
    }

    Stream {
      // Gather
      if (isLoad) {
        val addrBus = StreamOut[I64](GatherAddrBus)
        val dataBus = StreamIn[A](GatherDataBus[A]())

        // Send
        Foreach(iters par p){i =>
          val addr = __ifThenElse(i >= requestLength, dram.address, (addrs.__read(Seq(i),Set.empty) * bytesPerWord).to[I64] + dram.address) //if (i >= requestLength) dram.address.to[I64] else (addrs.__read(Seq(i),Set.empty) * bytesPerWord).to[I64] + dram.address
          val addr_bytes = addr
          addrBus := addr_bytes
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

        // Send
        Foreach(iters par p){i =>
          val pad_addr = max(requestLength - 1, 0.to[I32])
          val curAddr  = if (i >= requestLength) addrs.__read(Seq(pad_addr), Set.empty) else addrs.__read(Seq(i), Set.empty)
          val data     = if (i >= requestLength) local.__read(Seq(pad_addr), Set.empty) else local.__read(Seq(i), Set.empty)

          val addr     = (curAddr * bytesPerWord).to[I64] + dram.address
          val addr_bytes = addr

          cmdBus := pack(data, addr_bytes)
        }
        // Fringe
        Fringe.sparseStore(dram, cmdBus, ackBus)
        // Receive
        // TODO[4]: Assumes one ack per address
        Foreach(iters by target.burstSize/A.nbits){i =>
          val ack = ackBus.value()
        }
      }
    }

  }

}