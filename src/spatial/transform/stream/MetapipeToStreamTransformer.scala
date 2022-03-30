package spatial.transform.stream

import argon._
import argon.passes.RepeatableTraversal
import argon.tags.struct
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.transform._
import spatial.node._
import spatial.traversal.AccelTraversal
import spatial.util.TransformerUtilMixin

import scala.collection.mutable

/** Converts Metapipelined controllers into streamed controllers.
  */
case class MetapipeToStreamTransformer(IR: State) extends MutateTransformer with AccelTraversal
  with MetaPipeToStreamBase with
  RepeatableTraversal with CounterChainToStream with StreamMemoryTracker with TransformerUtilMixin {

  override val recurse = Recurse.Always

  @struct case class ReduceData[T: Bits](payload: T, emit: Bit, last: Bit)

  private def transformForeach[A: Type](lhs: Sym[A], foreach: OpForeach): Sym[Void] = {

    val parentPars = foreach.cchain.counters map { ctr => ctr.ctrParOr1 }
    val parentShifts = spatial.util.computeShifts(parentPars)

    val internalMems = foreach.block.internalMems.toSet
    val internalRegs = internalMems.filter(_.isReg)

    dbgs(s"InternalMems: ${internalMems.mkString(", ")}")

    val (singleUse, nonLocalUses) = computeNonlocalUses(lhs)
    dbgs(s"NonLocal Uses: $nonLocalUses")
    dbgs(s"Single Uses: $singleUse")
    val singleWrites = singleUse intersect foreach.effects.writes

    dbgs(s"Single Writes: $singleWrites")
    val singleReads = (singleUse intersect foreach.effects.reads) diff singleWrites
    dbgs(s"Single Reads: $singleReads")

    def getReadRegs(s: Sym[_]) = (s.effects.reads union s.effects.writes) intersect internalRegs

    def getWrittenRegs(s: Sym[_]) = s.effects.writes intersect internalRegs

    // Transforms the foreach into a streampipe of foreaches
    val replacement = stageWithFlow(UnitPipe(
      foreach.ens, stageBlock {
        // for each parent shift, we restage the entire thing.

        parentShifts foreach {
          pShift =>

            implicit val parentShift = (foreach.cchain.counters zip pShift) map {
              case (ctr, shift) =>
                implicit def NumEV: Num[ctr.CT] = ctr.CTeV.asInstanceOf[Num[ctr.CT]]
                implicit def cast: Cast[ctr.CT, I32] = argon.lang.implicits.numericCast[ctr.CT, I32]
                ctr.step.asInstanceOf[ctr.CT].to[I32] * I32(shift)
            }

            initializeMemoryTracker(foreach.block.stms, nonLocalUses)

            foreach.block.stms foreach {
              case stmt if stmt.isCounter || stmt.isCounterChain =>
                dbgs(s"Eliding counter operations: ${stmt}")

              case s if canTransformMem(s) && shouldDuplicate(s) =>
                dbgs(s"Skipping re-staging $s since it can be transformed")

              case loop@Op(loopCtrl@OpForeach(ens, cchain, block, iters, stopWhen)) =>
                val stmtReads = getReadRegs(loop)
                val stmtWrites = getWrittenRegs(loop)
                dbgs(s"Stmt: $loop, effects: ${loop.effects} reads: $stmtReads, writes: $stmtWrites")
                // for each stmtRead, we need to create a register of the same type inside.

                // If the register is read, then it is either written by a previous controller, or we use the
                // default value.

                // If the register is written, then we create a local copy, which we then enqueue at the end
                // of the controller.
                // Since writes can be conditional, we must read the previous value before muxing between the
                // two on enqueue.
                dbgs(s"Staging fused loop $loop = ${loop.op}")
                val shape = cchain.counters map {
                  _.ctrParOr1
                }


                // Hook up notifications
                dbgs(s"Setting up notifications for $loop")
                dbgs(s"Recv FIFOs: ${memoryBufferNotifs.getRecvFIFOs(loop)}")
                dbgs(s"Send FIFOs: ${memoryBufferNotifs.getSendFIFOs(loop)}")

                val childShifts = spatial.util.computeShifts(shape)
                dbgs(s"Unrolling with shifts: $childShifts")

                val newParentCtrs = (foreach.cchain.counters zip parentShift) map {
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

                val (ccnew, newIters) = stagePreamble(loopCtrl.asInstanceOf[Control[_]], newParentCtrs, foreach.iters)

                def remaps = childShifts map {
                  cShift =>
                    createSubstData {
                      val childShift = (cchain.counters zip cShift) map {
                        case (ctr, shift) =>
                          implicit def numEV: Num[ctr.CT] = ctr.CTeV.asInstanceOf[Num[ctr.CT]]

                          implicit def castEV: Cast[ctr.CT, I32] = argon.lang.implicits.numericCast[ctr.CT, I32]

                          ctr.step.asInstanceOf[ctr.CT].to[I32] * I32(shift)
                      }

                      val shift = parentShift ++ childShift
                      dbgs(s"Staging Calcs for shift: $shift")
                      ((foreach.iters ++ iters) zip newIters) zip shift map {
                        case ((oldIter, newIter), s) =>
                          register(oldIter, () => {
                            newIter + s
                          })
                      }
                    }
                }

                stageWithFlow(OpForeach(ens, ccnew, stageBlock {
                  val innerIters = newIters.takeRight(iters.size)
                  val isFirst = innerIters.map(spatial.util.TransformUtils.isFirstIter(_)).toSet
                  val memTokens = getMemTokens(loop, isFirst, memoryBufferNotifs)
                  dbgs(s"MemTokens: ${memTokens}")
                  block.nestedStms foreach {
                    stmt =>
                      val mems = stmt.readMem ++ stmt.writtenMem
                      mems.filter(internalMems.contains) foreach {
                        mem =>
                          memTokens.get(f(mem)) match {
                            case Some(ind) =>
                              dbgs(s"Adding Buffering Info to: $stmt = $ind")
                              stmt.bufferIndex = ind
                            case None =>
                          }
                      }
                  }

                  val cloned = registerDuplicationFIFOReads(loop.isOuterControl)(stmtReads, duplicationReadFIFOs.getOrElse(loop, mutable.Map.empty).toMap, isFirst)

                  visitWithSubsts(remaps, block.stms)

                  val isLastEn = innerIters.map(spatial.util.TransformUtils.isLastIter(_)).toSet
                  memoryBufferNotifs.getSendFIFOs(loop) foreach {
                    case (mem, fifo) =>
                      dbgs(s"Sending fifo: $mem, $fifo")
                      stage(FIFOEnq(fifo, memTokens(f(mem)), isLastEn))
                  }

                  stmtWrites foreach {
                    case wr: Reg[_] if duplicationWriteFIFOs.getOrElse(loop, mutable.Map.empty) contains wr.asSym =>
                      // Write the write register to the FIFO.
                      implicit lazy val ev: Bits[wr.RT] = wr.A.asInstanceOf[Bits[wr.RT]]
                      val read = cloned(wr).asInstanceOf[Reg[wr.RT]].value
                      duplicationWriteFIFOs(loop)(wr.asSym) foreach {
                        s =>
                          val of = s.asInstanceOf[FIFO[wr.RT]]
                          stage(FIFOEnq(of, read.asInstanceOf[Bits[wr.RT]], isLastEn))
                      }
                    case _ =>
                  }
                }, newIters, f(stopWhen))) {
                  lhs2 =>
                    lhs2.ctx = lhs2.ctx.copy(previous=Seq(loop.ctx))
                    lhs2.prevNames = lhs2.prevNames ++ loop.prevNames
                    loop.getUserSchedule match {
                      case Some(sched) => lhs2.userSchedule = sched
                      case None =>
                    }
                    lhs2.haltIfStarved = false
                }
              case stmt if shouldDuplicate(stmt) =>
                dbgs(s"Re-staging memory: $stmt")
                register(stmt -> mirrorSym(stmt))
              case stmt if !stmt.isControl =>
                dbgs(s"Default Mirroring: $stmt of type ${stmt.op}")
                visit(stmt)
              case stmt if stmt.isControl =>
                error(s"Could not convert controller: $stmt of type ${stmt.op}")
                throw new Exception()
            }
        }
      }, f(foreach.stopWhen)
    )) {
      lhs2 =>
        lhs2.rawSchedule = Streaming
        lhs2.userSchedule = Streaming
        lhs2.shouldConvertToStreamed = false
        lhs2.explicitName = lhs.explicitName.getOrElse(s"MetapipeToStream_${lhs}")
    }.asSym

    replacement
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    (rhs match {
      case AccelScope(_) => inAccel {
        super.transform(lhs, rhs)
      }

      case foreach:OpForeach if lhs.streamify =>
        dbgs(s"Transforming: $lhs = $rhs")
        converged = false
        indent {
          transformForeach(lhs, foreach)
        }

      case _: CounterNew[_] if copyMode =>
        val newCounter = super.transform(lhs, rhs)
        val asCounter = lhs.asInstanceOf[Counter[_]]
        val oldIter = asCounter.iter.get
        implicit def EV: Num[asCounter.CT] = asCounter.CTeV.asInstanceOf[Num[asCounter.CT]]
        val newIter = boundVar[asCounter.CT]
        newIter.name = oldIter.name
        val lanes = oldIter.asInstanceOf[Num[_]].counter.lanes
        newIter.asInstanceOf[Num[asCounter.CT]].counter = IndexCounterInfo(newCounter.asInstanceOf[Counter[asCounter.CT]], lanes)
        register(oldIter -> newIter)
        newCounter

      case genericControl: Control[_] if copyMode =>
        dbgs(s"Control: $lhs = $rhs")
        genericControl.iters.foreach {
          iter =>
            val IndexCounterInfo(chain, lanes) = iter.counter
            val shouldRemap = chain != f(chain) && !subst.contains(iter)
            dbgs(s"Chain: $chain -> ${f(chain)} Should Remap: ($shouldRemap)")
            if (shouldRemap) {
              // Mirror across the IndexCounterInfo
              implicit def bEV: Bits[chain.CT] = chain.CTeV
              val newVar = boundVar[chain.CT].asInstanceOf[Bits[chain.CT]]
              newVar.counter = IndexCounterInfo(f(chain), lanes)
              register(iter -> newVar)
            }
        }
        indent {
          val result = super.transform(lhs, rhs)
          dbgs(s"$lhs = $rhs")
          dbgs(s"    $result = ${result.op}")
          result
        }
      case _ =>
        super.transform(lhs, rhs)
    }).asInstanceOf[Sym[A]]
  }
}

