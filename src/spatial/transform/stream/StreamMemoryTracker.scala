package spatial.transform.stream

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.node._

import scala.collection.mutable

trait StreamMemoryTracker extends MetaPipeToStreamBase {
  this: MutateTransformer =>

  type MemType = LocalMem[_, C forSome {type C[_]}]

  def canTransformMem(s: Sym[_]): Boolean = s match {
    case _: Reg[_] => true
    case _ if s.isSRAM => true
    case _ => false
  }

  def shouldBuffer(s: Sym[_]): Boolean = s match {
    case _: SRAM[_, _] => true
    case _ => false
  }

  def shouldDuplicate(s: Sym[_]): Boolean = s match {
    case _: Reg[_] => true
    case _ => false
  }

  def shouldIgnore(s: Sym[_]): Boolean = s match {
    case _: FIFO[_] => true
    case _ => false
  }

  private def requiredBuffers(s: Sym[_]): Int = {
    s match {
      case _ if s.isSeqControl => 1  // Only 1 stage is active at a time
      case _ if s.isInnerPipeLoop =>
        scala.math.ceil(s.children.length / s.II).toInt
      case _ if s.isOuterPipeControl =>
        s.children.length
      case _ if s.isStreamControl || s.isParallel => 1  // These manage synchronization on the buffer manually.
      case _ =>
        throw new Exception(s"Cannot determine buffer depth for a non-control symbol ${s}")
    }
  }

  class MemoryBufferNotification {

    // src -> dst -> mem -> FIFO
    // (src, dst, mem, fifo) sets
    case class Internal(src: Sym[_], dst: Sym[_], mem: Sym[_], fifo: FIFO[I32])
    val notifiers = mutable.ArrayBuffer[Internal]()

    def register(src: Sym[_], dst: Sym[_], mem: Sym[_], depth: I32, initialTokens: Int = 0) = {
      val fifo = FIFO[I32](depth)
      fifo.explicitName = s"CommFIFO_${src}_${dst}"
      notifiers.append(Internal(src, dst, mem, fifo))
      if (initialTokens > 0) {
        fifo.conflictable

        val enableReg = Reg[Bit](true)
        enableReg.explicitName = s"CommFIFO_Initial_${src}_${dst}"
        enableReg.dontTouch
        ('TokenInitializer.Pipe {
          fifo.enqVec(Vec.fromSeq(Range(0, initialTokens) map {i => I32(i)}), enableReg.value)
          enableReg := false
        }).asSym.shouldConvertToStreamed = false
      }
    }

    // Mem -> FIFO
    def getSendFIFOs(src: Sym[_]): List[(Sym[_], FIFO[I32])] = {
      (notifiers filter {_.src == src} map {x => (x.mem, x.fifo)}).toList
    }

    // Mem -> FIFO
    def getRecvFIFOs(dst:Sym[_]): List[(Sym[_], FIFO[I32])] = {
      (notifiers filter {_.dst == dst} map {x => (x.mem, x.fifo)}).toList
    }
  }

  val duplicationReadFIFOs = mutable.Map[Sym[_], mutable.Map[Sym[_], Sym[_]]]()

  // Writer -> Memory -> FIFOs
  val duplicationWriteFIFOs = mutable.Map[Sym[_], mutable.Map[Sym[_], mutable.ArrayBuffer[Sym[_]]]]()

  var memoryBufferNotifs: MemoryBufferNotification = null

  def computeBufferDepth(mem: Sym[_], writeData: Seq[MemoryWriteData]): (Map[Sym[_], Int], Int) = {
    val accesses = writeData.flatMap {
      case MemoryWriteData(wr, rds) =>
        Seq(wr) ++ rds
    } ++ Seq(writeData.head.writer)
    val result = ((accesses zip accesses.tail) map {
      case (source, destination) =>
        // the buffer depth necessary is from when the destination dequeues to when the src can create a new element.
        // if src cycles < dest II then the result is 3. The source can catch up even if it ends up clearing its
        // pipeline each time.
        val sourceIterations = source.approxIters
        val destIterations = destination.approxIters

        val srcII = source.II * sourceIterations
        val destII = destination.II * destIterations
        val srcCycles = source.II * (sourceIterations - 1) + source.latencySum
        dbgs(s"$source -> $destination ($srcII, $destII, $sourceIterations, $destIterations)")

        // if src cycles > dest II, but src II <= dest II, then the result should be src cycles - src II = srcLatency
        // Can't tolerate clearing pipeline, so we have to buffer by latency.
        // Otherwise, the source can't keep up, so use 3.

        val minBufferDepth = {
          // Also, need to make sure that the destination can terminate while still having elements left in the fifo
          // to prevent deadlock. This means that there need to be enough credits in the system
          // such that the consumer can actually dequeue a token.
          requiredBuffers(destination)
        }

        val requiredBufferDepth = if (srcCycles <= destII) {
          // In this case, we can start the next iteration after the destination dequeues, so it's fine.
          3
        } else if (srcCycles > destII && srcII <= destII) {
          // In this case, the producer can keep up, but needs some extra time.
          scala.math.max(scala.math.ceil((source.latencySum - destII)).toInt, 3)
        } else {
          // In this case, srcCycles >= destII, and srcII > destII, so we can't keep up anyways. Default to 3.
          3
        }
        destination -> scala.math.max(requiredBufferDepth, minBufferDepth)
    }).toMap
    dbgs(s"Computing Buffer Depths: ${result}")
    // For maximum throughput, buffer depth = sum(buffer depths) + 1
    // For minimum throughput, buffer depth = max(buffer depths) + 1
    val minBufferDepth = spatial.util.roundUpToPow2(result.values.max + 1)
    val maxBufferDepth = spatial.util.roundUpToPow2(result.values.sum + 1)
    mem.bufferAmount = StreamBufferAmount(maxBufferDepth, minBufferDepth, maxBufferDepth)

    ((result.keys map {k => (k -> (maxBufferDepth * 2))}).toMap, maxBufferDepth)
  }

  def getMemTokens(stmt: Sym[_], en: Set[Bit], memoryBufferNotifs: MemoryBufferNotification) = {
    val tmpRegs = (memoryBufferNotifs.getRecvFIFOs(stmt) map {
      case(mem, fifo) =>
        val tokenRegBuf = Reg[I32]
        tokenRegBuf.explicitName = s"TokenReg_${mem}_${fifo}_buf"
        ((mem, fifo) -> tokenRegBuf)
    }).toMap

    val tmp = (memoryBufferNotifs.getRecvFIFOs(stmt) map {
      case(mem, fifo) =>
        val token = stage(FIFODeq(fifo, en))
        val tokenReg = Reg[I32]
        val tokenRegBuf = tmpRegs((mem, fifo))

        tokenReg.explicitName = s"TokenReg_${mem}_${fifo}"
        tokenReg.nonbuffer
        tokenReg.write(token, en.toSeq:_*)
        tokenRegBuf := tokenReg

        dbgs(s"Staging token: $mem -> ${f(mem)}, $fifo, $tokenReg, $tokenRegBuf")
        (f(mem) -> tokenRegBuf)
    }).toMap
    tmp map {case (m, reg) => m -> reg.value }
  }

  def registerDuplicationFIFOReads(stmtReads: Traversable[Sym[_]], duplicationReadFIFOs: Map[Sym[_], Sym[_]], en: Set[Bit]): Map[Sym[_], Sym[_]] = {
    val cloned = mutable.Map[Sym[_], Sym[_]]()
    stmtReads foreach {
      case read: Reg[_] =>
        type T = read.RT
        lazy implicit val bT: Bits[T] = read.A
        val tmp = mirrorSym(read)
        cloned(read) = tmp

        // All of the original register reads/writes are now delegated to a proxy register.
        register(read -> tmp)

        tmp.explicitName = read.explicitName.getOrElse(s"InsertedReg_$read")
        if (duplicationReadFIFOs contains read) {
          val deq = stage(FIFODeq(duplicationReadFIFOs(read).asInstanceOf[FIFO[T]], en))
          stage(RegWrite(tmp.unbox, deq, en))
        }
    }
    cloned.toMap
  }

  def initializeMemoryTracker(stms: Seq[Sym[_]], nonLocalUses: Map[Sym[_], List[Sym[_]]]): Unit = {

    // For Duplicated Memories
    // Reader -> Memory -> FIFO
//    val duplicationReadFIFOs = mutable.Map[Sym[_], mutable.Map[Sym[_], Sym[_]]]()

    // Writer -> Memory -> FIFOs
    duplicationWriteFIFOs.clear()
    duplicationReadFIFOs.clear()

    memoryBufferNotifs = new MemoryBufferNotification

    val linearizedUses = computeLinearizedUses(stms)
    dbgs(s"Linearised Uses: ${linearizedUses.dataMap}")

    linearizedUses.dataMap foreach {
      case (mem: Mem[_, _], wrData) if shouldDuplicate(mem) =>
        wrData foreach {
          case MemoryWriteData(wr, rds) =>
            // handle the FIFO-ization logic

            // Memory was previously written, now need a new fifo.
            val writerLatency = wr.children.length
            val latencyEpsilon = 4

            lazy implicit val bits: Bits[mem.L] = mem.A.asInstanceOf[Bits[mem.L]]
            val fifoDepth = I32(writerLatency + latencyEpsilon)

            rds foreach {
              rd =>
                val newFIFO = stage(FIFONew[mem.L](fifoDepth))
                newFIFO.explicitName = s"${mem.explicitName.getOrElse(s"$mem")}_${wr}_$rd"
                duplicationWriteFIFOs.getOrElseUpdate(wr, mutable.Map.empty).getOrElseUpdate(mem, mutable.ArrayBuffer.empty).append(newFIFO.asSym)
                duplicationReadFIFOs.getOrElseUpdate(rd, mutable.Map.empty)(mem) = newFIFO.asSym
            }
        }

      case (mem: Mem[_, _], wrData) if shouldBuffer(mem) =>
        // for each memory, synchronize accesses with FIFOs.
        val (bufferAmounts, duplicates) = mem.bufferAmount match {
          case Some(bam) =>
            val bdepths = Map[Sym[_], Int]().withDefaultValue(2*bam)
            (bdepths, bam)
          case None =>
            dbgs(s"Inferring buffer depths for: $mem")
            computeBufferDepth(mem, wrData)
        }
        dbgs(s"Mem: $mem, buffering: $bufferAmounts, duplications: $duplicates")

        wrData foreach {
          case mwd@MemoryWriteData(writer, readers) =>
            dbgs(s"Memory: $mem, data: $mwd")

            // writer sends a signal to each reader signaling that it's ready
            // Creates a cyclic loop of FIFOs
            (Seq(writer) ++ readers) zip readers foreach {
              case (s, d) =>
                val bufferDepth = bufferAmounts(d)
                memoryBufferNotifs.register(s, d, mem, I32(bufferDepth))
            }
        }
        // Add a backedge from the last reader back to the writer
        val lastReader = wrData.last.readers.last
        val firstWriter = wrData.head.writer

        memoryBufferNotifs.register(lastReader, firstWriter, mem, bufferAmounts(firstWriter), duplicates)

      case (mem: Mem[_, _], _) if shouldIgnore(mem) =>
        dbgs(s"Ignoring $mem")

      case (mem: Mem[_, _], wrData) =>
        dbgs(s"Skipping $mem, since it's neither buffered nor duplicated. Probably a FIFO. $wrData")
    }

    dbgs(s"Duplication FIFOs: $duplicationReadFIFOs -> $duplicationWriteFIFOs")
    nonLocalUses foreach {
      case (mem, users) =>
        users.sliding(2) foreach {
          case a::b::_ =>
            // what should the buffer depth be?
            memoryBufferNotifs.register(a, b, mem, 128)
          case _ =>
            error("Error computing notifications")
        }
        memoryBufferNotifs.register(users.last, users.head, mem, 128, 1)
    }
  }
}
