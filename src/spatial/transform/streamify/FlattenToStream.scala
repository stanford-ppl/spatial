package spatial.transform.streamify

import argon.transform.{ForwardTransformer, MutateTransformer}
import argon._
import argon.tags.struct
import spatial.lang._
import spatial.metadata.access
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
    val (iterFIFO, releaseIterFIFO) = {
      implicit def vecBitsEV: Bits[Vec[PseudoIter]] = Vec.bits[PseudoIter](allCounters.size)
      val iterFIFO: FIFO[PseudoIters[Vec[PseudoIter]]] = FIFO[PseudoIters[Vec[PseudoIter]]](fifoDepth)
      iterFIFO.explicitName = s"IterFIFO_$lhs"

      val releaseIterFIFO = mirrorSym(iterFIFO).unbox
      releaseIterFIFO.explicitName = s"ReleaseIterFIFO_$lhs"

      (iterFIFO, releaseIterFIFO)
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

    def recurseHelper(chains: List[CounterChain], backlog: cm.Buffer[Sym[_]], firstIterMap: cm.Map[Sym[_], Bit]): Sym[_] = {
      dbgs(s"All token comms: $remainingComms")
      dbgs(s"Peeling chain: ${chains.head}")
      val headChain = chains.head
      val currentCtrl = headChain.parent
      val dependencies = computeLocalDeps(headChain).filter(_.isMem)
      dbgs(s"Dependencies: $dependencies")
      val curIters = headChain.counters.flatMap(_.iter)

      def updateFirstIterMap(): Unit = {
        if (backlog.isEmpty) {
          dbgs(s"Skipping Update, Backlog was empty")
          return
        }
        dbgs(s"Updating First Iter Map: $firstIterMap")
        // takes the current firstIterMap and fills in all missing entries
        // backlog is outermost to innermost, all inner compared to entries firstIterMap
        val isFirsts = isFirstIters(f(backlog.map(_.unbox.asInstanceOf[I32])):_*)
        firstIterMap.keys.foreach {
          iter => firstIterMap(iter) &= isFirsts.head
        }
        firstIterMap ++= (backlog zip isFirsts).toMap
        dbgs(s"Updated First Iter Map: $firstIterMap")
        backlog.clear()
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

      if (dependencies.nonEmpty) {
        updateFirstIterMap()
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


      // Now that we've fetched everything, we need to mirror the chain and all of its predecessors.
      chains match {
        case cchain :: Nil =>
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
          register(curIters, newChains.flatMap(_.iter))

          stageWithFlow(OpForeach(Set.empty, newCChain, stageBlock {
            updateFirstIterMap()
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
            controllerInfo.releaseIterFIFO.enq(pIters)
            dbgs(s"Token Map: $tokenMap")
            dbgs(s"Relevant Comms: ${controllerInfo.intakeComms}")
            val allTokenAcquireEnables = computeIntakeEnables(controllerInfo.intakeComms, firstIterMap.toMap.withDefaultValue(Bit(true))).mapValues {
              commsAndBits => commsAndBits.map(_._2).reduceTree(_ || _)
            }
            dbgs(s"All Token Acquires: $allTokenAcquireEnables")
            val encoded = controllerInfo.tokenStreamType.packStruct(tokenMap.toMap)
            (tokenFIFO, bypassTokenFIFO, acquireFIFO) match {
              case (Some(tf), Some(btf), Some(af)) =>
                tf.enq(encoded, isActive)
                btf.enq(encoded, !isActive)
                af.enq(controllerInfo.tokenEnableType.packStruct(allTokenAcquireEnables), isActive)
              case (None, None, None) => dbgs(s"Skipping CounterGen Tokens -- no tokens found")
            }

            tokenSourceFIFO.enq(isActive)

//            // Update StopWhen -- on the last iteration, kill the controller.
//            // By stalling this out, we can guarantee that the preceding writes happen before the controller gets killed
//            retimeGate()
//            val endOfWorld = getOutermostIter(lhs.ancestors).map(lastIterMap(_)).getOrElse(Bit(true))
//            stopWhen.write(endOfWorld, endOfWorld)

          }, newIters.asInstanceOf[Seq[I32]], None)) {
            lhs2 =>
              lhs2.explicitName = s"CounterGen_${cchain.owner}"
              lhs2.ctx = augmentCtx(cchain.ctx)
              lhs2.userSchedule = Pipelined
          }

        case cchain :: rest =>
          // Fetch all dependencies of the chain

          val newChains = cchain.counters.map(mirrorSym(_).unbox)
          val newCChain = CounterChain(newChains)
          val newIters = makeIters(newChains)
          backlog.appendAll(curIters)
          register(curIters, newChains.flatMap(_.iter))

          stageWithFlow(OpForeach(Set.empty, newCChain, stageBlock {
            indent {
              recurseHelper(rest, backlog, firstIterMap)
            }
            void
          }, newIters.asInstanceOf[Seq[I32]], None)) {
            lhs2 =>
              lhs2.explicitName = s"CounterGen_${cchain}"
              lhs2.ctx = augmentCtx(cchain.ctx)
              lhs2.userSchedule = Pipelined
          }
      }
    }
    isolateSubst() { recurseHelper(allChains.toList, cm.ListBuffer.empty, cm.Map.empty) }
  }

  private def createReleaseController(controllerInfo: ControllerInfo): Sym[_] = {
    import controllerInfo._
    val stopWhen = Reg[Bit]
    val forever = stage(ForeverNew())

    stageWithFlow(OpForeach(Set.empty, CounterChain(Seq(forever)), stageBlock {
      val streamIters = releaseIterFIFO.deq().iters

      val allOldIters = controllerInfo.allCounters.flatMap(_.iter)
      register(allOldIters, streamIters.elems.map(_.i))
      val lastIterMap = allOldIters.zip(streamIters.elems.map(_.isLast)).toMap

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
      stopWhen.write(endOfWorld, endOfWorld)

    }, Seq(makeIter(forever).unbox), Some(stopWhen))) {
      lhs2 => lhs2.userSchedule = Pipelined
        lhs2.ctx = augmentCtx(lhs.ctx)
    }
  }

  private def computeBufferDepth(mem: Sym[_]): Int = {
    // we need 2 copies in each user at the level of the mem.
    2 * mem.parent.children.count(ctrl => (ctrl.sym.effects.reads ++ ctrl.sym.effects.writes).contains(mem))
  }

  val intakeTokens = cm.Map[Sym[_], Sym[_]]()
  private def visitInnerForeach(lhs: Sym[_], foreachOp: OpForeach): Sym[_] = {
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
    val stopWhen = foreachOp.stopWhen match {
      case Some(reg) => f(reg).conflictable
      case None => Reg[Bit](Bit(false)).dontTouch
    }

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

    val mainCtrl = if (lhs.isInnerControl) {
      stageWithFlow(OpForeach(Set.empty, CounterChain(ctrs), stageBlock {
        val streamIters = controllerInfo.iterFIFO.deq().iters
        val allOldIters = controllerInfo.allCounters.flatMap(_.iter)

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

        val lastIterMap = allOldIters.zip(streamIters.elems.map(_.isLast)).toMap

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

        intakeTokens.clear()
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
            intakeTokens(mem) = holdReg.value.asInstanceOf[Bits[value.R]]
            dbgs(s"Registering Intake Token for $mem")
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
        val updatedValues = intakeTokens ++ regValues
        controllerInfo.finishTokenFIFO match {
          case Some(finishTokenFIFO) =>
            finishTokenFIFO.enq(controllerInfo.tokenStreamType.packStruct(updatedValues.toMap.mapValues(_.asInstanceOf[Bits[_]])))
          case None =>
            dbgs(s"Skipping Finish Token FIFO staging -- no tokens found!")
        }

        // Update StopWhen -- on the last iteration, kill the controller.
        // By stalling this out, we can guarantee that the preceding writes happen before the controller gets killed
        retimeGate()
        val endOfWorld = getOutermostIter(lhs.ancestors).map(lastIterMap(_)).getOrElse(Bit(true))
        stopWhen.write(endOfWorld, endOfWorld)

      }, newIters.map(_.unbox.asInstanceOf[I32]), Some(stopWhen))) {
        newForeach =>
          transferData(lhs, newForeach)
          newForeach.ctx = augmentCtx(lhs.ctx)
          dbgs(s"Forwarding schedule: $lhs => ${lhs.getRawSchedule}")
          newForeach.userSchedule = lhs.getRawSchedule.getOrElse(Pipelined)
      }
    } else {
      stageWithFlow(OpForeach(Set.empty, CounterChain(ctrs), stageBlock {
        val streamIters = controllerInfo.iterFIFO.deq().iters
        val allOldIters = controllerInfo.allCounters.flatMap(_.iter)
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

        intakeTokens.clear()
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
            regValues(mem) = void

          case (mem, value) if MemStrategy(mem) == Buffer =>
            implicit def bEV: Bits[value.R] = value.asSym.tp.asInstanceOf[Bits[value.R]]

            val holdReg = Reg[value.R]
            holdReg.explicitName = s"HoldReg_${lhs}_$mem"
            holdReg.nonbuffer
            holdReg.write(value.asInstanceOf[value.R], wrEns(mem))
            intakeTokens(mem) = holdReg.value.asInstanceOf[Bits[value.R]]
            dbgs(s"Registering Intake Token for $mem")
        }

        val ctrs = lhs.cchains.flatMap(_.counters)
        register(ctrs, ctrs.map(mirrorSym(_)))
        register(ctrs.flatMap(_.iter), makeIters(f(ctrs)))
        register(lhs.cchains, lhs.cchains.map(mirrorSym(_)))
        mirrorSym(lhs)

        regValues ++= (regValues.keys.toSeq.map {
          case reg: Reg[_] => reg.asSym -> f(reg).value.asInstanceOf[Sym[_]]
        })

        retimeGate()
        // Release tokens
        val updatedValues = intakeTokens ++ regValues
        controllerInfo.finishTokenFIFO match {
          case Some(finishTokenFIFO) =>
            finishTokenFIFO.enq(controllerInfo.tokenStreamType.packStruct(updatedValues.toMap.mapValues(_.asInstanceOf[Bits[_]])))
          case None =>
            dbgs(s"Skipping Finish Token FIFO staging -- no tokens found!")
        }

        // Update StopWhen -- on the last iteration, kill the controller.
        // By stalling this out, we can guarantee that the preceding writes happen before the controller gets killed
        retimeGate()
        val lastIterMap = allOldIters.zip(streamIters.elems.map(_.isLast)).toMap
        val endOfWorld = getOutermostIter(controllerInfo.allCtrlers).map(lastIterMap(_)).getOrElse(Bit(true))
        stopWhen.write(endOfWorld, endOfWorld)
      }, newIters.map(_.unbox.asInstanceOf[I32]), Some(stopWhen))) {
        newForeach =>
          transferData(lhs, newForeach)
          newForeach.ctx = augmentCtx(lhs.ctx)
          newForeach.userSchedule = Pipelined
      }
//
//      stageWithFlow(OpForeach(Set.empty, CounterChain(ctrs), stageBlock {
//        val streamIters = controllerInfo.iterFIFO.deq().iters
//        val allOldIters = controllerInfo.allCounters.flatMap(_.iter)
//
//        controllerInfo.allCounters.zipWithIndex foreach {
//          case (oldCtr, i) =>
//            val oldIter = oldCtr.iter.get
//            if (staticCounters contains oldCtr) {
//              dbgs(s"Registering $oldCtr ($oldIter) -> ${f(oldCtr)}(${f(oldCtr).iter.get})")
//              val newIter = f(oldCtr).iter.get
//              register(oldIter, newIter)
//            } else {
//              dbgs(s"Registering $oldCtr ($oldIter) -> Stream")
//              register(oldIter, streamIters(i).i)
//            }
//        }
//
//        val lastIterMap = allOldIters.zip(streamIters.elems.map(_.isLast)).toMap
//
//        // Maps registers to their 'latest' value
//        val regValues = cm.Map[Sym[_], Sym[_]]()
//
//        // Reads tokens and their validity from tokens, wrEns
//
//        val (tokens, wrEns) = (controllerInfo.tokenFIFO, controllerInfo.acquireFIFO) match {
//          case (Some(tokenFIFO), Some(acquireFIFO)) =>
//            val tk = controllerInfo.tokenStreamType.unpackStruct(tokenFIFO.deq())
//            val wEns = controllerInfo.tokenEnableType.unpackStruct(acquireFIFO.deq()).mapValues(_.as[Bit])
//            (tk, wEns)
//          case (None, None) =>
//            (Map.empty, Map.empty[Sym[_], Bit])
//        }
//
//        intakeTokens.clear()
//        tokens.foreach {
//          case (mem: Reg[_], value) =>
//            dbgs(s"Handling Register: $mem -> ${f(mem)}")
//
//            implicit def bEV: Bits[mem.RT] = mem.A.asInstanceOf[Bits[mem.RT]]
//
//            val holdReg = Reg[mem.RT]
//            holdReg.explicitName = s"HoldReg_${lhs}_$mem"
//            holdReg.nonbuffer
//            holdReg.write(value.asInstanceOf[mem.RT], wrEns(mem))
//
//            // write the result of the regvalue to the reg
//            f(mem).write(holdReg.value, wrEns(mem))
//            regValues(mem) = void
//
//          case (mem, value) if MemStrategy(mem) == Buffer =>
//            implicit def bEV: Bits[value.R] = value.asSym.tp.asInstanceOf[Bits[value.R]]
//
//            val holdReg = Reg[value.R]
//            holdReg.explicitName = s"HoldReg_${lhs}_$mem"
//            holdReg.nonbuffer
//            holdReg.write(value.asInstanceOf[value.R], wrEns(mem))
//            intakeTokens(mem) = holdReg.value.asInstanceOf[Bits[value.R]]
//            dbgs(s"Registering Intake Token for $mem")
//        }
//
//
//        stageWithFlow(UnitPipe(Set.empty, stageBlock {
//          inlineBlock(foreachOp.block)
//        }, None)) {
//          newInner =>
//            val newSched = lhs.getRawSchedule.getOrElse(Pipelined)
//            dbgs(s"Inner Schedule: $newSched")
//            newInner.userSchedule = newSched
//        }
//        //        inlineBlock(foreachOp.block)
//        regValues ++= (regValues.keys.toSeq.map {
//          case reg: Reg[_] => reg.asSym -> f(reg).value.asInstanceOf[Sym[_]]
//        })
//
//        retimeGate()
//        // Release tokens
//        val updatedValues = intakeTokens ++ regValues
//        controllerInfo.finishTokenFIFO match {
//          case Some(finishTokenFIFO) =>
//            finishTokenFIFO.enq(controllerInfo.tokenStreamType.packStruct(updatedValues.toMap.mapValues(_.asInstanceOf[Bits[_]])))
//          case None =>
//            dbgs(s"Skipping Finish Token FIFO staging -- no tokens found!")
//        }
//
//        // Update StopWhen -- on the last iteration, kill the controller.
//        // By stalling this out, we can guarantee that the preceding writes happen before the controller gets killed
//        retimeGate()
//        val endOfWorld = getOutermostIter(lhs.ancestors).map(lastIterMap(_)).getOrElse(Bit(true))
//        stopWhen.write(endOfWorld, endOfWorld)
//
//      }, newIters.map(_.unbox.asInstanceOf[I32]), Some(stopWhen))) {
//        newForeach =>
//          transferData(lhs, newForeach)
//          newForeach.ctx = augmentCtx(lhs.ctx)
//          if (lhs.isOuterControl) {
//            newForeach.userSchedule = Pipelined
//          } else {
//            dbgs(s"Forwarding schedule: $lhs => ${lhs.getRawSchedule}")
//            newForeach.userSchedule = lhs.getRawSchedule.getOrElse(Pipelined)
//          }
//      }
    }

    dbgs(s"Creating Release Controller")
    val finisher = indent {
      isolateSubst() { createReleaseController(controllerInfo) }
    }

    innerControllerMap(lhs) = ControllerImpl(counterGen, mainCtrl, finisher)
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case accelScope: AccelScope => inAccel {
      printRegister = false

      dbgs(s"Computing Global Mem Graph")
      tokenComms = accelScope.block.nestedStms.filter(_.isMem).filterNot(isInternalMem).flatMap(computeProducerConsumers(_))
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
        stageWithFlow(UnitPipe(Set.empty, stageBlock {
          visitInnerForeach(lhs, foreachOp)
          spatial.lang.void
        }, None)) {
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
      val cloned = super.transform(lhs, mem)
      cloned.bufferAmount = computeBufferDepth(lhs)
      cloned.isNonBuffer = true
      cloned

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

    case SRAMRead(mem, _, _) if intakeTokens.contains(mem) =>
      val newRead = mirror(lhs, rhs)
      dbgs(s"Tagging SRR $rhs -> $newRead with token ${intakeTokens(mem)}")
      newRead.bufferIndex = intakeTokens(mem).asInstanceOf[I32]
      newRead

    case SRAMRead(_, _, _) =>
      dbgs(s"Skipping SRAM Read: $lhs = $rhs")
      super.transform(lhs, rhs)

    case SRAMWrite(mem, _, _, _) if intakeTokens.contains(mem) =>
      val newWrite = mirror(lhs, rhs)
      dbgs(s"Tagging SRW $rhs -> ${newWrite} with token ${intakeTokens(mem)}")
      newWrite.bufferIndex = intakeTokens(mem).asInstanceOf[I32]
      newWrite

    case SRAMWrite(mem, _, _, _) =>
      dbgs(s"Skipping SRAM Write: $lhs = $rhs")
      super.transform(lhs, rhs)

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
}


