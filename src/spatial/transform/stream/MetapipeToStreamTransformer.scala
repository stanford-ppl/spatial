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

  override val recurse = Recurse.Always

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

    // can transform if all children are foreach loops
    lhs.blocks.flatMap(_.stms).forall {
      case stmt@Op(foreach:OpForeach) =>
        true
      case s if s.isMem =>
        val result = (s.writers union s.readers) forall {
          case Op(_:StreamOutWrite[_]) | Op(_:StreamInRead[_]) =>
            false
          case _ => true
        }
        dbgs(s"$result: ${s} = ${s.op}")
        result
      case s if s.isCounterChain => true
      case Op(ctr:CounterNew[_]) =>
        // Allowed if all params are either static or defined outside parent.
        dbgs(s"Checking Counter params are either static or defined outside of parent ($lhs): ${ctr.inputs}")
        ctr.inputs forall { sym =>
          dbgs(s"$sym: Const: ${sym.isConst} = ${sym.op}")
          dbgs(s"$sym: HasImmediateParent: ${sym.hasAncestor(lhs.toCtrl)}")
          sym.isConst || !sym.hasAncestor(lhs.toCtrl)
        }
//      case s if s.isTransient => true
      case s =>
        dbgs(s"Disallowed: ${s} = ${s.op}")
        false
    }
  }

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
    singleWrites.filter(_.isSRAM).foreach {
      case sr: SRAM[_, _] => sr.nonbuffer
      case _ =>
    }
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

                        val remaps = childShifts map {
                          cShift =>
                              val childShift = (cchain.counters zip cShift) map {
                                case (ctr, shift) =>
                                  implicit def numEV: Num[ctr.CT] = ctr.CTeV.asInstanceOf[Num[ctr.CT]]
                                  implicit def castEV: Cast[ctr.CT, I32] = argon.lang.implicits.numericCast[ctr.CT, I32]
                                  ctr.step.asInstanceOf[ctr.CT].to[I32] * I32(shift)
                              }

                              val shift = parentShift.shift ++ childShift
                              dbgs(s"Staging Calcs for shift: $shift")
                              ((foreach.iters ++ iters) zip newIters) zip shift map {
                                case ((oldIter, newIter), s) =>
                                  val shifted = newIter + s
                                  (oldIter -> shifted)
                              }
                        }
                        // We need to do some fancy equivalent of isolateSubst between the different shifts.
                        val remapSubsts = collection.mutable.Map(
                          (remaps map {
                            key =>
                              key -> (subst ++ key)
                          }):_*)
                        val cyclingVisit = (stmt: Sym[_]) => {
                          remaps foreach {
                            remap =>
                              // Load the subst for this copy of the controller
                              subst = remapSubsts(remap)
                              visit(stmt)
                              // persist the modified subst to remapSubsts
                              remapSubsts(remap) = subst
                          }
                        }
                        isolateSubst() { inCopyMode(true) {
                          block.stms.foreach {
                            case oldForeach@Op(OpForeach(ens, cchain, block, iters, stopWhen)) if cchain.isStatic && (cchain.approxIters == 1) =>
                              // Complex condition because we transform unitpipes into Foreach loops before this.
                              // In this case, we collapse the iterations together while replicating.
                              // To aid with analysis, we perform the same rotating remapping.
                              // if the controller only runs for 1 iteration, remap iter to ctr.start
                              // currently doesn't handle par factors.
                              // TODO(stanfurd): Handle Par Factors
                              dbgs(s"Intelligently unrolling single-iteration loop $oldForeach = ${oldForeach.op}")
                              iters foreach {
                                iter =>
                                  remaps foreach {
                                    remap =>
                                      subst = remapSubsts(remap)
                                      remapSubsts(remap) += (iter -> f(iter.counter.ctr.start))
                                  }
                              }
                              stageWithFlow(UnitPipe(f(ens), stageBlock {
                                block.stms.foreach(cyclingVisit)
                              }, f(stopWhen))) {lhs2 => transferData(oldForeach, lhs2)}

                            case stmt =>
                              dbgs(s"Dumbly handling: $stmt = ${stmt.op}")
                              cyclingVisit(stmt)
                          }
                        }}

                        val isLastEn = innerIters.map {
                          i =>
                            (i.unbox + i.counter.ctr.step.asInstanceOf[I32]) >= i.counter.ctr.end.asInstanceOf[I32]
                        }.toSet
                        memoryBufferNotifs.getSendFIFOs(stmt) foreach {
                          case(mem, fifo) =>
                            dbgs(s"Sending fifo: $mem, $fifo")
                            stage(FIFOEnq(fifo, memTokens(f(mem)), isLastEn))
                        }

                        dbgs(s"stmt: $stmt")
                        dbgs(s"Duplication Read FIFOs: $duplicationReadFIFOs")
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
//                subst += (stmt -> mirrorSym(stmt))
                inCopyMode(true) {
                  register(stmt -> mirrorSym(stmt))
                }
              case stmt if !stmt.isControl =>
                dbgs(s"Default Mirroring: $stmt of type ${stmt.op}")
                register(stmt -> mirrorSym(stmt))
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

      case foreach:OpForeach if inHw && canTransform(lhs, rhs) && foreach.cchain.isStatic && foreach.cchain.approxIters > 1 =>
        dbgs(s"Transforming: $lhs = $rhs")
//        converged = false
        indent {
          transformForeach(lhs, foreach)
        }

      case genericControl: Control[_] =>
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
        super.transform(lhs, rhs)

      case _ =>
        indent {
          val result = super.transform(lhs, rhs)
          dbgs(s"$lhs = $rhs")
          dbgs(s"    $result = ${result.op}")
          result
        }
    }).asInstanceOf[Sym[A]]
  }
}

case class ParentControl[R:Type](ctrl: Control[R])
case class ChildControl[R:Type](ctrl:Control[R])
case class ParentShift(shift: Seq[I32])
