package spatial.transform.stream

import argon._
import argon.passes.RepeatableTraversal
import argon.tags.struct
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.node._
import spatial.transform.{AllocMotion, PipeInserter}
import spatial.traversal.AccelTraversal

import scala.collection.mutable

/** Converts Metapipelined controllers into streamed controllers.
  */
case class MetapipeToStreamTransformer(IR: State) extends MutateTransformer with AccelTraversal
  with MetaPipeToStreamBase with
  RepeatableTraversal with CounterChainToStream with StreamMemoryTracker {

  @struct case class ReduceData[T: Bits](payload: T, emit: Bit, last: Bit)

  private val allowableSchedules = Set[CtrlSchedule](Pipelined, Sequenced)
  private def canTransform[A: Type](lhs: Sym[A], rhs: Op[A]): Boolean = {
    dbgs(s"Considering: $lhs = $rhs")
    // No point in converting inner controllers
    if (lhs.isInnerControl) {
      return false
    }

    dbgs(s"Schedule: ${lhs.schedule}")
    if (!allowableSchedules.contains(lhs.schedule)) {
      return false
    }

    if (lhs.children.size <= 1) {
      return false
    }

    if (lhs.shouldConvertToStreamed.isDefined) {
      return lhs.shouldConvertToStreamed.get
    }

    // Don't transform if unitpipe
    dbgs(s"Expected Iters: ${lhs.approxIters}")
    if (lhs.approxIters <= 1) {
      return false
    }

    val hasForbidden = lhs.blocks.flatMap(_.nestedStms).exists {
      case Op(_:StreamOutWrite[_]) | Op(_:StreamInRead[_]) | Op(_:StreamInNew[_]) | Op(_:StreamOutNew[_]) =>
        true
      case _ => false
    }
    if (hasForbidden) { return false }

//    return lhs.shouldConvertToStreamed.getOrElse(false)

    // can transform if all children are foreach loops
    lhs.blocks.flatMap(_.stms).forall {
      case stmt@Op(foreach:OpForeach) =>
        dbgs(s"True: ${stmt} = ${stmt.op}")
        true
      case stmt@Op(red:OpReduce[_]) =>
        dbgs(s"True: ${stmt} = ${stmt.op}")
        true
      case s if s.isMem =>
        val result = (s.writers union s.readers) forall {
          case Op(_:StreamOutWrite[_]) | Op(_:StreamInRead[_]) =>
            false
          case _ => true
        }
        dbgs(s"$result: ${s} = ${s.op}")
        result
      case s if s.isCounter || s.isCounterChain => true
      case s =>
        dbgs(s"False: ${s} = ${s.op}")
        false
    }
  }

  private def transformForeach[A: Type](lhs: Sym[A], foreach: OpForeach): Sym[Void] = {

    val parentPars = foreach.cchain.counters map { ctr => ctr.ctrParOr1 }
    val parentShifts = spatial.util.computeShifts(parentPars)

    val internalMems = foreach.block.internalMems.toSet
    val internalRegs = internalMems.filter(_.isReg)

    dbgs(s"InternalMems: ${internalMems.mkString(", ")}")

    val nonLocalUses = computeNonlocalUses(lhs)
    dbgs(s"NonLocal Uses: $nonLocalUses")

    def getReadRegs(s: Sym[_]) = (s.effects.reads union s.effects.writes) intersect internalRegs

    def getWrittenRegs(s: Sym[_]) = s.effects.writes intersect internalRegs

    // Transforms the foreach into a streampipe of foreaches
    val replacement = stageWithFlow(UnitPipe(
      foreach.ens, stageBlock {
        // for each parent shift, we restage the entire thing.

        // for each block which reads this mem, convert it into a FIFO.

        parentShifts foreach {
          pShift =>

            implicit val parentShift = ParentShift((foreach.cchain.counters zip pShift) map {
              case (ctr, shift) =>
                implicit def NumEV: Num[ctr.CT] = ctr.CTeV.asInstanceOf[Num[ctr.CT]]
                implicit def cast: Cast[ctr.CT, I32] = argon.lang.implicits.numericCast[ctr.CT, I32]
                ctr.step.asInstanceOf[ctr.CT].to[I32] * I32(shift)
            })

            initializeMemoryTracker(foreach.block.stms, nonLocalUses)

            val newParentCtrs = (foreach.cchain.counters zip parentShift.shift) map {
              case (ctr, pshift) =>
                ctr match {
                  case Op(CounterNew(start, stop, step, par)) =>
                    // we're handling the parent par at a high level.
                    stage(CounterNew(
                      f(start).asInstanceOf[I32],
                      f(stop).asInstanceOf[I32] - pshift,
                      f(step).asInstanceOf[I32] * par, I32(1)
                    ))

                  case Op(ForeverNew()) =>
                    // Don't need to shift, since ForeverNews are parallelized by 1.
                    stage(ForeverNew())
                }
            }

            foreach.block.stms foreach {
              case stmt if stmt.isCounter || stmt.isCounterChain =>
                dbgs(s"Eliding counter operations: ${stmt}")

              case s if canTransformMem(s) && shouldDuplicate(s) =>
                dbgs(s"Skipping re-staging $s since it can be transformed")

              case stmt if stmt.isControl =>
                val stmtReads = getReadRegs(stmt)
                val stmtWrites = getWrittenRegs(stmt)
                dbgs(s"Stmt: $stmt, effects: ${stmt.effects} reads: $stmtReads, writes: $stmtWrites")
                // for each stmtRead, we need to create a register of the same type inside.

                // If the register is read, then it is either written by a previous controller, or we use the
                // default value.

                // If the register is written, then we create a local copy, which we then enqueue at the end
                // of the controller.
                // Since writes can be conditional, we must read the previous value before muxing between the
                // two on enqueue.

                indent { isolateSubst() {
                  stmt match {
                    case loop@Op(loopCtrl@OpForeach(ens, cchain, block, iters, stopWhen)) =>
                      dbgs(s"Staging fused loop $loop = ${loop.op}")
                      // Unroll early here.
                      // start, stop, step, par ->
                      // start, stop, step * par, 1


                      val shape = cchain.counters map {_.ctrParOr1}

                      val (ccnew, newIters) = stagePreamble(loopCtrl.asInstanceOf[Control[_]], newParentCtrs, foreach.iters)

                      stage(OpForeach(ens, ccnew, stageBlock {
                        val innerIters = newIters.takeRight(iters.size)
                        val en = innerIters.map {
                          i => i === i.counter.ctr.start
                        }.toSet

                        // Hook up notifications
                        dbgs(s"Setting up notifications for $stmt")
                        dbgs(s"Recv FIFOs: ${memoryBufferNotifs.getRecvFIFOs(stmt)}")
                        dbgs(s"Send FIFOs: ${memoryBufferNotifs.getSendFIFOs(stmt)}")

                        val memTokens = getMemTokens(stmt, en, memoryBufferNotifs)

                        dbgs(s"MemTokens: ${memTokens}")

                        dbgs(s"Blk: ${block.nestedStms}")
                        block.nestedStms foreach {
                          stmt =>
                            val mems = stmt.readMem ++ stmt.writtenMem
                            mems foreach {
                              mem =>
                                memTokens.get(f(mem)) match {
                                  case Some(ind) =>
                                    dbgs(s"Adding Buffering Info to: $stmt = $ind")
                                    stmt.bufferIndex = ind
                                  case None =>
                                }
                            }
                        }

                        val childShifts = spatial.util.computeShifts(shape)
                        dbgs(s"Unrolling with shifts: $childShifts")

                        val cloned = registerDuplicationFIFOReads(stmtReads, duplicationReadFIFOs.getOrElse(stmt, mutable.Map.empty).toMap, en)

                        childShifts foreach {
                          cShift =>
                            isolateSubst() {
                              val childShift = (cchain.counters zip cShift) map {
                                case (ctr, shift) =>
                                  implicit def numEV: Num[ctr.CT] = ctr.CTeV.asInstanceOf[Num[ctr.CT]]
                                  implicit def castEV: Cast[ctr.CT, I32] = argon.lang.implicits.numericCast[ctr.CT, I32]
                                  ctr.step.asInstanceOf[ctr.CT].to[I32] * I32(shift)
                              }

                              val shift = parentShift.shift ++ childShift
                              dbgs(s"Processing shift: $shift")
                              ((foreach.iters ++ iters) zip newIters) zip shift foreach {
                                case ((oldIter, newIter), s) =>
                                  val shifted = newIter + s
                                  subst += (oldIter -> shifted)
                              }

                              isolateSubst() { indent {
                                block.stms foreach {
                                  stm =>
                                    dbgs(s"Mirroring Inside Loop: $stm = ${stm.op}")
                                    register(stm -> mirrorSym(stm))
                                    dbgs(s"  -> ${f(stm)} = ${f(stm).op}")
                                }
                              }}
                            }
                        }

                        val isLastEn = innerIters.map {
                          i =>
                            (i.unbox + i.counter.ctr.step.asInstanceOf[I32]) >= i.counter.ctr.end.asInstanceOf[I32]
                        }.toSet
                        memoryBufferNotifs.getSendFIFOs(stmt) foreach {
                          case(mem, fifo) =>
                            dbgs(s"Sending fifo: $mem, $fifo")
                            stage(FIFOEnq(fifo, memTokens(f(mem)), isLastEn))
                        }

                        dbgs(s"Duplication Write FIFOs: $duplicationWriteFIFOs")

                        stmtWrites foreach {
                          case wr: Reg[_] if duplicationWriteFIFOs(stmt) contains wr.asSym =>
                            // Write the write register to the FIFO.
                            implicit lazy val ev: Bits[wr.RT] = wr.A.asInstanceOf[Bits[wr.RT]]
                            val read = cloned(wr).asInstanceOf[Reg[wr.RT]].value
                            duplicationWriteFIFOs(stmt)(wr.asSym) foreach {
                              s =>
                                val of = s.asInstanceOf[FIFO[wr.RT]]
                                stage(FIFOEnq(of, read.asInstanceOf[Bits[wr.RT]], isLastEn))
                            }
                          case _ =>
                        }
                      }, newIters, stopWhen))
                  }
                }}
              case stmt if shouldDuplicate(stmt) =>
                dbgs(s"Re-staging memory: $stmt")
                subst += (stmt -> mirrorSym(stmt))
              case stmt if !stmt.isControl =>
                dbgs(s"Didn't know how to convert: $stmt of type ${stmt.op}")
                subst += (stmt -> mirrorSym(stmt))
              case stmt if stmt.isControl =>
                error(s"Could not convert controller: $stmt of type ${stmt.op}")
                throw new Exception()
            }
        }
      }, foreach.stopWhen
    )) {
      lhs2 =>
        lhs2.rawSchedule = Streaming
        lhs2.userSchedule = Streaming
        lhs2.shouldConvertToStreamed = false
        lhs2.explicitName = lhs.explicitName.getOrElse(s"MetapipeToStream_${lhs}")
    }.asSym

    replacement
  }

  val visitStack = mutable.ArrayStack[Sym[_]]()
  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    (rhs match {
      case AccelScope(_) => inAccel {
        super.transform(lhs, rhs)
      }

      case foreach:OpForeach if inHw && canTransform(lhs, rhs) =>
        dbgs(s"Transforming: $lhs = $rhs")
        visitStack.push(lhs)
        val result = isolateSubst(lhs) {
          converged = false
          indent {
            transformForeach(lhs, foreach)
          }
        }
        visitStack.pop()
        result

      case _ => super.transform(lhs, rhs)
    }).asInstanceOf[Sym[A]]
  }
}

case class ParentControl[R:Type](ctrl: Control[R])
case class ChildControl[R:Type](ctrl:Control[R])
case class ParentShift(shift: Seq[I32])
