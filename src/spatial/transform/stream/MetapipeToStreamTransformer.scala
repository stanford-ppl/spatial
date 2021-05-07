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

    // Can't currently handle the semantics of Stream control, ends up being too strict for Parallel
    if (!(allowableSchedules contains lhs.schedule)) {
      return false
    }

    rhs match {
      case foreach: OpForeach =>
        foreach.block.stms.forall {
          sym =>
            if (!sym.isMem) {
              true
            } else {
              canTransformMem(sym)
            }
        }
      case _ => false
    }
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

  class MemoryBufferNotification {

    // src -> dst -> mem -> FIFO
    // (src, dst, mem, fifo) sets
//    val notifiers = mutable.Map[Sym[_], mutable.Map[Sym[_], mutable.Map[Sym[_], FIFO[I32]]]]()
    case class Internal(src: Sym[_], dst: Sym[_], mem: Sym[_], fifo: FIFO[I32])
    val notifiers = mutable.ArrayBuffer[Internal]()

    def register(src: Sym[_], dst: Sym[_], mem: Sym[_], depth: I32, initialTokens: Int = 0) = {
      val fifo = FIFO[I32](depth)
      notifiers.append(Internal(src, dst, mem, fifo))
      if (initialTokens > 0) {
        fifo.conflictable
        Pipe {
          fifo.enqVec(Vec.fromSeq(Range(0, initialTokens) map {i => I32(i)}))
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
    val parentShifts = computeShifts(parentPars)
    // Transforms the foreach into a streampipe of foreaches
    val replacement = stageWithFlow(UnitPipe(
      foreach.ens, stageBlock {
        // for each parent shift, we restage the entire thing.

        val internalMems = (foreach.block.stms filter canTransformMem).toSet

        val internalRegs = internalMems filter {
          _.isReg
        }

        def getReadRegs(s: Sym[_]) = {
          (s.readMems union s.writtenMems) intersect internalRegs
        }

        def getWrittenRegs(s: Sym[_]) = {
          s.writtenMems intersect internalRegs
        }

        // for each block which reads this mem, convert it into a FIFO.
        dbgs(s"InternalMems: ${internalMems.mkString(", ")}")

        parentShifts foreach {
          parentShift =>


            // For Duplicated Memories
            // Reader -> Memory -> FIFO
            val duplicationReadFIFOs = mutable.Map[Sym[_], mutable.Map[Sym[_], Sym[_]]]()

            // Writer -> Memory -> FIFOs
            val duplicationWriteFIFOs = mutable.Map[Sym[_], mutable.Map[Sym[_], mutable.ArrayBuffer[Sym[_]]]]()

            val memoryBufferNotifs = new MemoryBufferNotification

            val linearizedUses = computeLinearizedUses(foreach.block.stms)
            dbgs(s"Linearised Uses: ${linearizedUses}")

            linearizedUses.dataMap foreach {
              case (mem: Mem[_, _], wrData) if shouldDuplicate(mem) =>
                wrData foreach {
                  case MemoryWriteData(wr, rds) =>
                    // handle the FIFO-ization logic

                    // Memory was previously written, now need a new fifo.
                    val writerLatency = math.ceil(wr.latencySum).toInt
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
                wrData foreach {
                  case mwd@MemoryWriteData(writer, readers) =>
                  // TODO: Calculate bufferdepth

                    dbgs(s"Memory: $mem, data: $mwd")

                    // writer sends a signal to each reader signaling that it's ready
                    val bufferDepth = 10
                    // Creates a cyclic loop of FIFOs
                    (Seq(writer) ++ readers) zip readers foreach {
                      case (s, d) => memoryBufferNotifs.register(s, d, mem, I32(bufferDepth))
                    }

                    // Add a backedge from the last reader back to the writer
                    memoryBufferNotifs.register(readers.last, writer, mem, I32(bufferDepth), mem.bufferAmountOr1)
                }
            }

            foreach.block.stms foreach {
              case stmt if stmt.isCounter || stmt.isCounterChain =>
                subst += (stmt -> Invalid)
                dbgs(s"Eliding counter operations: ${stmt}")

              case s if canTransformMem(s) && shouldDuplicate(s) =>
                dbgs(s"Skipping re-staging $s since it can be transformed")
                subst += (s -> Invalid)

              case stmt if stmt.isControl && !stmt.isStreamControl =>
                val stmtReads = getReadRegs(stmt)
                val stmtWrites = getWrittenRegs(stmt)
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
                                stop.asInstanceOf[I32] - I32(pshift),
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
                      stageWithFlow(OpForeach(ens, ccnew, stageBlock {
                        val isFirstIter = newiters.takeRight(iters.size)
                        val en = isFirstIter.map {
                          i => i === i.counter.ctr.start
                        }.toSet

                        val isLastEn = isFirstIter.map {
                          i =>
                            (i.unbox + i.counter.ctr.step.asInstanceOf[I32]) >= i.counter.ctr.end.asInstanceOf[I32]
                        }.toSet

                        // Hook up notifications
                        dbgs(s"Setting up notifications for $stmt")
                        dbgs(s"Recv FIFOs: ${memoryBufferNotifs.getRecvFIFOs(stmt)}")
                        dbgs(s"Send FIFOs: ${memoryBufferNotifs.getSendFIFOs(stmt)}")


                        memTokens = (memoryBufferNotifs.getRecvFIFOs(stmt) map {
                          case(mem, fifo) =>
                            val token = stage(FIFODeq(fifo, en))
                            dbgs(s"Staging token: $mem, $fifo, $token")
                            (f(mem) -> token)
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

                        val childShifts = computeShifts(shape)
                        dbgs(s"Unrolling with shifts: $childShifts")

                        childShifts foreach {
                          childShift =>
                            isolateSubst() {
                              val shift = parentShift ++ childShift
                              dbgs(s"Processing shift: $shift")
                              (alliters zip newiters) zip shift foreach {
                                case ((oldIter, newIter), s) =>
                                  val shifted = newIter + I32(s)
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

                        retimeGate()
                        memoryBufferNotifs.getSendFIFOs(stmt) foreach {
                          case(mem, fifo) =>
                            dbgs(s"Sending fifo: $mem, $fifo")
                            stage(FIFOEnq(fifo, memTokens(f(mem)), isLastEn))
                        }
                      }, newiters, stopWhen)) {
                        lhs2 =>
                          dbgs(s"Filling in buffering information: $memTokens")
                          dbgs(s"Blocks: ${lhs2.blocks}")
                          lhs2.blocks.foreach {
                            blk =>
                              dbgs(s"Blk: ${blk.nestedStms}")
                              blk.nestedStms foreach {
                                stmt =>
                                  val mems = stmt.readMem ++ stmt.writtenMem
                                  dbgs(s"$stmt -> $mems")
                                  mems foreach {
                                    mem => memTokens.get(mem) match {
                                      case Some(ind) => stmt.bufferIndex = ind
                                      case None =>
                                    }
                                  }
                              }
                          }
                      }
                  }
                }
              case stmt if shouldDuplicate(stmt) =>
                dbgs(s"Re-staging memory: $stmt")
                subst += (stmt -> mirrorSym(stmt))
              case stmt if !stmt.isControl =>
                dbgs(s"Didn't know how to convert: $stmt of type ${stmt.op}")
                subst += (stmt -> mirrorSym(stmt))
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
//    dbgs(s"Transforming: $lhs = $rhs")
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