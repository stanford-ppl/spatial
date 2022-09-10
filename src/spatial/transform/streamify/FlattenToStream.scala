package spatial.transform.streamify

import argon.transform.{ForwardTransformer, MutateTransformer}
import argon._
import argon.node.IfThenElse
import argon.tags.struct
import spatial.lang._
import spatial.node._
import spatial.traversal.AccelTraversal

import scala.collection.{mutable => cm}
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.access._
import spatial.metadata.transform._
import spatial.util.TransformUtils._
import spatial.util._
import spatial.util.modeling._

/**
  * A PseudoIter represents an iteration variable that is sent along a FIFO in order to handle variable bounds
  * @param i: the value of the iterator
  * @param isFirst: whether this is the first iteration of this particular iterator
  * @param isLast: whether this is the last iteration of this particular iterator
  */
@struct case class PseudoIter(i: I32, isFirst: Bit, isLast: Bit)

/**
  * A bundle of pseudoIters, representing the leading iterators of a variable-iteration controller
  * @param iters: a vector of PseudoIter
  */
@struct case class PseudoIters[IterVec:Bits](iters: IterVec)


/**
  * FlattenToStream takes an arbitrary chunk of code and turns it into a single unitpipe containing
  *   many stream-synchronized controllers -- one for each inner controller.
  *
  * In addition, this also creates two auxiliary structures:
  *   1. Counter Generator and Token Intake
  *   2. Token Distributor
  */
case class FlattenToStream(IR: State)(implicit isl: poly.ISL) extends ForwardTransformer with AccelTraversal {

  type IterFIFO = FIFO[PseudoIters[Vec[PseudoIter]]]

  private val commFIFOs = cm.Map[TokenComm, FIFO[_]]()

  private var accelHandle: BundleHandle = null

  private var tokenComms: Seq[TokenComm] = null

  case class ControllerImpl(counterGen: Sym[_], main: Sym[_], finisher: Sym[_])
  private val innerControllerMap = cm.Map[Sym[_], ControllerImpl]()

  private def createFIFO[T: Bits](key: TokenComm)(implicit srcCtx: argon.SrcCtx) = {
    assert(!commFIFOs.contains(key), s"Already created a FIFO corresponding to $key at ${srcCtx}")
    IR.withScope(accelHandle) {
      val bufferDepth = computeBufferDepth(key.mem)
      val newFIFO = FIFO[T](2 * bufferDepth)
      newFIFO.explicitName = s"CommFIFO_${key.mem}_${key.src.s.get}_${key.dst.s.get}"
      commFIFOs(key) = newFIFO
      if (key.direction == Return) {
        newFIFO.fifoInits = Range(0, bufferDepth).map(I32(_))
      }
    }
  }

  private def getFIFO(key: TokenComm): FIFO[_] = {
    commFIFOs(key)
  }

  def isInternalMem(mem: Sym[_]): Boolean = {
    val allAccesses = mem.readers union mem.writers
    dbgs(s"Mem: $mem -- $allAccesses")
    if (allAccesses.isEmpty) { return true }
    val mutualLCA = LCA(allAccesses)
    dbgs(s"isInternalMem($mem):")
    indent {
      dbgs(s"Mutual LCA: $mutualLCA")
      dbgs(s"IsInner: ${mutualLCA.isInnerControl}")
      dbgs(s"HasStreamPrimitiveAncestor: ${mutualLCA.hasStreamPrimitiveAncestor}")
    }
    mutualLCA.isInnerControl || mutualLCA.hasStreamPrimitiveAncestor
  }

  type MemTokenEnableMap = Map[Sym[_], Seq[(TokenComm, Bit)]]
  /**
    * Maps each TokenComm for a controller to an enable signal
    * @param tokens: The control tokens in question
    * @param firstIterMap A map of each iterator to a bit indicating the first iteration
    * @return A Map from memory to the relevant TokenComms
    */

  private def computeIntakeEnables(tokens: Seq[TokenComm], firstIterMap: Map[Sym[_], Bit]): MemTokenEnableMap = {
    tokens.groupBy(_.mem).map {
      case (mem, comms) =>
        dbgs(s"Setting up input enables for $mem")
        val commToEn = indent { comms.map {
          case comm@TokenComm(mem, _, dst, edgeType, lca, _) =>
            dbgs(comm)
            val dstPath = dst.ancestors(lca)
            dbgs(s"DstPath: $dstPath")
            dbgs(s"First Iter Map: $firstIterMap")
            val isFirstIterInLCA = getOutermostIter(dstPath.drop(1)).map(firstIterMap(_)).getOrElse(Bit(true))
            val en = edgeType match {
              case Forward | Initialize(_) | Return =>
                // For forward edges, we care if we're on the first iteration WITHIN the LCA
                // This also works for Return edges because they're initialized with data
                isFirstIterInLCA
              case Backward =>
                // For backward edges, we care if we're NOT on the first iteration OF the LCA (since there won't be data yet)
                val isFirstIterOfLCA = getOutermostIter(lca.ancestors(mem.parent).reverse).map(firstIterMap(_)).getOrElse(Bit(true))
                isFirstIterInLCA & !isFirstIterOfLCA
            }
            comm -> en
        }}
        mem -> commToEn
    }
  }

  private def computeOutputEnablesForCtrl(ctrl: Sym[_], lastIterMap: Map[Sym[_], Bit]): MemTokenEnableMap = {
    val intakes = tokenComms.filter(_.src == ctrl.toCtrl)
    intakes.groupBy(_.mem).map {
      case (mem, comms) =>
        dbgs(s"Setting up output enables for $mem")
        val commToEn = indent { comms.map {
          case comm@TokenComm(mem, _, _, edgeType, lca, _) =>
            dbgs(comm)
            val writerPath = ctrl.ancestors(lca)
            val isLastIterWithinLCA = getOutermostIter(writerPath.drop(1)).map(lastIterMap(_)).getOrElse(Bit(true))
            val en = edgeType match {
              case Forward | Return =>
                // We also send the token back for Return so that it's available for the next iteration.
                isLastIterWithinLCA
              case Backward =>
                // we send the value backward if it isn't the last iteration of the LCA
                val isLastIterOfLCA = getOutermostIter(lca.ancestors(mem.parent).reverse).map(lastIterMap(_)).getOrElse(Bit(true))
                isLastIterWithinLCA & !isLastIterOfLCA
            }
            comm -> en
        }}
        mem -> commToEn
    }
  }

  private def createIntakeReads[T: Bits](hold: Reg[T], tokenWithEns: Seq[(TokenComm, Bit)]): Bit = {
    val shouldWrite = tokenWithEns.map(_._2).reduceTree(_|_)
    val dequeuedValues = tokenWithEns.map {
      case (comm@TokenComm(_, _, _, Initialize(v), _, _), en) =>
        v.asInstanceOf[T]
      case (comm, en) =>
        getFIFO(comm).deq(en).asInstanceOf[T]
    }
    val writeValue = oneHotMux(tokenWithEns.map(_._2), dequeuedValues)
    hold.write(writeValue, shouldWrite)

    shouldWrite
  }

  private def handleIntakes(tokenEnables: MemTokenEnableMap): Map[Sym[_], Sym[_]] = {
    tokenEnables.collect {
      case (r: Reg[_], tokenWithEns) =>
        implicit def bEV: Bits[r.RT] = r.A
        // We have a bunch of sources for the reg. For this, first we stage into a holding register
        val holdReg = mirrorSym(r).unbox
        val shouldWrite = createIntakeReads(holdReg, tokenWithEns)
        r -> holdReg.value.asInstanceOf[Bits[r.RT]].asSym
      case (mem, tokenWithEns) if MemStrategy(mem) == Buffer =>
        val holdReg = Reg[I32]
        val shouldWrite = createIntakeReads(holdReg, tokenWithEns)
        mem -> holdReg.value
    }.toMap[Sym[_], Sym[_]]
  }

  /**
    * Computes the local dependencies of a set of symbols
    * @param syms Set of symbols
    * @param current Set of symbols already visited
    * @return Set of root dependencies, set of processed nodes
    */
  private def computeLocalDeps(sym: Sym[_]): Set[Sym[_]] = {
    val result = cm.Set[Sym[_]]()
    val stack = cm.Stack[Sym[_]](sym)
    while (stack.nonEmpty) {
      val cur = stack.pop()
      if (!result.contains(cur)) {
        result.add(cur)
        stack.pushAll(cur.inputs.filterNot(result.contains))
      }
    }
    result.toSet
  }

  private def commsToVecStructType(comms: Seq[TokenComm]): VecStructType[Sym[_]] = {
    spatial.util.VecStructType(comms.map(_.mem).distinct.sortBy(_.progorder) map {
      case mem if mem.isReg =>
        (mem, mem.asMem.A)
      case mem if MemStrategy(mem) == Buffer =>
        (mem, I32(0))
      case mem if MemStrategy(mem) == Arbitrate =>
        (mem, Bit(false))
    })
  }

  case class ControllerInfo(lhs: Sym[_]) {
    dbgs(s"Ancestors of $lhs = ${lhs.ancestors} [${lhs.isStreamPrimitive}]")
    val allCtrlers = lhs.ancestors.dropRight(if (lhs.isStreamPrimitive) 1 else 0)
    val allChains = allCtrlers.flatMap(_.cchains)
    val allCounters = allChains.flatMap(_.counters)
    val allIters = allCounters.flatMap(_.iter)

    val fifoDepth = I32(128)
    val iterFIFO = {
      implicit def vecBitsEV: Bits[Vec[PseudoIter]] = Vec.bits[PseudoIter](allCounters.size)
      val iterFIFO: FIFO[PseudoIters[Vec[PseudoIter]]] = FIFO[PseudoIters[Vec[PseudoIter]]](fifoDepth)
      iterFIFO.explicitName = s"IterFIFO_$lhs"

      iterFIFO
    }

    val lastIterType = VecStructType(allChains.map(_.counters.head).map {ctr => ctr.iter.get -> Bit(false)})
    val releaseLastFIFO = {
      implicit def vecBitsEV = lastIterType.bitsEV
      val releaseLastFIFO = FIFO[Vec[Bit]](fifoDepth)
      releaseLastFIFO.explicitName = s"ReleaseLastFIFO_$lhs"
      releaseLastFIFO
    }

    val intakeComms = tokenComms.filter(_.dst == lhs.toCtrl)
    val releaseComms = tokenComms.filter(_.src == lhs.toCtrl)

    val tokenStreamType = commsToVecStructType(intakeComms)
    val tokenEnableType = VecStructType(intakeComms.map(_.mem).distinct.map(mem => (mem, Bit(false))))

    assert(tokenStreamType.isEmpty == tokenEnableType.isEmpty, s"Mismatch between $tokenStreamType and its enabler $tokenEnableType")

    dbgs(s"Token Enable Type: $tokenEnableType")
    val (tokenFIFO, finishTokenFIFO, bypassTokenFIFO) = if (!tokenStreamType.isEmpty) {
      implicit def bEV: Bits[Vec[Bit]] = tokenStreamType.bitsEV
      val tokenFIFO = FIFO[Vec[Bit]](fifoDepth)
      tokenFIFO.explicitName = s"TokenFIFO_$lhs"
      val finishTokenFIFO = FIFO[Vec[Bit]](fifoDepth)
      finishTokenFIFO.explicitName = s"FinishTokenFIFO_$lhs"
      val bypassTokenFIFO = FIFO[Vec[Bit]](fifoDepth)
      bypassTokenFIFO.explicitName = s"BypassTokenFIFO_$lhs"
      (Some(tokenFIFO), Some(finishTokenFIFO), Some(bypassTokenFIFO))
    } else { (None, None, None) }

    val acquireFIFO = if (!tokenEnableType.isEmpty) {
      implicit def bEV: Bits[Vec[Bit]] = tokenEnableType.bitsEV
      val acquireFIFO = FIFO[Vec[Bit]](fifoDepth)
      acquireFIFO.explicitName = s"AcquireFIFO_$lhs"
      Some(acquireFIFO)
    } else None

    val tokenSourceFIFO = FIFO[Bit](fifoDepth)
    tokenSourceFIFO.explicitName = s"TokenSourceFIFO_$lhs"
  }

  private def createCounterGenerator(controllerInfo: ControllerInfo): Sym[_] = {
    import controllerInfo._

    // Maps from memory to either their token or their value (for duplicates)
    val tokenMap = cm.Map[Sym[_], Bits[_]]()

    var remainingComms = controllerInfo.intakeComms.toSet

    val backlog = cm.Buffer[Sym[_]]()
    val firstIterMap = cm.Map[Sym[_], Bit]()
    val lastIterMap = cm.Map[Sym[_], Bit]()

    def updateIterInfoMaps(): Unit = {
      if (backlog.isEmpty) {
        dbgs(s"Skipping Update, Backlog was empty")
        return
      }
      dbgs(s"Updating First Iter Map: $firstIterMap")
      // takes the current firstIterMap and fills in all missing entries
      // backlog is outermost to innermost, all inner compared to entries firstIterMap
      val isFirsts = isFirstIters(f(backlog.map(_.unbox.asInstanceOf[I32])): _*)
      firstIterMap.keys.foreach {
        iter => firstIterMap(iter) &= isFirsts.head
      }
      firstIterMap ++= (backlog zip isFirsts).toMap
      dbgs(s"Updated First Iter Map: $firstIterMap")

      val isLasts = isLastIters(f(backlog.map(_.unbox.asInstanceOf[I32])):_*)
      lastIterMap.keys.foreach {
        iter => lastIterMap(iter) &= isLasts.head
      }
      lastIterMap ++= (backlog zip isLasts).toMap
      backlog.clear()
      dbgs(s"Updated Last Iter Map: $lastIterMap")
    }

    def updateTokenMap(comms: Seq[TokenComm]): Unit = {
      val enables = computeIntakeEnables(comms, firstIterMap.toMap.withDefaultValue(Bit(true)))
      val regMap = handleIntakes(enables)
      regMap.foreach {
        case (mem, value) =>
          tokenMap(mem) = tokenMap.get(mem) match {
            case Some(old: Bits[_]) =>

              dbgs(s"Overwriting: $mem = $old with $value")

              implicit def bitsEV: Bits[old.R] = old

              val en = enables(mem)
              mux(en.map(_._2).reduceTree {
                _ | _
              }, value.asInstanceOf[old.R], old.asInstanceOf[old.R]).asInstanceOf[Bits[old.R]]
            case None => value.asInstanceOf[Bits[_]]
          }
      }
    }

    def recurseHelper(chains: List[CounterChain]): Sym[_] = {
      dbgs(s"All token comms: $remainingComms")
      dbgs(s"Peeling chain: ${chains.head}")
      val headChain = chains.head
      val currentCtrl = headChain.parent
      val dependencies = computeLocalDeps(headChain).filter(_.isMem)
      dbgs(s"Dependencies: $dependencies")
      val curIters = headChain.counters.flatMap(_.iter)

      if (dependencies.nonEmpty) {
        updateIterInfoMaps()
        val relevantComms = remainingComms.filter {
          case TokenComm(mem, _, _, _, lca, _) =>
            dependencies.contains(mem) && currentCtrl.hasAncestor(lca)
        }
        remainingComms --= relevantComms

        updateTokenMap(relevantComms.toSeq)
      }

      // Mirror all of the relevant inputs for the chain
      {
        val stack = cm.Stack[Sym[_]](headChain.counters.flatMap(_.inputs): _*)
        dbgs(s"Processing inputs for counters ${headChain.counters}: $stack")
        while (stack.nonEmpty) {
          val cur = stack.pop()
          cur match {
            // If it's a register read, then we grab the register's value
            case rr@Op(op:RegRead[_]) if tokenMap.contains(rr.readMem.get) =>
              dbgs(s"Remapping $cur = $op to ${tokenMap(rr.readMem.get)}")
              register(rr -> tokenMap(rr.readMem.get))
            case _ if subst.contains(cur) => f(cur)
            case _ if cur.inputs.forall(subst.contains) =>
              dbgs(s"Forwarding $cur since all inputs are available")
              register(cur -> mirrorSym(cur))
            case _ =>
              // not all of the inputs have been processed, push all the inputs that haven't been processed
              dbgs(s"Re-pushing input: $cur -> ${cur.inputs.filterNot(subst.contains)}")
              stack.push(cur)
              stack.pushAll(cur.inputs.filterNot(subst.contains))
          }
        }
      }

      val computeChains = () => chains match {
        case cchain::Nil =>
          // innermost iteration

          var shouldFullyPar = false
          var totalPar = I32(1)
          val newChains = cchain.counters.reverse.map {
            ctr =>
              if (shouldFullyPar && ctr.isStatic) {
                type CT = ctr.CT

                implicit def numEV: Num[CT] = ctr.CTeV.asInstanceOf[Num[CT]]

                val castedCtr = ctr.asInstanceOf[Counter[CT]]
                val parFactor = castedCtr.end.unbox.to[I32] - castedCtr.start.unbox.to[I32]

                totalPar *= parFactor

                stage(CounterNew(castedCtr.start.unbox, castedCtr.end.unbox, castedCtr.step.unbox, parFactor))
              } else {
                shouldFullyPar = false
                mirrorSym(ctr).unbox
              }
          }.reverse
          val newCChain = CounterChain(newChains)
          val newIters = makeIters(newChains)
          backlog.appendAll(curIters)

          register(cchain, newCChain)
          register(cchain.counters, newChains)
          register(curIters, newChains.flatMap(_.iter))
          (newCChain, newIters)
        case cchain::_ =>
          val newChains = cchain.counters.map(mirrorSym(_).unbox)
          val newCChain = CounterChain(newChains)
          val newIters = makeIters(newChains)
          backlog.appendAll(curIters)

          register(curIters, newChains.flatMap(_.iter))
          register(cchain, newCChain)
          register(cchain.counters, newChains)
          register(curIters, newChains.flatMap(_.iter))
          (newCChain, newIters)
      }


      // Now that we've fetched everything, we need to mirror the chain and all of its predecessors.
      val stageBody = (newCChain: CounterChain, newIters: Seq[I32]) => chains match {
        case cchain :: Nil =>
          stageWithFlow(OpForeach(Set.empty, newCChain, stageBlock {
            updateIterInfoMaps()
            val oldItersAsI32 = allIters.map(_.unbox.asInstanceOf[I32])
            val allNewIters = f(oldItersAsI32)
            val isLasts = isLastIters(allNewIters: _*)
            val indexData = oldItersAsI32.zip(isLasts).map {
              case (iter, last) => PseudoIter(f(iter), firstIterMap(iter), last)
            }
            val pIters = {
              implicit def bEV: Bits[Vec[PseudoIter]] = Vec.bits[PseudoIter](indexData.size)
              PseudoIters(Vec.fromSeq(indexData))
            }

            updateTokenMap(remainingComms.toSeq)
            // TODO: Compute bypass for when the controller is inactive (0 iteration ctrls, if/else, etc.)
            val isActive = Bit(true)

            controllerInfo.iterFIFO.enq(pIters, isActive)
            val lastMap = lastIterType.fields.map {
              case (iter, _) => iter -> lastIterMap(iter.unbox.asInstanceOf[I32])
            }
            controllerInfo.releaseLastFIFO.enq(lastIterType.packStruct(lastMap.toMap))
            dbgs(s"Token Map: $tokenMap")
            dbgs(s"Relevant Comms: ${controllerInfo.intakeComms}")
            val allTokenAcquireEnables = computeIntakeEnables(controllerInfo.intakeComms, firstIterMap.toMap.withDefaultValue(Bit(true))).mapValues {
              commsAndBits => commsAndBits.map(_._2).reduceTree(_ || _)
            }
            dbgs(s"All Token Acquires: $allTokenAcquireEnables")
            val encoded = controllerInfo.tokenStreamType.packStruct(tokenMap.toMap)
            (tokenFIFO, acquireFIFO) match {
              case (Some(tf), Some(af)) =>
                tf.enq(encoded, isActive)
                af.enq(controllerInfo.tokenEnableType.packStruct(allTokenAcquireEnables), isActive)
              case (None, None) => dbgs(s"Skipping CounterGen Tokens -- no tokens found")
            }

            tokenSourceFIFO.enq(isActive)
          }, newIters, None)) {
            lhs2 =>
              lhs2.explicitName = s"CounterGen_${cchain.owner}"
              lhs2.ctx = augmentCtx(cchain.ctx)
              lhs2.userSchedule = Pipelined
          }

        case cchain :: rest =>
          stageWithFlow(OpForeach(Set.empty, newCChain, stageBlock {
            indent {
              recurseHelper(rest)
            }
            void
          }, newIters, None)) {
            lhs2 =>
              lhs2.explicitName = s"CounterGen_${cchain}"
              lhs2.ctx = augmentCtx(cchain.ctx)
              lhs2.userSchedule = Sequenced
          }
      }

      // check if we can run for 0 iters.

      val canRunZeroIters = !headChain.isStatic
      if (canRunZeroIters) {
        dbgs(s"$headChain can run for zero iterations! Staging an if/then/else to bypass.")
        val willRun = headChain.counters.filterNot(_.isForever).filterNot(_.isStatic).map({
          counter =>
            val castedCounter = counter.asInstanceOf[Counter[counter.CT]]

            implicit def numImpl: Num[counter.CT] = counter.CTeV.asInstanceOf[Num[counter.CT]]

            f(castedCounter.start).asInstanceOf[Num[counter.CT]] < f(castedCounter.end).unbox.asInstanceOf[counter.CT]
        }).reduceTree(_ & _)
        val iterInfo = (firstIterMap.clone(), lastIterMap.clone())
        val comms = remainingComms
        val tMap = tokenMap.clone()
        val oldBacklog = backlog.clone()
        stage(IfThenElse(willRun, stageBlock {
          dbgs(s"Through path:")
          indent {
            val (newCChain, newIters) = computeChains()
            stageBody(newCChain, newIters.map(_.unbox.asInstanceOf[I32]))
          }
        }, stageBlock {
          dbgs(s"Bypass Path")
          indent {
            firstIterMap.clear()
            firstIterMap ++= iterInfo._1
            lastIterMap.clear()
            lastIterMap ++= iterInfo._2
            backlog.clear()
            backlog ++= oldBacklog
            updateIterInfoMaps()
            // Otherwise, we note that all of the remaining iters are on their last iter, to
            // immediately release the token.
            tokenMap.clear()
            tokenMap ++= tMap
            updateTokenMap(comms.toSeq)
            // TODO: Compute bypass for when the controller is inactive (0 iteration ctrls, if/else, etc.)

            dbgs(s"Last Iter Map: $lastIterMap")
            val lastMap = lastIterType.fields.map {
              case (iter, _) => iter -> lastIterMap.getOrElse(iter.unbox.asInstanceOf[I32], Bit(true))
            }
            releaseLastFIFO.enq(lastIterType.packStruct(lastMap.toMap))
            dbgs(s"Token Map: $tokenMap")
            dbgs(s"Relevant Comms: ${controllerInfo.intakeComms}")
            val allTokenAcquireEnables = computeIntakeEnables(controllerInfo.intakeComms, firstIterMap.toMap.withDefaultValue(Bit(true))).mapValues {
              commsAndBits => commsAndBits.map(_._2).reduceTree(_ || _)
            }
            dbgs(s"All Token Acquires: $allTokenAcquireEnables")
            bypassTokenFIFO match {
              case Some(btf) =>
                val encoded = controllerInfo.tokenStreamType.packStruct(tokenMap.toMap)
                btf.enq(encoded)
              case _ => dbgs(s"Skipping CounterGen Tokens -- no tokens found")
            }

            tokenSourceFIFO.enq(Bit(false))
          }
        }))
      } else {
        val (newCChain, newIters) = computeChains()
        stageBody(newCChain, newIters.map(_.unbox.asInstanceOf[I32]))
      }
    }
    isolateSubst() { recurseHelper(allChains.toList) }
  }

  private def createReleaseController(controllerInfo: ControllerInfo, killer: Reg[Bit]): Sym[_] = {
    import controllerInfo._
    val forever = stage(ForeverNew())

    stageWithFlow(OpForeach(Set.empty, CounterChain(Seq(forever)), stageBlock {
      val lastIterMap = lastIterType.unpackStruct(releaseLastFIFO.deq()).mapValues(_.as[Bit])

      val source = tokenSourceFIFO.deq()
      val tokens = (finishTokenFIFO, bypassTokenFIFO) match {
        case (Some(ftf), Some(btf)) =>
          val finishUnpacked = tokenStreamType.unpackStruct(ftf.deq(source))
          val bypassUnpacked = tokenStreamType.unpackStruct(btf.deq(!source))
          finishUnpacked.map {
            case (k, v) =>
              val v2 = bypassUnpacked(k)
              k -> mux(source, v.asInstanceOf[Bits[v.R]], v2.asInstanceOf[Bits[v.R]])
          }
        case (None, None) => Map.empty[Sym[_], Bits[_]]
      }

      val ensAndComms = computeOutputEnablesForCtrl(lhs, lastIterMap.withDefaultValue(Bit(true)))
      ensAndComms foreach {
        case (mem, commsWithEns) =>
          val value = tokens(mem)
          commsWithEns.foreach {
            case (comm, en) =>
              val fifo = getFIFO(comm)
              val castedFIFO: FIFO[fifo.A.R] = fifo.asInstanceOf[FIFO[fifo.A.R]]
              castedFIFO.enq(value.asInstanceOf[fifo.A.R], en)
          }
      }

      retimeGate()
      val endOfWorld = getOutermostIter(controllerInfo.allCtrlers).map(lastIterMap(_)).getOrElse(Bit(true))
      killer.write(endOfWorld, endOfWorld)

    }, Seq(makeIter(forever).unbox), None)) {
      lhs2 => lhs2.userSchedule = Pipelined
        lhs2.ctx = augmentCtx(lhs.ctx)
    }
  }

  private def computeBufferDepth(mem: Sym[_]): Int = {
    // we need 2 copies in each user at the level of the mem.
    2 * mem.parent.children.count(ctrl => (ctrl.sym.effects.reads ++ ctrl.sym.effects.writes).contains(mem))
  }

  private var accessIterSize: Int = -1

  val intakeTokens = cm.Map[Sym[_], Sym[_]]()
  private def visitInnerForeach(lhs: Sym[_], foreachOp: OpForeach, killer: Reg[Bit]): Sym[_] = {
    dbgs(s"Visiting Foreach: $lhs = $foreachOp [${lhs.isInnerControl}]")
    val controllerInfo = ControllerInfo(lhs)

    dbgs(s"Creating Counter Generator and fetching tokens")
    val counterGen = indent {
      isolateSubst() { createCounterGenerator(controllerInfo) }
    }


    /**
      * What we have here looks like this:
      * s s s s s s s s s s
      * - - - - - - - - i i
      *
      * Where there's a (possible) overlap between streamIters and newInnerIters
      *
      */

    // restage every register used by the controller but not defined within -- we'll maintain a private copy.
    val incomingRegisters = controllerInfo.tokenStreamType.fields.map(_._1)
    incomingRegisters.foreach {
      case reg: Reg[_] =>
        dbgs(s"Mirroring: $reg")
        val mirrored = mirrorSym(reg)
        mirrored.ctx = implicitly[argon.SrcCtx].copy(previous = Seq(reg.ctx))
        mirrored.explicitName = s"${reg.explicitName.getOrElse(reg.toString)}_cloned_$lhs"
        register(reg -> mirrored)
      case _ =>
    }

    val staticCounters = controllerInfo.allCounters.reverse.takeWhile(_.isStatic).reverse
    val hasDynamicCounters = controllerInfo.allCounters.exists(!_.isStatic)
    dbgs(s"Static Counters: $staticCounters")
    val newStaticCounters = staticCounters.map(mirrorSym(_))
    register(staticCounters, newStaticCounters)
    val ctrs = {
      val newCounters = (if (hasDynamicCounters) Seq(stage(ForeverNew())) else Seq.empty) ++ newStaticCounters
      newCounters.map(_.unbox)
    }

    val newCounterChain = CounterChain(ctrs)
    register(foreachOp.cchain, newCounterChain)

    val newIters = makeIters(ctrs)
    // Split here based on if we're an inner or outer control
    accessIterSize = staticCounters.size

    val mainCtrl = if (lhs.isInnerControl) {
      stageWithFlow(OpForeach(Set.empty, CounterChain(ctrs), stageBlock {
        val streamIters = controllerInfo.iterFIFO.deq().iters

        controllerInfo.allCounters.zipWithIndex foreach {
          case (oldCtr, i) =>
            val oldIter = oldCtr.iter.get
            if (staticCounters contains oldCtr) {
              dbgs(s"Registering $oldCtr ($oldIter) -> ${f(oldCtr)}(${f(oldCtr).iter.get})")
              val newIter = f(oldCtr).iter.get
              register(oldIter, newIter)
            } else {
              dbgs(s"Registering $oldCtr ($oldIter) -> Stream")
              register(oldIter, streamIters(i).i)
            }
        }

        // Maps registers to their 'latest' value
        val regValues = cm.Map[Sym[_], Sym[_]]()

        // Reads tokens and their validity from tokens, wrEns

        val (tokens, wrEns) = (controllerInfo.tokenFIFO, controllerInfo.acquireFIFO) match {
          case (Some(tokenFIFO), Some(acquireFIFO)) =>
            val tk = controllerInfo.tokenStreamType.unpackStruct(tokenFIFO.deq())
            val wEns = controllerInfo.tokenEnableType.unpackStruct(acquireFIFO.deq()).mapValues(_.as[Bit])
            (tk, wEns)
          case (None, None) =>
            (Map.empty, Map.empty[Sym[_], Bit])
        }
        tokens.foreach {
          case (mem: Reg[_], value) =>
            dbgs(s"Handling Register: $mem -> ${f(mem)}")

            implicit def bEV: Bits[mem.RT] = mem.A.asInstanceOf[Bits[mem.RT]]

            val holdReg = Reg[mem.RT]
            holdReg.explicitName = s"HoldReg_${lhs}_$mem"
            holdReg.nonbuffer
            holdReg.write(value.asInstanceOf[mem.RT], wrEns(mem))

            if (!lhs.isInnerControl) {
              // write the result of the regvalue to the reg
              f(mem).write(holdReg.value, wrEns(mem))
              regValues(mem) = void
            } else {
              val writesToReg = lhs.writtenMems.contains(mem)
              if (writesToReg) {
                // In this case, we want it to grab the value from the previous write
                val oldValue = f(mem).value.asInstanceOf[Bits[mem.RT]]
                val oldRead = oldValue.asSym
                dbgs(s"Registering read for $mem: $oldRead with enable ${wrEns(mem)}")
                regValues(mem) = mux(wrEns(mem), holdReg.value.asInstanceOf[Bits[mem.RT]], oldValue).asInstanceOf[Sym[_]]
              } else {
                regValues(mem) = holdReg.value
              }
            }

          case (mem, value) if MemStrategy(mem) == Buffer =>
            implicit def bEV: Bits[value.R] = value.asSym.tp.asInstanceOf[Bits[value.R]]

            val holdReg = Reg[value.R]
            holdReg.explicitName = s"HoldReg_${lhs}_$mem"
            holdReg.nonbuffer
            holdReg.write(value.asInstanceOf[value.R], wrEns(mem))
            intakeTokens(mem) = holdReg
            dbgs(s"Registering Intake Token for $mem = ${intakeTokens(mem)}")
        }

        // Restage the actual innards of the foreach
          foreachOp.block.stms.foreach {
            case rr@Op(RegRead(reg)) if regValues.contains(reg) =>
              dbgs(s"Forwarding read: $rr = ${rr.op.get} <= ${regValues(reg)} =  ${regValues(reg).op}")
              register(rr -> regValues(reg))
            case rw@Op(RegWrite(reg, data, ens)) if regValues.contains(reg) =>
              dbgs(s"Forwarding write: $rw = ${rw.op.get}")
              val alwaysEnabled = ens.forall {
                case Const(b) => b.value
                case _ => false
              }
              if (alwaysEnabled) {
                regValues(reg) = f(data)
              } else {
                regValues(reg) = mux(f(ens).toSeq.reduceTree(_ & _), f(data).asInstanceOf[Bits[reg.RT]], regValues(reg).asInstanceOf[Bits[reg.RT]]).asInstanceOf[Sym[reg.RT]]
              }
              dbgs(s"reg($reg) = ${regValues(reg)}")

              // stage the write anyways
              visit(rw)
            case other =>
              dbgs(s"Default Visiting: $other = ${other.op} inside of Inner Control")
              visit(other)
          }

        retimeGate()
        // Release tokens
        controllerInfo.finishTokenFIFO match {
          case Some(finishTokenFIFO) =>
            val updatedValues = intakeTokens.mapValues(_.asInstanceOf[Reg[_]].value) ++ regValues
            finishTokenFIFO.enq(controllerInfo.tokenStreamType.packStruct(updatedValues.toMap.mapValues(_.asInstanceOf[Bits[_]])))
          case None =>
            dbgs(s"Skipping Finish Token FIFO staging -- no tokens found!")
        }
      }, newIters.map(_.unbox.asInstanceOf[I32]), None)) {
        newForeach =>
          transferData(lhs, newForeach)
          newForeach.ctx = augmentCtx(lhs.ctx)
          dbgs(s"Forwarding schedule: $lhs => ${lhs.getRawSchedule}")
          newForeach.userSchedule = lhs.getRawSchedule.getOrElse(Pipelined)
      }
    } else {
      // Outer Control / StreamPrimitive case
      stageWithFlow(OpForeach(Set.empty, CounterChain(ctrs), stageBlock {
        val streamIters = controllerInfo.iterFIFO.deq().iters
        controllerInfo.allCounters.zipWithIndex foreach {
          case (oldCtr, i) =>
            val oldIter = oldCtr.iter.get
            if (staticCounters contains oldCtr) {
              dbgs(s"Registering $oldCtr ($oldIter) -> ${f(oldCtr)}(${f(oldCtr).iter.get})")
              val newIter = f(oldCtr).iter.get
              register(oldIter, newIter)
            } else {
              dbgs(s"Registering $oldCtr ($oldIter) -> Stream")
              register(oldIter, streamIters(i).i)
            }
        }

        // Reads tokens and their validity from tokens, wrEns

        val (tokens, wrEns) = (controllerInfo.tokenFIFO, controllerInfo.acquireFIFO) match {
          case (Some(tokenFIFO), Some(acquireFIFO)) =>
            val tk = controllerInfo.tokenStreamType.unpackStruct(tokenFIFO.deq())
            val wEns = controllerInfo.tokenEnableType.unpackStruct(acquireFIFO.deq()).mapValues(_.as[Bit])
            (tk, wEns)
          case (None, None) =>
            (Map.empty, Map.empty[Sym[_], Bit])
        }
        tokens.foreach {
          case (mem: Reg[_], value) =>
            dbgs(s"Handling Register: $mem -> ${f(mem)}")

            implicit def bEV: Bits[mem.RT] = mem.A.asInstanceOf[Bits[mem.RT]]

            val holdReg = Reg[mem.RT]
            holdReg.explicitName = s"HoldReg_${lhs}_$mem"
            holdReg.nonbuffer
            holdReg.write(value.asInstanceOf[mem.RT], wrEns(mem))

            // write the result of the regvalue to the reg
            f(mem).write(holdReg.value, wrEns(mem))

          case (mem, value) if MemStrategy(mem) == Buffer =>
            implicit def bEV: Bits[value.R] = value.asSym.tp.asInstanceOf[Bits[value.R]]

            val holdReg = Reg[value.R]
            holdReg.explicitName = s"HoldReg_${lhs}_$mem"
            holdReg.nonbuffer
            holdReg.write(value.asInstanceOf[value.R], wrEns(mem))
            val bufferReg = Reg[value.R]
            bufferReg.explicitName = s"BufferedReg_${lhs}_$mem"
            bufferReg := holdReg.value
            intakeTokens(mem) = bufferReg
            dbgs(s"Registering Intake Token for $mem = $bufferReg.value")
        }

        val ctrs = lhs.cchains.flatMap(_.counters)
        register(ctrs, ctrs.map(mirrorSym(_)))
        register(ctrs.flatMap(_.iter), makeIters(f(ctrs)))
        register(lhs.cchains, lhs.cchains.map(mirrorSym(_)))
//        mirrorSym(lhs)
        super.transform[Void](lhs.asInstanceOf[Sym[Void]], lhs.op.get.asInstanceOf[Op[Void]])
        // Release tokens
        Pipe {
          controllerInfo.finishTokenFIFO match {
            case Some(finishTokenFIFO) =>
              val updatedValues = intakeTokens.mapValues(_.asInstanceOf[Reg[_]].value) ++ tokens.collect {
                case (mem: Reg[_], _) => mem -> f(mem).value
              }
              finishTokenFIFO.enq(controllerInfo.tokenStreamType.packStruct(updatedValues.toMap.mapValues(_.asInstanceOf[Bits[_]])))
            case None =>
              dbgs(s"Skipping Finish Token FIFO staging -- no tokens found!")
          }
        }
      }, newIters.map(_.unbox.asInstanceOf[I32]), None)) {
        newForeach =>
          transferData(lhs, newForeach)
          newForeach.ctx = augmentCtx(lhs.ctx)
          newForeach.userSchedule = Pipelined
      }
    }

    accessIterSize = -1

    dbgs(s"Creating Release Controller")
    val finisher = indent {
      isolateSubst() { createReleaseController(controllerInfo, killer) }
    }

    intakeTokens.clear()
    innerControllerMap(lhs) = ControllerImpl(counterGen, mainCtrl, finisher)
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case accelScope: AccelScope => inAccel {
      printRegister = true

      dbgs(s"Computing Global Mem Graph")
      tokenComms = accelScope.block.nestedStms.filter(_.isMem).filterNot(isInternalMem).filterNot(_.isLUT).flatMap(computeProducerConsumers(_))
      dbgs(s"Comms:")
      indent {
        tokenComms.foreach(dbgs(_))
      }
      dbgs(s"="*100)
      tokenComms.dumpToFile(state.config.logDir)

      stageWithFlow(AccelScope(stageBlock {
        accelHandle = IR.getCurrentHandle()
        tokenComms.foreach {
          case tk@TokenComm(r: Reg[_], _, _, _, _, _) =>
            implicit def bitsEV: Bits[r.RT] = r.A
            createFIFO[r.RT](tk)
          case tk if MemStrategy(tk.mem) == Buffer =>
            createFIFO[I32](tk)
        }
        Stream {
          accelScope.block.stms.foreach(visit)
        }
      })) {
        lhs2 => transferData(lhs, lhs2)
      }
    }

    case foreachOp: OpForeach if inHw && lhs.isStreamPrimitive || (lhs.isInnerControl && !lhs.toCtrl.hasStreamPrimitiveAncestor) =>
      dbgs(s"Transforming Foreach: $lhs = $foreachOp")
      indent { isolateSubst() {
        val killer = lhs.stopWhen match {
          case Some(F(kill: Reg[Bit])) =>
            kill.ignoreAllConflicts
            kill
          case None =>
            Reg[Bit](false)
        }
        killer.explicitName = s"${lhs}_killer"
        stageWithFlow(UnitPipe(Set.empty, stageBlock {
          visitInnerForeach(lhs, foreachOp, killer)
          spatial.lang.void
        }, Some(killer))) {
          lhs2 =>
            transferDataIfNew(lhs, lhs2)
            lhs2.userSchedule = Streaming
        }
      } }

    case ctrlOp: Control[_] if inHw && lhs.isOuterControl && !lhs.toCtrl.hasStreamPrimitiveAncestor =>
      dbgs(s"Skipping Control: $lhs = $ctrlOp")
      stageWithFlow(UnitPipe(Set.empty, stageBlock {
        ctrlOp.blocks.foreach(inlineBlock(_))
        spatial.lang.void
      }, None)) {
        lhs2 =>
          transferDataIfNew(lhs, lhs2)
          lhs2.userSchedule = Streaming
      }

    case mem if inHw && lhs.isMem && lhs.isReg && !isInternalMem(lhs) =>
      dbgs(s"Skipping $lhs = $mem since it'll be duplicated.")
      lhs

    case mem if inHw && lhs.isSRAM && !isInternalMem(lhs) =>
      expandMem(lhs)

    case _:CounterNew[_] | _:CounterChainNew if lhs.parent.isStreamPrimitive || (lhs.parent.isInnerControl && !lhs.toCtrl.hasStreamPrimitiveAncestor) =>
      dbgs(s"Skipping $lhs = $rhs since it'll be re-created later.")
      lhs

    case _: CounterNew[_] if inHw =>
      dbgs(s"Encountered Counter: $lhs")
      val transformed = super.transform(lhs, rhs)
      dbgs(s"Updating Counter info for $lhs = $rhs (${lhs.asInstanceOf[Counter[_]].iter})")
      val newIter = TransformUtils.makeIter(transformed.asInstanceOf[Counter[_]])
      val oldIter = lhs.asInstanceOf[Counter[_]].iter.get
      register(oldIter, newIter)
      transformed

    case srr@SRAMRead(mem, _, _) if intakeTokens.contains(mem) =>
      expandReader(lhs, srr, intakeTokens(mem).asInstanceOf[Reg[I32]].value)

    case srw@SRAMWrite(mem, _, _, _) if intakeTokens.contains(mem) =>
      expandWriter(lhs, srw, intakeTokens(mem).asInstanceOf[Reg[I32]].value)

    case _ =>
      super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]

  override def postprocess[R](block: Block[R]): Block[R] = {
    val result = super.postprocess(block)
    accelHandle = null
    dbgs("="*80)
    dbgs(s"commFIFOs:")
    indent {
      commFIFOs.foreach {
        case (comm, fifo) =>
          dbgs(s"$comm <=> $fifo (tokens: ${computeBufferDepth(comm.mem)})")
      }
    }
    dbgs("="*80)
    dbgs(s"Controller Mapping:")
    indent {
      innerControllerMap.toSeq.sortBy(_._1.progorder).foreach {
        case (old, ControllerImpl(counterGen, main, finisher)) =>
          dbgs(s"$old = ${old.op.get}")
          indent {
            dbgs(s"CounterGen: $counterGen")
            dbgs(s"Main: $main")
            dbgs(s"Finisher: $finisher")
          }
      }
    }
    result
  }

  override def preprocess[S](block: Block[S]): Block[S] = {
    commFIFOs.clear()
    innerControllerMap.clear()
    super.preprocess(block)
  }

  private def expandMem(mem: Sym[_]) = mem match {
    case Op(srn@SRAMNew(dims)) =>
      dbgs(s"Expanding SRAM: $mem = $srn")
      val bufferAmount = mem.bufferAmount.getOrElse(computeBufferDepth(mem))
      val newDims = Seq(I32(bufferAmount)) ++ dims
      type A = srn.A.R
      lazy implicit val bitsEV: Bits[A] = srn.A

      implicit def ctx: SrcCtx = mem.ctx

      val newMem = stageWithFlow(SRAMNew[A, SRAMN](newDims)) { nm => transferData(mem, nm) }
      newMem.r = dims.size + 1
      transferData(mem, newMem)
      // Shift fully banked dims back by 1, then fully bank this one.
      newMem.fullyBankDims = mem.fullyBankDims.map(_ + 1) + 0
      newMem.shouldIgnoreConflicts = mem.shouldIgnoreConflicts.map(_ + 1) + 0
      dbgs(s"Old Fully Banked Dims: ${mem.fullyBankDims}")
      dbgs(s"New Fully Banked Dims: ${newMem.fullyBankDims}")
      newMem.duplicates = mem.duplicates.map {
        case Memory(banking, depth, padding, accType) =>
          val newBank = ModBanking(bufferAmount, 1, Seq(1), Seq(0), Seq(bufferAmount))
          val oldBanks = banking.map {
            case modBank: ModBanking =>
              modBank.copy(axes = modBank.axes.map(_ + 1))
          }
          Memory(Seq(newBank) ++ oldBanks, 1, Seq(0) ++ padding, accType)
      }
      dbgs(s"Old Banking: ${mem.duplicates}")
      dbgs(s"New Banking: ${newMem.duplicates}")
      newMem.bufferAmount = None
      newMem.hierarchical
      newMem.nonbuffer
      newMem.freezeMem = true
      dbgs(s"NewMem: $newMem = ${newMem.op.get}")
      newMem
  }

  private def expandWriter(sym: Sym[_], writer: Writer[_], insertedDim: Idx) = {
    dbgs(s"Expanding Writer: $sym = $writer")
    writer.mem match {
      case sr: SRAM[_, _] =>
        type A = sr.A.R
        lazy implicit val bitsEV: Bits[A] = sr.A
        val dataAsBits = f(writer.data).asInstanceOf[Bits[A]]

        implicit def ctx: SrcCtx = sym.ctx

        val newWrite = stage(SRAMWrite(f(writer.mem).asInstanceOf[SRAM[A, SRAMN]], dataAsBits, Seq(insertedDim) ++ f(writer.addr), f(writer.ens)))
        copyMemoryMeta(newWrite, sym)
        newWrite
    }
  }

  private def expandReader(sym: Sym[_], reader: Reader[_, _], insertedDim: Idx) = {
    dbgs(s"Expanding Reader: $sym = $reader")
    reader.mem match {
      case sr: SRAM[_, _] =>
        type A = sr.A.R
        lazy implicit val bitsEV: Bits[A] = sr.A

        implicit def ctx: SrcCtx = sym.ctx

        val result = stage(SRAMRead(f(reader.mem).asInstanceOf[SRAM[A, SRAMN]], Seq(insertedDim) ++ f(reader.addr), f(reader.ens)))
        copyMemoryMeta(result.asSym, sym)
        result
    }
  }

  private def copyMemoryMeta(newSym: Sym[_], oldSym: Sym[_]): Unit = {
    def toNewUid(oldUid: Iterable[Int]): List[Int] = {
      // How many iters are there between us and the mem?
      // For an innerControl:
      //    accessIterSize has how many counters are staged into the inner foreach. Since everything else is a unitpipe
      //    The uid should be a list of length accessIterSize.
      // For a streamPrimitive:
      //    accessIterSize has the wrapper around the actual primitive
      //    so in this case, we also need to account for the iterators WITHIN the stream primitive
      assert(accessIterSize >= 0)
      if (oldSym.hasStreamPrimitiveAncestor) {
        // Figure out how many iters there are in the streamPrimitive path
        val primitiveIters = oldSym.scopes(scope => scope.s.exists(_.isStreamPrimitive)).filterNot(_.stage == -1).flatMap(_.iters).filter(!_.counter.ctr.isForever)
        List.fill(accessIterSize + primitiveIters.size - oldUid.size)(0) ++ oldUid
      } else {
        List.fill(accessIterSize - oldUid.size)(0) ++ oldUid
      }
    }

    newSym.dispatches = oldSym.dispatches.map {
      case (uid, disp) =>
        toNewUid(uid) -> disp
    }

    newSym.gids = oldSym.gids.map {
      case (uid, groups) =>
        toNewUid(uid) -> groups
    }

    oldSym.getPorts.getOrElse(Map.empty).foreach {
      case (disp, portData) =>
        portData.foreach {
          case (uid, port) =>
            newSym.addPort(disp, toNewUid(uid), port)
        }
    }
  }
}


