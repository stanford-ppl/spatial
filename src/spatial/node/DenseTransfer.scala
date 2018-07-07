package spatial.node

import argon._
import forge.tags._

import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.bounds._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.util.modeling.target
import spatial.util.memops._

/** A dense transfer between on-chip and off-chip memory
  * If isLoad is true, this is a transfer from off-chip to on-chip.
  * Otherwise, this is a transfer from on-chip to off-chip.
  * If isAlign
  *
  * @tparam A The type of the elements being loaded/stored
  * @tparam Dram The type of the off-chip memory
  * @tparam Local The type of the on-chip memory
  * @param dram The instance of the off-chip memory
  * @param local The instance of the on-chip memory
  * @param isLoad If true this is a load from off-chip (if true), otherwise this is a store to off-chip
  * @param forceAlign If true, this forces the transfer to be aligned with the DRAM's burst size.
  * @param ens Explicit enable signals for this transfer
  * @param A Type evidence for the element
  * @param Local Type evidence for the on-chip memory
  * @param Dram Type evidence for the off-chip memory
  */
@op case class DenseTransfer[A,Dram[T]<:DRAM[T,Dram],Local[T]<:LocalMem[T,Local]](
    dram:       Dram[A],
    local:      Local[A],
    isLoad:     Boolean,
    forceAlign: Boolean = false,
    ens:        Set[Bit] = Set.empty
  )(implicit
    val A:     Bits[A],
    val Local: Type[Local[A]],
    val Dram:  Type[Dram[A]])
  extends EarlyBlackBox[Void] {
    def isStore: Boolean = !isLoad

    override def effects: Effects = if (isStore) Effects.Writes(dram) else Effects.Writes(local)
    @rig def lower(): Void = {
      DenseTransfer.transfer(dram,local,forceAlign,ens,isLoad)
    }
}

object DenseTransfer {
  @rig def transfer[A,Dram[T]<:DRAM[T,Dram],Local[T]<:LocalMem[T,Local]](
    dram:       Dram[A],
    local:      Local[A],
    forceAlign: Boolean,
    ens:        Set[Bit],
    isLoad:     Boolean
  )(implicit
    A:     Bits[A],
    Local: Type[Local[A]],
    Dram:  Type[Dram[A]]
  ): Void = {

    // Special case if dram is a DenseAlias with the last dimension slashed
    val normalCounting: Boolean = dram.rawRank.last == dram.seqRank.last
    val dramOffsets: Seq[I32] = dram.starts()
    val rawDramOffsets: Seq[I32] = dram.rawStarts()
    val lens: Seq[I32] = dram.lens() ++ {if (!normalCounting) Seq[I32](1) else Seq[I32]()}
    val rawDims: Seq[I32] = dram.rawDims()
    val strides: Seq[I32] = dram.steps()
    val pars: Seq[I32] = dram.pars() ++ {if (!normalCounting) Seq[I32](1) else Seq[I32]()}
    val counters: Seq[() => Counter[I32]] = lens.zip(pars).map{case (d,p) => () => Counter[I32](start = 0, end = d, par = p) }

    val p = pars.last
    val requestLength: I32 = lens.last
    val bytesPerWord = A.nbits / 8 + (if (A.nbits % 8 != 0) 1 else 0)

    val outerLoopCounters = counters.dropRight(1)
    if (outerLoopCounters.nonEmpty) {
      Stream.Foreach(outerLoopCounters.map{ctr => ctr()}){ is =>
        val indices = is :+ 0.to[I32]

        // Pad indices, strides with 0's against rawDramOffsets
        val indicesPadded = dram.rawRank.map{i => if (dram.seqRank.contains(i)) indices(dram.seqRank.indexOf(i)) else 0.to[I32]}
        val stridesPadded = dram.rawRank.map{i => if (dram.seqRank.contains(i)) strides(dram.seqRank.indexOf(i)) else 1.to[I32]}

        val dramAddr = () => flatIndex((rawDramOffsets,indicesPadded,stridesPadded).zipped.map{case (ofs,i,s) => ofs + i*s }, rawDims)
        val localAddr = if (normalCounting) {i: I32 => is :+ i } else {_: I32 => is}
        if (isLoad) load(dramAddr, localAddr)
        else        store(dramAddr, localAddr)
      }
    }
    else {
      Stream {
        val dramAddr = () => flatIndex(rawDramOffsets, rawDims)
        val localAddr = {i: I32 => Seq(i) }
        if (isLoad) load(dramAddr, localAddr)
        else        store(dramAddr, localAddr)
      }
    }

    def store(dramAddr: () => I32, localAddr: I32 => Seq[I32]): Void = requestLength match {
      case Expect(c) if (c*A.nbits) % target.burstSize == 0 | forceAlign =>
        dbg(s"$local => $dram: Using aligned store ($c * ${A.nbits} % ${target.burstSize} = ${c*A.nbits % target.burstSize})")
        alignedStore(dramAddr, localAddr)
      case Expect(c) =>
        dbg(s"$local => $dram: Using unaligned store ($c * ${A.nbits} % ${target.burstSize} = ${c*A.nbits % target.burstSize})")
        unalignedStore(dramAddr, localAddr)
      case _ =>
        dbg(s"$local => $dram: Using unaligned store (request length is statically unknown ($requestLength))")
        unalignedStore(dramAddr, localAddr)
    }
    def load(dramAddr: () => I32, localAddr: I32 => Seq[I32]): Void = requestLength match {
      case Expect(c) if (c.toInt*A.nbits) % target.burstSize == 0 | forceAlign =>
        dbg(s"$dram => $local: Using aligned load ($c * ${A.nbits} % ${target.burstSize} = ${c*A.nbits % target.burstSize})")
        alignedLoad(dramAddr, localAddr)
      case Expect(c) =>
        dbg(s"$dram => $local: Using unaligned load ($c * ${A.nbits} % ${target.burstSize} = ${c*A.nbits % target.burstSize})")
        unalignedLoad(dramAddr, localAddr)
      case _ =>
        dbg(s"$dram => $local: Using unaligned load (request length is statically unknown ($requestLength))")
        unalignedLoad(dramAddr, localAddr)
    }

    def alignedStore(dramAddr: () => I32, localAddr: I32 => Seq[I32]): Void = {
      val cmdStream  = StreamOut[BurstCmd](BurstCmdBus)
      //cmdStream.isAligned = true
      // val issueQueue = FIFO[I32](16)  // TODO: Size of issued queue?
      val dataStream = StreamOut[Tup2[A,Bit]](BurstFullDataBus[A]())
      val ackStream  = StreamIn[Bit](BurstAckBus)

      // Command generator
      Pipe {
        val addr_bytes = (dramAddr() * bytesPerWord).to[I64] + dram.address
        val size = requestLength
        val size_bytes = size * bytesPerWord
        cmdStream := BurstCmd(addr_bytes.to[I64], size_bytes, false)
        // issueQueue.enq(size)
      }

      // Data loading
      Foreach(requestLength par p){i =>
        val data = local.__read(localAddr(i), Set.empty)
        dataStream := pack(data,true)
      }

      // Fringe
      Fringe.denseStore[A,Dram](dram, cmdStream, dataStream, ackStream)

      // Ack receiver
      // TODO[2]: Assumes one ack per command
      Pipe {
        // val size = Reg[I32]
        // Pipe{size := issueQueue.deq()}
        val ack  = ackStream.value()
        ()
        // Foreach(requestLength by target.burstSize/bits[T].length) {i =>
        //   val ack  = ackStream.value()
        // }
      }
    }

    case class AlignmentData(start: I32, end: I32, size: I32, addr_bytes: I64, size_bytes: I32)

    def alignmentCalc(dramAddr: () => I32) = {
      /*
              ←--------------------------- size ----------------→
                               ←  (one burst) →
                     _______________________________
              |-----:_________|________________|____:------------|
              0
          start ----⬏
            end -----------------------------------⬏
                                                   extra --------⬏

      */
      val elementsPerBurst = (target.burstSize/A.nbits).to[I32]
      val bytesPerBurst = (target.burstSize/8).to[I32]

      val maddr_bytes  = dramAddr() * bytesPerWord     // Raw address in bytes
      val start_bytes  = maddr_bytes % bytesPerBurst    // Number of bytes offset from previous burst aligned address
      val length_bytes = requestLength * bytesPerWord   // Raw length in bytes
      val offset_bytes = maddr_bytes - start_bytes      // Burst-aligned start address, in bytes
      val raw_end      = maddr_bytes + length_bytes     // Raw end, in bytes, with burst-aligned start

      val end_bytes = mux(raw_end % bytesPerBurst === 0.to[I32],  0.to[I32], bytesPerBurst - raw_end % bytesPerBurst) // Extra useless bytes at end

      // FIXME: What to do for bursts which split individual words?
      val start = start_bytes / bytesPerWord     // Number of WHOLE elements to ignore at start
      val extra = end_bytes / bytesPerWord       // Number of WHOLE elements that will be ignored at end
      val end   = start + requestLength          // Index of WHOLE elements to start ignoring at again
      val size  = requestLength + start + extra  // Total number of WHOLE elements to expect

      val size_bytes = length_bytes + start_bytes + end_bytes  // Burst aligned length
      val addr_bytes = offset_bytes.to[I64] + dram.address     // Burst-aligned offchip byte address

      AlignmentData(start, end, size, addr_bytes, size_bytes)
    }

    def unalignedStore(dramAddr: () => I32, localAddr: I32 => Seq[I32]): Void = {
      val cmdStream  = StreamOut[BurstCmd](BurstCmdBus)
      cmdStream.isAligned = false
      val dataStream = StreamOut[Tup2[A,Bit]](BurstFullDataBus[A]())
      val ackStream  = StreamIn[Bit](BurstAckBus)

      // Command generator
      Pipe{ // Outer pipe necessary or else acks may come back after extra write commands
        Pipe {
          val startBound = Reg[I32]
          val endBound   = Reg[I32]
          val length     = Reg[I32]
          Pipe {
            val aligned = alignmentCalc(dramAddr)

            cmdStream := BurstCmd(aligned.addr_bytes.to[I64], aligned.size_bytes, false)
            //          issueQueue.enq(aligned.size)
            startBound := aligned.start
            endBound := aligned.end
            length := aligned.size
          }
          Foreach(length.value par p){i =>
            val en = i >= startBound && i < endBound
            val data = local.__read(localAddr(i - startBound), Set(en))
            dataStream := pack(data,en)
          }
        }
        // Fringe
        Fringe.denseStore(dram, cmdStream, dataStream, ackStream)
        // Ack receive
        // TODO[4]: Assumes one ack per command
        Pipe {
          //        val size = Reg[I32]
          //        Pipe{size := issueQueue.deq()}
          val ack  = ackStream.value()
          ()
          //        Foreach(size.value by size.value) {i => // TODO: Can we use by instead of par?
          //          val ack  = ackStream.value()
          //        }
        }
      }
    }

    def alignedLoad(dramAddr: () => I32, localAddr: I32 => Seq[I32]): Void = {
      val cmdStream  = StreamOut[BurstCmd](BurstCmdBus)
      cmdStream.isAligned = true
      val dataStream = StreamIn[A](BurstDataBus[A]())

      // Command generator
      Pipe {
        val addr = (dramAddr() * bytesPerWord).to[I64] + dram.address
        val size = requestLength

        val addr_bytes = addr
        val size_bytes = size * bytesPerWord

        cmdStream := BurstCmd(addr_bytes.to[I64], size_bytes, true)
      }
      // Fringe
      Fringe.denseLoad(dram, cmdStream, dataStream)
      Foreach(requestLength par p){i =>
        val data = dataStream.value()
        val addr = localAddr(i)
        local.__write(data, addr, Set.empty)
      }
    }

    def unalignedLoad(dramAddr: () => I32, localAddr: I32 => Seq[I32]): Void = {
      val cmdStream  = StreamOut[BurstCmd](BurstCmdBus)
      cmdStream.isAligned = false
      val issueQueue = FIFO[IssuedCmd](16)  // TODO: Size of issued queue?
      val dataStream = StreamIn[A](BurstDataBus[A]())

      // Command
      Pipe {
        val aligned = alignmentCalc(dramAddr)

        cmdStream := BurstCmd(aligned.addr_bytes.to[I64], aligned.size_bytes, true)
        issueQueue.enq( IssuedCmd(aligned.size, aligned.start, aligned.end) )
      }

      // Fringe
      Fringe.denseLoad(dram, cmdStream, dataStream)

      // Receive
      Pipe {
        // TODO: Should also try Reg[IssuedCmd] here
        val start = Reg[I32]
        val end   = Reg[I32]
        val size  = Reg[I32]
        Pipe {
          val cmd = issueQueue.deq()
          start := cmd.start
          end := cmd.end
          size := cmd.size
        }
        Foreach(size par p){i =>
          val en = i >= start && i < end
          val addr = localAddr(i - start)
          val data = dataStream.value()
          local.__write(data, addr, Set(en))
        }
      }
    }
  }
}
