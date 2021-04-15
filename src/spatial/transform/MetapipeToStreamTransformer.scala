package spatial.transform

import scala.collection.mutable
import argon._
import argon.node._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.util.shouldMotionFromConditional
import spatial.traversal.AccelTraversal
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.blackbox._
import spatial.metadata.access._
import spatial.metadata.control._

import scala.collection.mutable.ArrayBuffer

/** Converts Metapipelined controllers into streamed controllers.
  */
case class MetapipeToStreamTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  private val allowableSchedules = Set[CtrlSchedule](Pipelined, Sequenced)
  private def canTransform[A: Type](lhs: Sym[A], rhs: Op[A]): Boolean = {
    // No point in converting inner controllers
    if (lhs.isInnerControl) { return false }

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
    case r:Reg[_] => true
    case _ => false
  }


  private def transformForeach[A: Type](lhs: Sym[A], foreach: OpForeach): Sym[Void] = {
    // Transforms the foreach into a streampipe of foreaches
    val replacement = stageWithFlow(UnitPipe(
      foreach.ens, stageBlock {

        val internalMems = (foreach.block.stms filter canTransformMem).toSet

        def getReadMems(s: Sym[_]) = {
          (s.readMems union s.writtenMems) intersect internalMems
        }

        def getWrittenMems(s: Sym[_]) = {
          s.writtenMems intersect internalMems
        }

        // for each block which reads this mem, convert it into a FIFO.
        dbgs(s"InternalMems: ${internalMems.mkString(", ")}")

        // Memory -> Last Writer
        val lastWrite = scala.collection.mutable.Map[Sym[_], Sym[_]]()

        // Reader -> Memory -> FIFO
        val readFIFOs = mutable.Map[Sym[_], mutable.Map[Sym[_], Sym[_]]]()

        // Writer -> Memory -> FIFOs
        val writeFIFOs = mutable.Map[Sym[_], mutable.Map[Sym[_], mutable.ArrayBuffer[Sym[_]]]]()

        foreach.block.stms foreach {
          stmt =>
            getReadMems(stmt) foreach {
              mem =>
                lastWrite.get(mem) match {
                  case None =>
                    // Memory was never written before.
                  case Some(writer) =>
                    // Memory was previously written, now need a new fifo.
                    val writerLatency = math.ceil(writer.latencySum).toInt
                    val latencyEpsilon = 5
                    mem match {
                      case r: Reg[_] =>
                        lazy implicit val bits: Bits[r.L] = r.A.asInstanceOf[Bits[r.L]]
                        val newFIFO = stage(FIFONew[r.L](I32(writerLatency + latencyEpsilon)))
                        newFIFO.explicitName = r.explicitName.getOrElse(s"$r")
                        dbgs(s"WriteFIFOs: $writeFIFOs, writer: $writer")
                        writeFIFOs(writer).getOrElseUpdate(mem, mutable.ArrayBuffer.empty).append(newFIFO.asSym)
                        readFIFOs.getOrElseUpdate(stmt, mutable.Map.empty)(mem) = newFIFO.asSym
                    }
                }
            }

            getWrittenMems(stmt) foreach {
              case r: Reg[_] =>
                lastWrite(r) = stmt
                writeFIFOs.getOrElseUpdate(stmt, mutable.Map.empty)(r) = new ArrayBuffer[Sym[_]]()
            }
        }

        foreach.block.stms foreach {
          case s if canTransformMem(s) =>
            dbgs(s"Skipping re-staging $s since it can be transformed")

          case stmt if stmt.isControl =>
            val stmtReads = getReadMems(stmt)
            val stmtWrites = getWrittenMems(stmt)
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
                  val newctrs = (foreach.cchain.counters ++ cchain.counters).map {
                    case Op(CounterNew(start, stop, step, par)) =>
                      stage(CounterNew[I32](start.asInstanceOf[I32], stop.asInstanceOf[I32], step.asInstanceOf[I32], par))
                    case Op(ForeverNew()) =>
                      stage(ForeverNew())
                  }
                  val ccnew = stage(CounterChainNew(newctrs))
                  register(foreach.cchain -> ccnew)

                  val newiters = (foreach.iters ++ iters).zip(newctrs).map { case (i, ctr) =>
                    val n = boundVar[I32]
                    subst += (i -> n)
                    n.name = i.name
                    n.counter = IndexCounterInfo(ctr, Seq.tabulate(ctr.ctrParOr1) { i => i })
                    n
                  }

                  f(stage(OpForeach(ens, ccnew, stageBlock {
                    val isFirstIter = newiters.takeRight(iters.size)
                    val en = isFirstIter.map {
                      _ === I32(0)
                    }.toSet

                    stmtReads foreach {
                      case read: Reg[_] =>
                        type T = read.RT
                        lazy implicit val bT: Bits[T] = read.A.asInstanceOf[Bits[T]]
                        val tmp = read.__makeCopy.asInstanceOf[Reg[T]]
                        tmp.isWriteBuffer = true
                        cloned(read) = tmp

                        // All of the original register reads/writes are now delegated to a proxy register.
                        register(read -> tmp)
                        read.consumers filter {_.hasAncestor(stmt.toCtrl)} foreach {
                          user =>
                            user.op match {
                              case Some(op) =>
                                op.update(f)
                            }
                        }

                        tmp.explicitName = read.explicitName.getOrElse(s"InsertedReg_$read")
                        if (readFIFOs.getOrElse(stmt, mutable.Map.empty) contains read) {
                          val deq = stage(FIFODeq(readFIFOs(stmt)(read).asInstanceOf[FIFO[T]], en))
                          stage(RegWrite(tmp, deq, en))
                        }
                    }
                    // Within this scope, we rename this register.
                    block.stms foreach {
                      x =>
                        visit(f(x))
                    }

                    stmtWrites foreach {
                      case wr: Reg[_] =>
                        // Write the write register to the FIFO.
                        implicit lazy val ev: Bits[wr.RT] = wr.A.asInstanceOf[Bits[wr.RT]]
                        val read = cloned(wr).asInstanceOf[Reg[wr.RT]].value
                        writeFIFOs(stmt)(wr.asSym) foreach {
                          s =>
                            val of = s.asInstanceOf[FIFO[wr.RT]]
                            stage(FIFOEnq(of, read.asInstanceOf[Bits[wr.RT]], en))
                        }
                    }

                    void
                  }, newiters, stopWhen)))
              }
            }
          case stmt if !stmt.isControl =>
            dbgs(s"Didn't know how to convert: $stmt of type ${stmt.op}")
            stmt.op match {
              case Some(rhs) =>
                implicit val ev: Type[stmt.R] = rhs.R.asInstanceOf[Type[stmt.R]]
                transform[stmt.R](stmt.asInstanceOf[Sym[stmt.R]], rhs.asInstanceOf[Op[stmt.R]])
              case None =>
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

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    dbgs(s"Transforming: $lhs = $rhs")
    indent {
      (rhs match {
        case AccelScope(_) => inAccel {
          super.transform(lhs, rhs)
        }

        case foreach@OpForeach(ens, cchain, block, iters, stopWhen) if inHw && canTransform(lhs, rhs) =>
          indent {
            transformForeach(lhs, foreach)
          }

        case _ => dbgs(s"Passing Through $lhs = $rhs"); super.transform(lhs, rhs)
      }).asInstanceOf[Sym[A]]
    }
  }
}


