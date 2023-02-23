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
import spatial.util.spatialConfig

import spatial.metadata.transform._

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
  extends EarlyBlackbox[Void] {
    def isStore: Boolean = !isLoad

    override def effects: Effects = if (isStore) Effects.Writes(dram) else Effects.Writes(local)
    @rig def lower(old:Sym[Void]): Void = {
      DenseTransfer.transfer(old, dram,local,forceAlign,ens,isLoad)
    }
    @rig def pars: Seq[I32] = {
      val normalCounting: Boolean = dram.rawRank.last == dram.sparseRank.last
      (dram.sparsePars().map(_._2) ++ {if (!normalCounting) Seq(I32(1)) else Nil }).toSeq
    }
    @rig def ctrSteps: Seq[I32] = {
      val normalCounting: Boolean = dram.rawRank.last == dram.sparseRank.last
      (dram.sparseSteps().map(_._2) ++ {if (!normalCounting) Seq(I32(0)) else Nil }).toSeq
    }
    @rig def lens: Seq[I32] = {
      val normalCounting: Boolean = dram.rawRank.last == dram.sparseRank.last
      (dram.sparseLens().map(_._2) ++ {if (!normalCounting) Seq(I32(1)) else Nil}).toSeq
    }
}

object DenseTransfer {
  @rig def transfer[A,Dram[T]<:DRAM[T,Dram],Local[T]<:LocalMem[T,Local]](
    old:        Sym[Void],
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
    val normalCounting: Boolean = dram.rawRank.last == dram.sparseRank.last
    val rawDramOffsets: Seq[I32] = dram.rawStarts()
    val rawRank: Int = dram.rawRank.length
    val sparseRank: Seq[Int] = dram.sparseRank ++ {if (!normalCounting) Seq(rawRank) else Nil }
    val lens: Map[Int,I32] = dram.sparseLens() ++ {if (!normalCounting) Seq(rawRank -> I32(1)) else Nil }
    val rawDims: Seq[I32] = dram.rawDims()
    val strides: Map[Int,I32] = dram.sparseSteps()
    val pars: Map[Int,I32] = dram.sparsePars() ++ {if (!normalCounting) Seq(rawRank -> I32(1)) else Nil }
    val counters: Seq[() => Counter[I32]] = sparseRank.map{d => () => Counter[I32](start = 0, end = lens(d), par = pars(d)) }

    val p = pars.toSeq.maxBy(_._1)._2
    val upperPars = pars.dropRight(1).map(_._2 match {case Expect(p) => p.toInt; case _ => 1})
    if (upperPars.exists(_ > 1)) throw new Exception(s"Cannot parallelize non-leading dimension of tile transfer by more than 1 (${dram.name.getOrElse("")}) <-> ${local} (${local.name.getOrElse("")}).  Please rewrite with metaprogramming.  See Spatial issue #238 for details")
    val lastPar = pars.last._2 match {case Expect(p) => p.toInt; case _ => 1}
    val requestLength: I32 = lens.toSeq.maxBy(_._1)._2
    val bytesPerWord = {A.nbits / 8} max 1
    val wordsPackedInByte = {8 / A.nbits} max 1 // Misnomer
    if ((A.nbits * lastPar) % 8 != 0) throw new Exception(s"Cannot transfer to/from DRAM ${A.nbits * lastPar} bits per cycle, since it cannot be packed into bytes. ${dram} (${dram.name.getOrElse("")}) <-> ${local} (${local.name.getOrElse("")}) ")
    p match {case Expect(p) => assert(p.toInt*A.nbits <= target.burstSize, s"Cannot parallelize by more than the burst size! Please shrink par (par ${p.toInt} * ${A.nbits} > ${target.burstSize})"); case _ =>}

    val outerLoopCounters = counters.dropRight(1)
    if (outerLoopCounters.nonEmpty) {
      val top = 'DenseTransfer.Stream.Foreach(outerLoopCounters.map{ctr => ctr()}){ is =>
        val indices = is :+ 0.to[I32]

        // Pad indices, strides with 0's against rawDramOffsets
        val indicesPadded = dram.rawRank.map{i => if (dram.sparseRank.contains(i)) indices(dram.sparseRank.indexOf(i)) else 0.to[I32]}
        val stridesPadded = dram.rawRank.map{i => strides.getOrElse(i, 1.to[I32])}

        val dramAddr = () => flatIndex((rawDramOffsets,indicesPadded,stridesPadded).zipped.map{case (ofs,i,s) => ofs + i*s }, rawDims) / wordsPackedInByte
        val localAddr = if (normalCounting) {i: I32 => is :+ i } else {_: I32 => is}
        if (isLoad) load(dramAddr, localAddr)
        else        store(dramAddr, localAddr)
      }
      top.loweredTransfer = if (isLoad) DenseLoad else DenseStore
      top.asSym.isStreamPrimitive = true
//      val alignedSize = lens.last._2 match {case Expect(c) if (c*A.nbits) % target.burstSize == 0 => lens.last._2; case Expect(c) => lens.last._2.from(c - (c % (target.burstSize / A.nbits)) + target.burstSize / A.nbits); case _ => lens.last._2}
      top.loweredTransferSize = (lens.last._2, pars.last._2, lastPar*A.nbits)
    }
    else {
      val top = 'DenseTransfer.Stream {
        val dramAddr = () => flatIndex(rawDramOffsets, rawDims) / wordsPackedInByte
        val localAddr = {i: I32 => Seq(i) }
        if (isLoad) load(dramAddr, localAddr)
        else        store(dramAddr, localAddr)
      }
      top.loweredTransfer = if (isLoad) DenseLoad else DenseStore
      top.asSym.isStreamPrimitive = true
//      val alignedSize = lens.last._2 match {case Expect(c) if (c*A.nbits) % target.burstSize == 0 => lens.last._2; case Expect(c) => lens.last._2.from(c - (c % (target.burstSize / A.nbits)) + target.burstSize / A.nbits); case _ => lens.last._2}
      top.loweredTransferSize = (lens.last._2, pars.last._2, lastPar*A.nbits)
    }

    // struc.loweredTransfer = if (isLoad) DenseLoad else DenseStore

    def store(dramAddr: () => I32, localAddr: I32 => Seq[I32]): Void = requestLength match {
      case _ if forceAlign =>
        dbg(s"$local => $dram: Using aligned store (forceAlign) requestLength:$requestLength")
        alignedStore(dramAddr, localAddr)
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
      case _ if forceAlign =>
        dbg(s"$local => $dram: Using aligned load (forceAlign) requestLength:$requestLength")
        alignedLoad(dramAddr, localAddr)
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
      val enableQueue = FIFO[Bit](32)

      // Command generator
      'DenseTransferCommands.Sequential {
        enableQueue.deqVec(p match {case Const(c) => c.toInt})
        val addr_bytes = (dramAddr() * bytesPerWord).to[I64] + dram.address
        val size_bytes = requestLength * bytesPerWord / wordsPackedInByte
        cmdStream := (BurstCmd(addr_bytes.to[I64], size_bytes, false), dram.isAlloc)
        // issueQueue.enq(size)
      }

      // Data loading
      Foreach(requestLength par p){i =>
        val data = local.__read(localAddr(i), Set.empty)
        dataStream := pack(data,true)
        retimeGate()
        enableQueue.enq(Bit(true), i < p)
      }

      // Fringe
      val store = Fringe.denseStore[A,Dram](dram, cmdStream, dataStream, ackStream)
      transferSyncMeta(old, store)

      // Ack receiver
      // TODO[2]: Assumes one ack per command
      'DenseTransferAck.Pipe {
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

    def staticStart(dramAddr: () => I32): Either[scala.Int, Sym[_]] = {
      val bytesPerBurst = (target.burstSize/8).to[I32]

      val maddr_bytes  = dramAddr() * bytesPerWord     // Raw address in bytes
      val start_bytes  = maddr_bytes % bytesPerBurst    // Number of bytes offset from previous burst aligned address

      val start = start_bytes / bytesPerWord     // Number of WHOLE elements to ignore at start

      start match {
        case Const(x) => Left(x.toInt)
        case x => Right(x)
      }
    }

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

      if (A.nbits < 8) {
        val elementsPerBurst = (target.burstSize / A.nbits).to[I32]
        val bytesPerBurst = (target.burstSize / 8).to[I32]

        val maddr_bytes = dramAddr() * bytesPerWord // Raw address in bytes
        val start_bytes = maddr_bytes % bytesPerBurst // Number of bytes offset from previous burst aligned address
        val length_bytes = requestLength * bytesPerWord / wordsPackedInByte // Raw length in bytes
        val offset_bytes = maddr_bytes - start_bytes // Burst-aligned start address, in bytes
        val raw_end = maddr_bytes + length_bytes // Raw end, in bytes, with burst-aligned start

        val end_bytes = mux(raw_end % bytesPerBurst === 0.to[I32], 0.to[I32], bytesPerBurst - raw_end % bytesPerBurst) // Extra useless bytes at end

        // FIXME: What to do for bursts which split individual words?
        val start = wordsPackedInByte * start_bytes / bytesPerWord // Number of WHOLE elements to ignore at start
        val extra = wordsPackedInByte * end_bytes / bytesPerWord // Number of WHOLE elements that will be ignored at end
        val end = start + requestLength // Index of WHOLE elements to start ignoring at again
        val size = end + extra // Total number of WHOLE elements to expect

        val size_bytes = length_bytes + start_bytes + end_bytes // Burst aligned length
        val addr_bytes = offset_bytes.to[I64] + dram.address // Burst-aligned offchip byte address
        AlignmentData(start, end, size, addr_bytes, size_bytes)
      } else {
         // In Plasticine dram.address is guaranteed to be burst aligned when host malloc
         // TODO: On plasticine only consider word width dividable by burstSize
         // This logic should be easily extendable to FPGA by converting addresses first to bitPerBurst instead of wordPerBurst. 
         // The key obervation is that once addresses are burst aligned they are byte aligned already. 
         val wordsPerBurst = (target.burstSize/A.nbits).to[I32] 
         val offsetWord = dramAddr()
         val offsetBurstAlignedWord = offsetWord / wordsPerBurst * wordsPerBurst
         val offsetBurstAlignedByte = offsetBurstAlignedWord * bytesPerWord / wordsPackedInByte
         val start = offsetWord - offsetBurstAlignedWord
         val end = start + requestLength
         val endBurstAlignedWord = (end + (wordsPerBurst - 1).to[I32]) / wordsPerBurst * wordsPerBurst
         val endBurstAlignedByte = endBurstAlignedWord * bytesPerWord / wordsPackedInByte
         val size = endBurstAlignedWord

         val size_bytes = endBurstAlignedByte
         val addr_bytes = offsetBurstAlignedByte.to[I64] + dram.address

         AlignmentData(start, end, size, addr_bytes, size_bytes)
      }
    }

    def unalignedStore(dramAddr: () => I32, localAddr: I32 => Seq[I32]): Void = {
      val cmdStream  = StreamOut[BurstCmd](BurstCmdBus)
      cmdStream.isAligned = false
      val dataStream = StreamOut[Tup2[A,Bit]](BurstFullDataBus[A]())
      val ackStream  = StreamIn[Bit](BurstAckBus)

      // Command generator
      Pipe{ // Outer pipe necessary or else acks may come back after extra write commands
        Pipe {
          val startBound: Reg[I32] = Reg[I32]
          val endBound: Reg[I32]   = Reg[I32]
          val length: Reg[I32]     = Reg[I32]
          val size_bytes: Reg[I32] = Reg[I32]
          val addr_bytes: Reg[I64] = Reg[I64]
          val enableQueue = FIFO[Bit](32)
          Pipe {
            val aligned = alignmentCalc(dramAddr)
            startBound := aligned.start
            endBound := aligned.end
            length := aligned.size
            size_bytes := aligned.size_bytes
            addr_bytes := aligned.addr_bytes
          }
          Foreach(length.value par p) { i =>
            val en = staticStart(dramAddr) match {
              case Left(x) => i >= x && i < endBound
              case Right(_) => i >= startBound && i < endBound
            }
            val addr = staticStart(dramAddr) match {
              case Left(x) => localAddr(i - x)
              case Right(_) => localAddr(i - startBound)
            }
            val data = local.__read(addr, Set(en))
            dataStream := pack(data, en)
            retimeGate()
            enableQueue.enq(i < p)
          }
          Pipe {
            enableQueue.deqVec(p match {case Const(c) => c.toInt})
            cmdStream := (BurstCmd(addr_bytes.value, size_bytes.value, false), dram.isAlloc)
          }

        }
        // Fringe
        val store = Fringe.denseStore(dram, cmdStream, dataStream, ackStream)
        transferSyncMeta(old, store)
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
      'AlignedLoadCommand.Sequential {
        val addr = (dramAddr() * bytesPerWord).to[I64] + dram.address
        val addr_bytes = addr
        val size_bytes = requestLength * bytesPerWord / wordsPackedInByte

        cmdStream := (BurstCmd(addr_bytes.to[I64], size_bytes, true), dram.isAlloc)
      }
      // Fringe
      val load = Fringe.denseLoad(dram, cmdStream, dataStream)
      transferSyncMeta(old, load)

      'AlignedLoadWrite.Foreach(requestLength par p){i =>
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
      Sequential {
        val aligned = alignmentCalc(dramAddr)

        cmdStream := (BurstCmd(aligned.addr_bytes.to[I64], aligned.size_bytes, true), dram.isAlloc)
        issueQueue.enq( IssuedCmd(aligned.size, aligned.start, aligned.end) )
      }

      // Fringe
      val load = Fringe.denseLoad(dram, cmdStream, dataStream)
      transferSyncMeta(old, load)

      // Receive
      Pipe {
        // TODO: Should also try Reg[IssuedCmd] here
        val start: Reg[I32] = Reg[I32]
        val end: Reg[I32]   = Reg[I32]
        val size: Reg[I32]  = Reg[I32]
        Pipe {
          val cmd = issueQueue.deq()
          start := cmd.start
          end := cmd.end
          size := cmd.size
        }
        Foreach(size par p){i =>
          val en = staticStart(dramAddr) match {
                  case Left(x)  => i >= x && i < end
                  case Right(_) => i >= start && i < end
                }
          val addr = staticStart(dramAddr) match {
                  case Left(x)  => localAddr(i - x)
                  case Right(_) => localAddr(i - start)
                }
          val data = dataStream.value()
          local.__write(data, addr, Set(en))
        }
      }
    }
  }

}
