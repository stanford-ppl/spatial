package spatial.transform.stream

import argon.transform.MutateTransformer
import argon._
import spatial.lang._
import spatial.node._
import spatial.traversal.AccelTraversal
import spatial.metadata.memory._
import spatial.metadata.control._
import spatial.metadata.types._
import spatial.metadata.access._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/** Converts Metapipelined controllers into streamed controllers.
  */
case class MetapipeToStreamTransformer(IR: State) extends MutateTransformer with AccelTraversal with MetaPipeToStreamBase {

  private val allowableSchedules = Set[CtrlSchedule](Pipelined, Sequenced)

  private def canTransform[A: Type](lhs: Sym[A], rhs: Op[A]): Boolean = {
    // No point in converting inner controllers
    if (lhs.isInnerControl) {
      return false
    }

    return lhs.shouldConvertToStreamed
  }

  type MemType = LocalMem[_, C forSome {type C[_]}]

  private def canTransformMem(s: Sym[_]): Boolean = s match {
    case r: Reg[_] => true
    case _ if s.isSRAM => true
    case _ => false
  }

  private def shouldBuffer(s: Sym[_]): Boolean = s match {
    case _: SRAM[_, _] => true
    case _ => false
  }

  private def shouldDuplicate(s: Sym[_]): Boolean = s match {
    case _: Reg[_] => true
    case _ => false
  }

  private def isUnconditionalWrite(s: Sym[_]): Boolean = s match {
    case Writer((_, _, _, ens)) => ens forall {
      case Const(c) => c.value
      case _ => false
    }
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
//    val notifiers = mutable.Map[Sym[_], mutable.Map[Sym[_], mutable.Map[Sym[_], FIFO[I32]]]]()
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
        'TokenInitializer.Pipe {
          fifo.enqVec(Vec.fromSeq(Range(0, initialTokens) map {i => I32(i)}), enableReg.value)
          enableReg := false
        }
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

  /**
    * For a sequence of statments, compute the following:
    * For each memory, for each writer, compute the set of readers which read its value.
    * This is slightly different from reaching definitions as each write KILLS the previous writes even if it is
    * conditional. Additionally, this only considers one level of the hierarchy.
    *
    * @param stmts
    */

  private def transformForeach[A: Type](lhs: Sym[A], foreach: OpForeach): Sym[Void] = {

    val parentPars = foreach.cchain.counters map { ctr => ctr.ctrParOr1 }
    val parentShifts = spatial.util.computeShifts(parentPars)
    // Transforms the foreach into a streampipe of foreaches
    dbgs(s"Transforming Foreach: $lhs = $foreach")
    val replacement = stageWithFlow(UnitPipe(
      foreach.ens, stageBlock {
        // for each parent shift, we restage the entire thing.

        val internalMems = foreach.block.internalMems.toSet

        val internalRegs = internalMems filter {
          _.isReg
        }

        def getReadRegs(s: Sym[_]) = {
          (s.readMems union s.writtenMems) intersect internalRegs
        }

        def getWrittenRegs(s: Sym[_]) = {
          s.readMems intersect internalRegs
        }

        // for each block which reads this mem, convert it into a FIFO.
        dbgs(s"InternalMems: ${internalMems.mkString(", ")}")

        parentShifts foreach {
          pShift =>

            val parentShift = (foreach.cchain.counters zip pShift) map {
              case (ctr, shift) =>
                implicit def NumEV: Num[ctr.CT] = ctr.CTeV.asInstanceOf[Num[ctr.CT]]
                implicit def cast: Cast[ctr.CT, I32] = argon.lang.implicits.numericCast[ctr.CT, I32]
                ctr.step.asInstanceOf[ctr.CT].to[I32] * I32(shift)
            }

            // For Duplicated Memories
            // Reader -> Memory -> FIFO
            val duplicationReadFIFOs = mutable.Map[Sym[_], mutable.Map[Sym[_], Sym[_]]]()

            // Writer -> Memory -> FIFOs
            val duplicationWriteFIFOs = mutable.Map[Sym[_], mutable.Map[Sym[_], mutable.ArrayBuffer[Sym[_]]]]()

            val memoryBufferNotifs = new MemoryBufferNotification

            val linearizedUses = computeLinearizedUses(foreach.block.stms)
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
            }

            dbgs(s"Duplication FIFOs: $duplicationReadFIFOs -> $duplicationWriteFIFOs")

            val nonLocalUses = computeNonlocalUses(lhs.readMems, lhs.writtenMems, foreach.block.stms)
            dbgs(s"NonLocal Uses: $nonLocalUses")
            nonLocalUses foreach {
              case (mem, users) =>
                users.sliding(2) foreach {
                  case a::b::_ =>
                    // what should the buffer depth be?
                    memoryBufferNotifs.register(a, b, mem, 128)
                }
                memoryBufferNotifs.register(users.last, users.head, mem, 128, 1)
            }

            foreach.block.stms foreach {
              case stmt if stmt.isCounter || stmt.isCounterChain =>
                subst += (stmt -> Invalid)
                dbgs(s"Eliding counter operations: ${stmt}")

              case s if canTransformMem(s) && shouldDuplicate(s) =>
                dbgs(s"Skipping re-staging $s since it can be transformed")
                subst += (s -> Invalid)

              case stmt if stmt.isControl =>
                val stmtReads = getReadRegs(stmt)
                val stmtWrites = getWrittenRegs(stmt)
                dbgs(s"Stmt: $stmt, effects: ${stmt.effects} reads: $stmtReads, writes: $stmtWrites")
                // for each stmtRead, we need to create a register of the same type inside.

                // map from sym to mems
                val cloned = scala.collection.mutable.Map[Sym[_], Sym[_]]()

                // If the register is read, then it is either written by a previous controller, or we use the
                // default value.

                // If the register is written, then we create a local copy, which we then enqueue at the end
                // of the controller.
                // Since writes can be conditional, we must read the previous value before muxing between the
                // two on enqueue.

                isolateSubst() {
                  stmt.op match {
                    case Some(OpForeach(ens, cchain, block, iters, stopWhen)) =>
                      // Unroll early here.
                      // start, stop, step, par ->
                      // start, stop, step * par, 1

                      val newParentCtrs = (foreach.cchain.counters zip parentShift) map {
                        case (ctr, pshift) =>
                          ctr match {
                            case Op(CounterNew(start, stop, step, par)) =>
                              // we're handling the parent par at a high level.
                              stage(CounterNew(
                                start.asInstanceOf[I32],
                                stop.asInstanceOf[I32] - pshift,
                                step.asInstanceOf[I32] * par, I32(1)
                              ))

                            case Op(ForeverNew()) =>
                              // Don't need to shift, since ForeverNews are parallelized by 1.
                              stage(ForeverNew())
                          }
                      }
                      val shape = ArrayBuffer[Int]()

                      val newChildCtrs = cchain.counters map {
                        case Op(CounterNew(start, stop, step, par)) =>
                          shape.append(par.toInt)
                          stage(CounterNew[I32](start.asInstanceOf[I32], stop.asInstanceOf[I32], step.asInstanceOf[I32] * par, I32(1)))
                        case Op(ForeverNew()) =>
                          shape.append(1)
                          stage(ForeverNew())
                      }

                      val newctrs = newParentCtrs ++ newChildCtrs
                      val ccnew = stage(CounterChainNew(newctrs))

                      val alliters = foreach.iters ++ iters

                      val newiters = alliters.zip(newctrs).map { case (i, ctr) =>
                        val n = boundVar[I32]
                        n.name = i.name
                        n.counter = IndexCounterInfo(ctr, Seq.tabulate(ctr.ctrParOr1) { i => i })
                        n
                      }

                      var memTokens: Map[Sym[_], I32] = null
                      stage(OpForeach(ens, ccnew, stageBlock {
                        val isFirstIter = newiters.takeRight(iters.size)
                        val en = isFirstIter.map {
                          i => i === i.counter.ctr.start
                        }.toSet

                        // Hook up notifications
                        dbgs(s"Setting up notifications for $stmt")
                        dbgs(s"Recv FIFOs: ${memoryBufferNotifs.getRecvFIFOs(stmt)}")
                        dbgs(s"Send FIFOs: ${memoryBufferNotifs.getSendFIFOs(stmt)}")

                        val tmpRegs = (memoryBufferNotifs.getRecvFIFOs(stmt) map {
                          case(mem, fifo) =>
                            val tokenRegBuf = Reg[I32]
                            tokenRegBuf.explicitName = s"TokenReg_${mem}_${fifo}_buf"
                            ((mem, fifo) -> tokenRegBuf)
                        }).toMap

                        memTokens = (memoryBufferNotifs.getRecvFIFOs(stmt) map {
                          case(mem, fifo) =>
                            val token = stage(FIFODeq(fifo, en))
                            val tokenReg = Reg[I32]
                            val tokenRegBuf = tmpRegs((mem, fifo))

                            tokenReg.explicitName = s"TokenReg_${mem}_${fifo}"
                            tokenReg.nonbuffer
                            tokenReg.write(token, en.toSeq:_*)
                            tokenRegBuf := tokenReg

                            dbgs(s"Staging token: $mem, $fifo, $tokenReg, $tokenRegBuf")
                            (f(mem) -> tokenRegBuf.value)
                        }).toMap

                        dbgs(s"MemTokens: ${memTokens}")

                        stmtReads foreach {
                          case read: Reg[_] =>
                            type T = read.RT
                            lazy implicit val bT: Bits[T] = read.A
                            val tmp = mirrorSym(read)
                            cloned(read) = tmp

                            // All of the original register reads/writes are now delegated to a proxy register.
                            register(read -> tmp)

                            tmp.explicitName = read.explicitName.getOrElse(s"InsertedReg_$read")
                            if (duplicationReadFIFOs.getOrElse(stmt, mutable.Map.empty) contains read) {
                              val deq = stage(FIFODeq(duplicationReadFIFOs(stmt)(read).asInstanceOf[FIFO[T]], en))
                              stage(RegWrite(tmp.unbox, deq, en))
                            }
                        }

                        dbgs(s"Blk: ${block.nestedStms}")
                        block.nestedStms foreach {
                          stmt =>
                            val mems = stmt.readMem ++ stmt.writtenMem
                            mems foreach {
                              mem =>
                                memTokens.get(f(mem)) match {
                                  case Some(ind) =>
                                    dbgs(s"Adding Buffering Info to: $stmt")
                                    stmt.bufferIndex = ind
                                  case None =>
                                }
                            }
                        }

                        val childShifts = spatial.util.computeShifts(shape)
                        dbgs(s"Unrolling with shifts: $childShifts")

                        childShifts foreach {
                          cShift =>
                            isolateSubst() {
                              val childShift = (cchain.counters zip cShift) map {
                                case (ctr, shift) =>
                                  implicit def numEV: Num[ctr.CT] = ctr.CTeV.asInstanceOf[Num[ctr.CT]]
                                  implicit def castEV: Cast[ctr.CT, I32] = argon.lang.implicits.numericCast[ctr.CT, I32]
                                  ctr.step.asInstanceOf[ctr.CT].to[I32] * I32(shift)
                              }

                              val shift = parentShift ++ childShift
                              dbgs(s"Processing shift: $shift")
                              (alliters zip newiters) zip shift foreach {
                                case ((oldIter, newIter), s) =>
                                  val shifted = newIter + s
                                  subst += (oldIter -> shifted)
                              }

                              block.stms foreach {
                                x =>
                                  val copy = mirrorSym(x)
                                  subst += (x -> copy)
                              }
                            }
                        }

                        stmtWrites foreach {
                          case wr: Reg[_] =>
                            // Write the write register to the FIFO.
                            implicit lazy val ev: Bits[wr.RT] = wr.A.asInstanceOf[Bits[wr.RT]]
                            val read = cloned(wr).asInstanceOf[Reg[wr.RT]].value
                            duplicationWriteFIFOs(stmt)(wr.asSym) foreach {
                              s =>
                                val of = s.asInstanceOf[FIFO[wr.RT]]
                                stage(FIFOEnq(of, read.asInstanceOf[Bits[wr.RT]], en))
                            }
                        }

                        val isLastEn = isFirstIter.map {
                          i =>
                            (i.unbox + i.counter.ctr.step.asInstanceOf[I32]) >= i.counter.ctr.end.asInstanceOf[I32]
                        }.toSet
                        memoryBufferNotifs.getSendFIFOs(stmt) foreach {
                          case(mem, fifo) =>
                            dbgs(s"Sending fifo: $mem, $fifo")
                            stage(FIFOEnq(fifo, memTokens(f(mem)), isLastEn))
                        }
                      }, newiters, stopWhen))
                  }
                }
              case stmt if shouldDuplicate(stmt) || shouldBuffer(stmt) =>
                dbgs(s"Re-staging memory: $stmt")
                subst += (stmt -> mirrorSym(stmt))
              case stmt if !stmt.isControl =>
                dbgs(s"Didn't know how to convert: $stmt of type ${stmt.op}")
                subst += (stmt -> mirrorSym(stmt))
              case stmt if stmt.isControl =>
                bug(s"Could not convert controller: $stmt of type ${stmt.op}")
                throw new Exception()
            }
        }
      }, foreach.stopWhen
    )) {
      lhs2 =>
        lhs2.rawSchedule = Streaming
        lhs2.userSchedule = Streaming
        lhs2.explicitName = lhs.explicitName.getOrElse(s"MetapipeToStream_${lhs}")
    }.asSym

    replacement
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    (rhs match {
      case AccelScope(_) => inAccel {
        super.transform(lhs, rhs)
      }

      case foreach@OpForeach(ens, cchain, block, iters, stopWhen) if inHw && canTransform(lhs, rhs) =>
        indent {
          transformForeach(lhs, foreach)
        }

      case _ => super.transform(lhs, rhs)
    }).asInstanceOf[Sym[A]]
  }
}
