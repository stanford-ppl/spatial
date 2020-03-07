package spatial.node

import argon._
import forge.tags._
import spatial.lang._
import spatial.metadata.memory._


/** A dense transfer between on-chip and off-chip memory
  * If isLoad is true, this is a transfer from off-chip to on-chip.
  * Otherwise, this is a transfer from on-chip to off-chip.
  * If isAlign
  *
  * @tparam A The type of the elements being loaded/stored
  * @tparam Frame The type of the off-chip memory
  * @tparam Local The type of the on-chip memory
  * @param frame The instance of the off-chip memory
  * @param local The instance of the on-chip memory
  * @param isLoad If true this is a load from off-chip (if true), otherwise this is a store to off-chip
  * @param forceAlign If true, this forces the transfer to be aligned with the DRAM's burst size.
  * @param ens Explicit enable signals for this transfer
  * @param A Type evidence for the element
  * @param Local Type evidence for the on-chip memory
  * @param Frame Type evidence for the off-chip memory
  */
@op case class FrameTransmit[A,Frame[T],Local[T]<:LocalMem[T,Local]](
    frame:       Frame[A],
    local:      Local[A],
    isLoad:     Boolean,
    forceAlign: Boolean = false,
    ens:        Set[Bit] = Set.empty
  )(implicit
    val A:     Bits[A],
    val Local: Type[Local[A]],
    val Frame:  Type[Frame[A]])
  extends EarlyBlackbox[Void] {
    def isStore: Boolean = !isLoad

    override def effects: Effects = if (isStore) Effects.Writes(frame) else Effects.Writes(local)
    @rig def lower(old:Sym[Void]): Void = {
      FrameTransmit.transfer(old, frame,local,forceAlign,ens,isLoad)
    }
}

object FrameTransmit {
  @rig def transfer[A,Frame[T],Local[T]<:LocalMem[T,Local]](
    old:        Sym[Void],
    frame:       Frame[A],
    local:      Local[A],
    forceAlign: Boolean,
    ens:        Set[Bit],
    isLoad:     Boolean
  )(implicit
    A:     Bits[A],
    Local: Type[Local[A]],
    Frame:  Type[Frame[A]]
  ): Void = {
    import spatial.metadata.types._

    val Op(FrameHostNew(len,_,dataStream)) = frame


    if (A.nbits % 8 != 0) throw new Exception(s"Cannot transfer to/from Frame ${A.nbits} bits per cycle, since it cannot be packed into bytes. ${frame} (${frame.name.getOrElse("")}) <-> ${local} (${local.name.getOrElse("")}) ")

    val top = Stream {
      val localAddr =
        if (local.constDims.product > len.head.toInt) {
          warn("On-chip memory for Frame transmit is smaller than Frame!  Will wrap around while transferring (You should probably use a Stream controller")
          i: ICTR => i % local.constDims.map(_.toInt).product
        } else {i: ICTR => i}
      if (isLoad) {

        // Data loading
        Stream.Foreach(len.head.to[ICTR] by 1){i =>
          local.__write(dataStream.asInstanceOf[StreamIn[A]].value(), Seq(localAddr(i)), Set.empty)
        }
      }
      else {
        val (tid, tdest) = dataStream.asInstanceOf[Sym[_]] match { case _@Op(StreamOutNew(bus: AxiStream64Bus)) => (bus.tid, bus.tdest); case _ => (0,0)}
        Stream.Foreach(len.head.to[ICTR] by 1){i =>
          val tuser = mux(i === 0, 2.to[U32], 0.to[U32])
          val data = local.__read(Seq(localAddr(i)), Set.empty)
          val last = i === (len.head-1)
          dataStream.asInstanceOf[StreamOut[AxiStream64]] := AxiStream64(data.as[U64], /*~*/0.to[U8], /*~*/0.to[U8], last, tid.to[U8], tdest.to[U8], tuser)
        }
      }
    }
  }
}
