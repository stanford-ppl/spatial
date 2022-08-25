package spatial.transform.streamify

import argon._
import argon.transform._
import spatial.lang._
import spatial.metadata.bounds.Final
import spatial.node._
import spatial.traversal.AccelTraversal
import spatial.util.TransformUtils._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.access._
import spatial.util.computeShifts

import scala.collection.immutable.{ListMap => LMap}

case class EarlyUnroller(IR: State) extends ForwardTransformer with AccelTraversal with spatial.util.TransformerUtilMixin with spatial.util.CounterIterUpdateMixin {
//  private def hasParFactor(cchain: CounterChain): Boolean = !cchain.counters.forall(_.ctrParOr1 == 1)

  private var laneMap: LMap[Sym[_], Int] = LMap.empty
  case class UnrollState(iterLanes: LMap[Sym[_], Int]) extends TransformerState {
    override def restore(): Unit = {
      laneMap = iterLanes
    }
  }

  override def saveSubsts(): TransformerStateBundle = {
    super.saveSubsts() ++ Seq(UnrollState(laneMap))
  }

  private def possiblyOOB(shift: Int, ctr: Counter[_]): Boolean = {
    if (ctr.isForever) { return false }
    (ctr.start, ctr.step, ctr.end, ctr.ctrPar) match {
      case (Final(start), Final(step), Final(end), Final(par)) =>
        val mod = (end - start) % (step * par)
        if (mod == 0) { false } else { shift >= mod }
      case _ => false
    }
  }

  private def enableBit = if (enables.isEmpty) { Bit(true) } else { enables.toSeq.reduceTree(_ && _)}

  def unrollForeach(lhs: Sym[_], foreachOp: OpForeach): Sym[_] = {
    dbgs(s"Transforming: $lhs = $foreachOp")
    val newCChain = expandCounterPars(foreachOp.cchain)
    val newIters = makeIters(newCChain.counters)
    val shifts = computeShifts(foreachOp.cchain.counters map {
      _.ctrParOr1
    })

    stageWithFlow(OpForeach(f(foreachOp.ens), newCChain, stageBlock {
      val substitutions = shifts.map(shift => createSubstData {
        (foreachOp.iters zip newIters zip shift zip foreachOp.cchain.counters) foreach {
          case (((oldIter, newIter), parShift), ctr) =>
            val castedShift = ctr.CTeV.from(parShift)
            val replacement = () => {
              val offset = ctr.step.asInstanceOf[Num[ctr.CT]] * castedShift.asInstanceOf[ctr.CT]
              newIter.asInstanceOf[Num[ctr.CT]] + offset.asInstanceOf[ctr.CT]
            }
            // Registered old iters to shifted iters
            register(oldIter, replacement)
            dbgs(s"Appending to LaneMap: $oldIter -> $parShift")
            laneMap += oldIter -> parShift

            // Check if the replacement iter is still valid
            // If it is possible to run off this edge of the counter, we include this
            // otherwise the enable would be always true, and this just creates extra HW
            if (possiblyOOB(parShift, ctr)) {
              val isInBounds = replacement.asInstanceOf[Num[ctr.CT]] < ctr.end.asInstanceOf[ctr.CT]
              enables += isInBounds
            }
        }
      })
      val newSubsts = indent { visitWithSubsts(substitutions, foreachOp.block.stms) }
//      (substitutions zip newSubsts) foreach {
//        case (oldSub, newSub) =>
//          dbgs(s"${oldSub.mkString(",")} -> ${newSub.mkString(", ")}")
//      }
      spatial.lang.void
    }, newIters.asInstanceOf[Seq[I32]], f(foreachOp.stopWhen))) {
      lhs2 =>
        transferData(lhs, lhs2)
        lhs2.ctx = lhs2.ctx.copy(previous = Seq(lhs.ctx))
    }
  }

    def unrollReduce[A: Bits](lhs: Sym[_], reduceOp: OpReduce[A]): Sym[_] = {
      dbgs(s"Transforming: $lhs = $reduceOp")
      val newCChain = expandCounterPars(reduceOp.cchain)
      val newIters = makeIters(newCChain.counters)
      val shifts = computeShifts(reduceOp.cchain.counters map {
        _.ctrParOr1
      })

      // To partially unroll a reduce -- create a new Map block with a reduction tree inside.
      stageWithFlow(OpReduce(f(reduceOp.ens), newCChain, f(reduceOp.accum), stageBlock {
        val substitutions = shifts.map(shift => createSubstData {
          (reduceOp.iters zip newIters zip shift zip reduceOp.cchain.counters) foreach {
            case (((oldIter, newIter), parShift), ctr) =>
              val castedShift = ctr.CTeV.from(parShift)
              val replacement = {
                val offset = ctr.step.asInstanceOf[Num[ctr.CT]] * castedShift.asInstanceOf[ctr.CT]
                newIter.asInstanceOf[Num[ctr.CT]] + offset.asInstanceOf[ctr.CT]
              }
              // Registered old iters to shifted iters
              register(oldIter, replacement)

              // Check if the replacement iter is still valid
              if (possiblyOOB(parShift, ctr)) {
                val isInBounds = replacement.asInstanceOf[Num[ctr.CT]] < ctr.end.asInstanceOf[ctr.CT]
                enables += isInBounds
              }
          }
        })
        val newSubsts = visitWithSubsts(substitutions, reduceOp.map.stms)
        (substitutions zip newSubsts) foreach {
          case (oldSub, newSub) =>
            dbgs(s"${oldSub.mkString(",")} -> ${newSub.mkString(", ")}")
        }

        // need to filter the results based on which ones are valid
        // If Ident is given, use Ident
        // Otherwise, need to build a mux tree of enables as well
        reduceOp.ident match {
          case Some(identity) =>
            val results = mapSubsts(newSubsts)({
              mux(enableBit, f(reduceOp.map.result).unbox, f(identity))
            }).map(_.unbox)
            val reduced = results.reduceTree(reduceOp.reduce.reapply)
            reduced.asInstanceOf[Bits[A]].asSym
          case None =>
            val resultsWithEns = mapSubsts(newSubsts) {
              (f(reduceOp.map.result), enableBit)
            }
            (resultsWithEns.reduceTree {
              case ((val1, valid1), (val2, valid2)) =>
                val resultValid = valid1 | valid2
                // if valid1 and valid2: then we take the reduce
                // if valid1 and not valid2 then we take val1
                // since we're worried about going over the edge, we don't need to worry about the last case
                (mux(valid2, reduceOp.reduce.reapply(val1.unbox, val2.unbox), val1.unbox), resultValid)
            })._1
        }

      }, f(reduceOp.load), f(reduceOp.reduce), f(reduceOp.store), f(reduceOp.ident), f(reduceOp.fold), newIters.asInstanceOf[Seq[I32]].toList, f(reduceOp.stopWhen))) {
        lhs2 =>
          transferData(lhs, lhs2)
          lhs2.ctx = lhs2.ctx.copy(previous = Seq(lhs.ctx))
      }
    }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case _:AccelScope => inAccel { super.transform(lhs, rhs) }
    case foreach:OpForeach if inHw =>
      unrollForeach(lhs, foreach)
    case reduceOp:OpReduce[_] if inHw =>
      type T = reduceOp.A.R
      implicit def bitsEV: Bits[T] = reduceOp.A
      unrollReduce[T](lhs, reduceOp)
    case LaneStatic(iter, elems) if laneMap.contains(iter) =>
      iter.from(elems(laneMap(iter))).asSym
    // TODO ADD SUPPORT FOR DISPATCHES
    case _ if lhs.getDispatches.nonEmpty && inHw =>
      dbgs(s"Processing Dispatches for $lhs = $rhs in $laneMap")
      val accessedMem = (lhs.readMem ++ lhs.writtenMem).head
      indent {
        dbgs(s"AccessedMem: $accessedMem")
        val iterators = accessIterators(lhs, accessedMem)
        dbgs(s"Iterators: ${accessIterators(lhs, accessedMem)}")
        val mappedIterator = iterators.map(laneMap(_)).toList
        dbgs(s"MappedIterator: $mappedIterator")
        dbgs(s"All Dispatches: ${lhs.dispatches}")
        val newIterator = mappedIterator.map(_ => 0)
        val v = mirrorSym(lhs)
        v.dispatches = Map(newIterator -> lhs.dispatches(mappedIterator))
        v.clearPorts

        lhs.dispatches(mappedIterator).foreach {
          disp =>
            val ports = lhs.ports(disp)
            ports.filter{_._1 == mappedIterator}.foreach {
              case (_, port) =>
                v.addPort(disp, newIterator, port)
            }
        }

        v.gids = Map(newIterator -> lhs.gids(mappedIterator))

        val keyReplacement = (laneMap.map {
          case (iter, offset) => iter.unbox.asInstanceOf[Idx] -> (f(iter).asInstanceOf[Idx], offset)
        }).toMap[Idx, (Idx, Int)]
        v.affineMatrices = lhs.affineMatrices.collect {
          case AccessMatrix(_, matrix, unroll, isReader) if unroll == mappedIterator =>
            AccessMatrix(v, matrix.replaceKeys(keyReplacement), newIterator, isReader)
        }
        v
      }

    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]

  printRegister = true
}
