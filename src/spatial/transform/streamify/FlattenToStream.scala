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
import spatial.util.TransformUtils._

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
  * A Triple containing an optional writer, a reader, and whether the write is a back-edge write across iterations.
  * @param writer Either the Writer symbol, or None for init
  * @param reader The Reader symbol
  * @param edgeType Whether the edge is a back-edge
  */

case class FlattenToStream(IR: State)(implicit isl: poly.ISL) extends ForwardTransformer with AccelTraversal {

  type IterFIFO = FIFO[PseudoIters[Vec[PseudoIter]]]

  private val regFIFOs = cm.Map[TokenComm, FIFO[_]]()

  private var accelHandle: BundleHandle = null

  private var tokenComms: Seq[TokenComm] = null

  private def getFIFO[T: Bits](key: TokenComm): FIFO[T] = {
    (regFIFOs.get(key) match {
      case Some(fifo) => fifo
      case None =>
        IR.withScope(accelHandle) {
          val bufferDepth = computeBufferDepth(key.mem)
          val newFIFO = FIFO[T](2*bufferDepth)
          newFIFO.explicitName = s"CommFIFO_${key.mem}_${key.src.s.get}_${key.dst.s.get}"
          regFIFOs(key) = newFIFO
          if (key.direction == Return) {
            newFIFO.fifoInits = Range(0, bufferDepth).map(I32(_))
          }
          newFIFO
        }
    }).asInstanceOf[FIFO[T]]
  }

  /**
    * FlattenToStream takes an arbitrary chunk of code and turns it into a single unitpipe containing
    *   many stream-synchronized controllers -- one for each inner controller.
    *
    * In addition, this also creates two auxiliary structures:
    *   1. Counter Generator and Token Intake
    *   2. Token Distributor
    */
  def isInternalMem(mem: Sym[_]): Boolean = {
    val regReaders = mem.readers.flatMap(_.parent.s)
    val regWriters = mem.writers.flatMap(_.parent.s)
    (regReaders union regWriters).size == 1
  }

  type MemTokenEnableMap = Map[Sym[_], Seq[(TokenComm, Bit)]]
  /**
    * Maps each TokenComm for a controller to an enable signal
    * @param ctrl The controller in question
    * @param firstIterMap A map of each iterator to a bit indicating the first iteration
    * @return A Map from memory to the relevant TokenComms
    */
  private def computeIntakeEnablesForCtrl(ctrl: Sym[_], firstIterMap: Map[Sym[_], Bit]): MemTokenEnableMap = {
    val intakes = tokenComms.filter(_.dst == ctrl.toCtrl)
    intakes.groupBy(_.mem).map {
      case (mem, comms) =>
        dbgs(s"Setting up input enables for $mem")
        val commToEn = indent { comms.map {
          case comm@TokenComm(mem, src, _, edgeType, _) =>
            dbgs(comm)
            val (lca, srcPath, dstPath) = LCAWithPaths(src.toCtrl, ctrl.toCtrl)
            val isFirstIterInLCA = getOutermostIter(dstPath.drop(1)).map(firstIterMap(_)).getOrElse(Bit(true))
            val en = edgeType match {
              case Forward | Initialize(_) | Return =>
                // For forward edges, we care if we're on the first iteration WITHIN the LCA
                // This also works for Return edges because they're initialized with data
                isFirstIterInLCA
              case Backward =>
                // For backward edges, we care if we're NOT on the first iteration OF the LCA (since there won't be data yet)
                val isFirstIterOfLCA = getOutermostIter(lca.ancestors(mem.parent).reverse).map(firstIterMap(_)).get
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
          case comm@TokenComm(mem, _, dst, edgeType, _) =>
            dbgs(comm)
            val (lca, writerPath, _) = LCAWithPaths(ctrl.toCtrl, dst.toCtrl)
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
      case (comm@TokenComm(_, _, _, Initialize(v), _), en) =>
        v.asInstanceOf[T]
      case (comm, en) =>
        getFIFO[T](comm).deq(en)
    }
    val writeValue = oneHotMux(tokenWithEns.map(_._2), dequeuedValues)
    hold.write(writeValue, shouldWrite)

    shouldWrite
  }

  private def handleIntakeRegisters(tokenEnables: MemTokenEnableMap): Map[Sym[_], Sym[_]] = {
    tokenEnables.collect {
      case (r: Reg[_], tokenWithEns) =>
        implicit def bEV: Bits[r.RT] = r.A
        // We have a bunch of sources for the reg. For this, first we stage into a holding register
        val holdReg = mirrorSym(r).unbox
        holdReg.nonbuffer
        val shouldWrite = createIntakeReads(holdReg, tokenWithEns)
        r -> mux(shouldWrite, holdReg.value.asInstanceOf[Bits[r.RT]], f(r).value.asInstanceOf[Bits[r.RT]]).asSym
    }.toMap[Sym[_], Sym[_]]
  }

  private def handleIntakeBufferTokens(tokenEnables: MemTokenEnableMap): Map[Sym[_], I32] = {
    tokenEnables.collect {
      case (mem, tokenWithEns) if MemStrategy(mem) == Buffer =>
        val holdReg = Reg[I32]
        holdReg.nonbuffer
        holdReg.explicitName = s"HoldReg_$mem"
        createIntakeReads(holdReg, tokenWithEns)
        mem -> holdReg.value
    }
  }

  private def handleOutputs(tokenEnables: MemTokenEnableMap, tokenMap: Map[Sym[_], Sym[_]]) = {
    tokenEnables.foreach {
      case (mem, commWithEns) =>
        val value = tokenMap(mem)
        implicit def bEV: Bits[value.R] = value.unbox.asInstanceOf[Bits[value.R]]
        commWithEns foreach {
          case (comm, en) =>
            val fifo = getFIFO[value.R](comm)
            fifo.enq(value.unbox.asInstanceOf[value.R], en)
        }
    }
  }

  /**
    * A pair of an IterFIFO and a map from OLD iter -> index
    * @param iterFIFO
    * @param info
    */
  case class IterFIFOWithInfo(iterFIFO: IterFIFO, info: Map[Sym[_], Int])

  private def createCounterGenerator(lhs: Sym[_]): IterFIFOWithInfo = {
    val allChains = lhs.ancestors.flatMap(_.cchains)
    dbgs(s"All chains of $lhs = $allChains")
    val allCounters = allChains.flatMap(_.counters)
    val allOldIters = allCounters.flatMap(_.iter)

    // This is just a hack to create the Bits evidence needed.
    implicit def vecBitsEV: Bits[Vec[PseudoIter]] = Vec.fromSeq(allCounters map {x => PseudoIter(I32(0), Bit(true), Bit(true))})
    val iterFIFO: FIFO[PseudoIters[Vec[PseudoIter]]] = FIFO[PseudoIters[Vec[PseudoIter]]](I32(-1))
    iterFIFO.explicitName = s"IterFIFO_$lhs"

    def recurseHelper(chains: List[CounterChain], backlog: cm.Buffer[Sym[_]], firstIterMap: cm.Map[Sym[_], Bit]): Unit = {
      dbgs(s"Peeling chain: ${chains.head}")
      def updateFirstIterMap(): Unit = {
        if (backlog.isEmpty) { return }
        dbgs(s"Updating First Iter Map: $firstIterMap")
        // takes the current firstIterMap and fills in all missing entries
        // backlog is outermost to innermost, all inner compared to entries firstIterMap
        val isFirsts = isFirstIters(backlog.map(_.unbox.asInstanceOf[I32]):_*)
        firstIterMap.keys.foreach {
          iter => firstIterMap(iter) &= isFirsts.head
        }
        firstIterMap ++= (backlog zip isFirsts).toMap
        dbgs(s"Updated First Iter Map: $firstIterMap")
        backlog.clear()
      }

      def fetchDeps(cchain: CounterChain): Unit = {
        val deps = cchain.counters.flatMap(_.inputs)
        if (deps.nonEmpty) {
          updateFirstIterMap()

          val intakeTokens = computeIntakeEnablesForCtrl(cchain.blk.s.get, firstIterMap.toMap)
          dbgs(s"Intake Tokens: $intakeTokens")
          val regValues = handleIntakeRegisters(intakeTokens)

          // Perform recursive remapping of deps
          val visited = cm.Set[Sym[_]]()
          def dfs(s: Sym[_]): Unit = {
            if (visited contains s) { return }
            visited.add(s)
            // only take dependencies in the current scope
            val inputs = s.inputs.filter(_.blk.s == s.blk.s)
            dbgs(s"Inputs of $s = $inputs")
            inputs.foreach(visit)
            // after processing all dependencies, mirror yourself
            dbgs(s"Substitutions: $subst")
            s match {
              case Op(RegRead(mem)) =>
                register(mem -> mirrorSym(mem))
                register(s -> regValues(mem))
              case other if !subst.contains(other) => register(other -> mirrorSym(other))
              case _ => // pass
            }
          }
          deps.foreach(dfs)
        }
      }
      chains match {
        case cchain :: Nil =>
          // innermost iteration

          fetchDeps(cchain)

          var shouldFullyPar = true
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
          backlog.appendAll(newIters)
          register(cchain.counters.flatMap(_.iter), newChains.flatMap(_.iter))

          stageWithFlow(OpForeach(Set.empty, newCChain, stageBlock {
            updateFirstIterMap()
            val oldItersAsI32 = allOldIters.map(_.unbox.asInstanceOf[I32])
            val allNewIters = f(oldItersAsI32)
            val isLasts = isLastIters(allNewIters: _*)
            val indexData = allNewIters.zip(isLasts).map {
              case (iter, last) => PseudoIter(iter, firstIterMap(iter), last)
            }
            val pIters = PseudoIters(Vec.fromSeq(indexData))

            // update the size of the FIFO to be sufficiently large as to not block.
            iterFIFO.asSym match {
              case Op(fnew@FIFONew(size)) =>
                isolateSubst() {
                  register(size -> totalPar*2)
                  fnew.update(f)
                }
            }

            iterFIFO.enq(pIters)
          }, newIters.asInstanceOf[Seq[I32]], None)) {
            lhs2 =>
              lhs2.explicitName = s"CounterGen_${cchain.owner}"
              lhs2.ctx = augmentCtx(cchain.ctx)
          }

        case cchain :: rest =>
          // Fetch all dependencies of the chain
          fetchDeps(cchain)

          val newChains = cchain.counters.map(mirrorSym(_).unbox)
          val newCChain = CounterChain(newChains)
          val newIters = makeIters(newChains)
          backlog.appendAll(newIters)
          register(cchain.counters.flatMap(_.iter), newChains.flatMap(_.iter))

          stageWithFlow(OpForeach(Set.empty, newCChain, stageBlock {
            indent {
              recurseHelper(rest, backlog, firstIterMap)
            }
          }, newIters.asInstanceOf[Seq[I32]], None)) {
            lhs2 =>
              lhs2.explicitName = s"CounterGen_${cchain.owner}"
              lhs2.ctx = augmentCtx(cchain.ctx)
          }
      }
    }
    isolateSubst() { recurseHelper(allChains.toList, cm.ListBuffer.empty, cm.Map.empty) }

    IterFIFOWithInfo(iterFIFO, allOldIters.zipWithIndex.toMap)
  }

  private def computeBufferDepth(mem: Sym[_]): Int = {
    // we need 2 copies in each user at the level of the mem.
    2 * mem.parent.children.count(ctrl => (ctrl.sym.effects.reads ++ ctrl.sym.effects.writes).contains(mem))
  }

//  def handleOutputRegisters(lhs: Sym[_], lastIterMap: Map[Sym[_], Bit]): Unit = {
//    outputRegisters(lhs) foreach {
//      reg =>
//        assert(!reg.isNonBuffer, s"Register $reg was marked nonBuffer -- this breaks when streamifying.")
//        type RT = reg.RT
//        implicit def bEV: Bits[RT] = reg.A.asInstanceOf[Bits[RT]]
//        val castedReg = reg.asInstanceOf[Reg[RT]]
//
//        // Get all triples which write to the register within LHS, but deduplicate in case there are multiple RegWrites
//        val allRelevant = memInfo.triples.filter {
//          case tp@RegRWTriple(Some(writer), _, _) =>
//            // Writers within lhs that write to reg
//            tp.wrParent.contains(lhs) && writer.writtenMem.contains(reg)
//          case _ => false
//        }.map {
//          triple =>
//            // Deduplicate based on reader's parent controller and edge type
//            triple.toMemKey -> triple
//        }.toMap
//        dbgs(s"Relevant Triples: $allRelevant")
//
//        val wrData = regValues.get(castedReg) match {
//          case Some(value) => value.asInstanceOf[RT]
//          case None => f(castedReg).value
//        }
//
//        allRelevant.foreach {
//          case (memKey, RegRWTriple(_, dest, edgeType)) =>
//            val fifo = getFIFO[RT](memKey)
//
//            val (lca, writerPath, readerPath) = LCAWithPaths(lhs.toCtrl, dest.toCtrl)
//            dbgs(s"Writer: $lhs ; LCA: $lca ; writerPath: $writerPath ; readerPath: $readerPath")
//
//            val isLastIterWithinLCA = getOutermostIter(writerPath.drop(1)).map(lastIterMap(_)).getOrElse(Bit(true))
//            val wrEnable = edgeType match {
//              case Forward => isLastIterWithinLCA
//              case Backward =>
//                // we send the value backward if it isn't the last iteration of the LCA
//                val isLastIterOfLCA = getOutermostIter(lca.ancestors(reg.parent).reverse).map(lastIterMap(_)).get
//                isLastIterWithinLCA & !isLastIterOfLCA
//            }
//            fifo.enq(wrData, wrEnable)
//        }
//    }
//  }

//  private def handleOutputTokens(lhs: Sym[_], lastIterMap: Map[Sym[_], Bit], tokenMap: Map[Sym[_], I32]): Unit = {
//    tokenMap foreach {
//      case (mem, token) =>
//
//        val destinations = bufferData.destinationMap((mem, lhs))
//        dbgs(s"Issuing tokens for $lhs -> $mem =")
//        destinations.foreach {
//          case DestData(dest, edgeType) =>
//            val memKey = InnerMemKey(Some(lhs), dest, mem)
//            val fifo = getFIFO[I32](memKey)
//            val (lca, writerPath, _) = LCAWithPaths(lhs.toCtrl, dest.toCtrl)
//            val isLastIterWithinLCA = getOutermostIter(writerPath.drop(1)).map(lastIterMap(_)).getOrElse(Bit(true))
//            val wrEnable = edgeType match {
//              case Forward | Return =>
//                // We also send the token back for Return so thatt it's available for the next iteration.
//                isLastIterWithinLCA
//              case Backward =>
//                // we send the value backward if it isn't the last iteration of the LCA
//                val isLastIterOfLCA = getOutermostIter(lca.ancestors(mem.parent).reverse).map(lastIterMap(_)).get
//                isLastIterWithinLCA & !isLastIterOfLCA
//            }
//            fifo.enq(token, wrEnable)
//        }
//    }
//  }

  private def visitInnerForeach(lhs: Sym[_], foreachOp: OpForeach): Sym[_] = {
    dbgs(s"Visiting Inner Foreach: $lhs = $foreachOp")

    // TODO: Should we get rid of streamed iters if no chains require external input?
    // This pushes isFirst/isLast calcs in, potentially triggering delayed conditional dequeues if there's enough
    // counters.

    // Add a forever counter in order accommodate reading ancestral counters

    // Mirror the local chains, but stop at the innermost dynamic counter
    val allChains = lhs.ancestors.flatMap(_.cchains).flatMap(_.counters)

    // We need a forever ctr if there's a dynamic counter.
    val needForever = allChains.exists(!_.isStatic)
    val staticInnerCtrs = allChains.reverse.takeWhile(_.isStatic).reverse // foreachOp.cchain.counters.reverse.takeWhile(_.isStatic).reverse
    val newInnerCtrs = staticInnerCtrs.map(mirrorSym(_).unbox)
    val newInnerIters = makeIters(newInnerCtrs).map(_.unbox.asInstanceOf[I32])

    val (newCChain, newIters) = if (needForever) {
      val foreverCtr = stage(ForeverNew())
      val foreverIter = makeIter(foreverCtr).unbox
      (CounterChain(Seq(foreverCtr) ++ newInnerCtrs), Seq(foreverIter) ++ newInnerIters)
    } else {
      (CounterChain(newInnerCtrs), newInnerIters)
    }

    dbgs(s"Creating Counter Generator")
    val IterFIFOWithInfo(iterFIFO, iterToIndexMap) = indent {
      createCounterGenerator(lhs)
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
    (lhs.readMems ++ lhs.writtenMems).diff(foreachOp.block.internalMems.toSet).foreach {
      case reg: Reg[_] if !reg.isGlobalMem =>
        dbgs(s"Mirroring: $reg")
        register(reg -> mirrorSym(reg))
      case _ =>
    }


    stageWithFlow(OpForeach(Set.empty, newCChain, stageBlock {
      val streamIters = iterFIFO.deq().iters
      val firstIterMap = cm.Map[Sym[_], Bit]()
      val lastIterMap = cm.Map[Sym[_], Bit]()
      iterToIndexMap foreach {
        case (oldIter, index) =>
          val newIter = streamIters(index)

          firstIterMap(oldIter) = newIter.isFirst
          lastIterMap(oldIter) = newIter.isLast

          register(oldIter -> newIter.i.asSym)
      }

      // We register newInnerIters after the streamIters as we wish for local iters to take precedence
      // They'll be the same value, but are analyzable by banking, etc.
      register(staticInnerCtrs.flatMap(_.iter), newInnerIters)

      // Computing Enable Signals
      val commsAndEnables = computeIntakeEnablesForCtrl(lhs, firstIterMap.toMap)

      // Maps registers to their 'latest' value
      val regValues = cm.Map[Sym[_], Sym[_]]()

      dbgs(s"Fetching Registers")
      indent {
        regValues ++= handleIntakeRegisters(commsAndEnables)
      }

      // Fetch Buffer Tokens
      dbgs(s"Fetching SRAM Tokens")
      val intakeTokens = indent {
        // Acquire tokens for each SRAM that we read/write
        handleIntakeBufferTokens(commsAndEnables)
      }

      dbgs(s"Intake Tokens: $intakeTokens")

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
            regValues(reg) = mux(f(ens).toSeq.reduceTree(_&_), f(data).asInstanceOf[Bits[reg.RT]], regValues(reg).asInstanceOf[Bits[reg.RT]]).asInstanceOf[Sym[reg.RT]]
          }
          dbgs(s"reg($reg) = ${regValues(reg)}")

          // stage the write anyways
          visit(rw)
        case sramRead@Op(SRAMRead(mem, _, _)) if intakeTokens.contains(mem) =>
          visit(sramRead)
          f(sramRead).bufferIndex = intakeTokens(mem)
        case sramWrite@Op(SRAMWrite(mem, _, _, _)) if intakeTokens.contains(mem) =>
          visit(sramWrite)
          f(sramWrite).bufferIndex = intakeTokens(mem)
        case other => visit(other)
      }


      dbgs(s"Computing Releases")
      val releaseEnables = indent {
        computeOutputEnablesForCtrl(lhs, lastIterMap.toMap)
      }

      // Release Register values if it was written by this controller
      dbgs(s"Handling Outputs")
      indent {
        dbgs(s"Values: $regValues")
        dbgs(s"Tokens: $intakeTokens")
        handleOutputs(releaseEnables, (regValues ++ intakeTokens).toMap)
      }

      // Release Buffer Tokens
      // Update StopWhen -- on the last iteration, kill the controller.
      // By stalling this out, we can guarantee that the preceding writes happen before the controller gets killed
      retimeGate()
      val endOfWorld = getOutermostIter(lhs.ancestors).map(lastIterMap(_)).getOrElse(Bit(true))
      stopWhen.write(endOfWorld, endOfWorld)

    }, newIters, Some(stopWhen))) {
      newForeach =>
        transferData(lhs, newForeach)
        newForeach.ctx = augmentCtx(lhs.ctx)
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case accelScope: AccelScope => inAccel {
      printRegister = true

      dbgs(s"Computing Global Mem Graph")
      tokenComms = accelScope.block.nestedStms.filter(_.isMem).filterNot(isInternalMem).flatMap(computeProducerConsumers(_))
      indent {
        tokenComms.foreach(dbgs(_))
      }
      withTab(0) {
        dbgs(tokenComms.toDotString)
      }
      dbgs(s"="*100)

      stageWithFlow(AccelScope(stageBlock {
        Stream {
          accelHandle = IR.getCurrentHandle()
          accelScope.block.stms.foreach(visit)
        }
      })) {
        lhs2 => transferData(lhs, lhs2)
      }
    }

    case foreachOp: OpForeach if inHw && lhs.isInnerControl =>
      dbgs(s"Transforming Inner: $lhs = $foreachOp")
      indent { isolateSubst() { visitInnerForeach(lhs, foreachOp) } }

    case ctrlOp: Control[_] if inHw && lhs.isOuterControl =>
      dbgs(s"Skipping Control: $lhs = $ctrlOp")
      ctrlOp.blocks.foreach(inlineBlock(_))
      lhs

    case mem if inHw && lhs.isMem && lhs.isReg =>
      dbgs(s"Skipping $lhs = $mem since it'll be duplicated.")
      lhs

    case mem if inHw && lhs.isMem && lhs.isSRAM =>
      val cloned = super.transform(lhs, mem)
      cloned.bufferAmount = computeBufferDepth(lhs)
      cloned

    case _ =>
      super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]

  override def postprocess[R](block: Block[R]): Block[R] = {
    val result = super.postprocess(block)
    accelHandle = null
    dbgs("="*80)
    dbgs(s"regFIFOs:")
    indent {
      regFIFOs.foreach(dbgs(_))
    }
    dbgs("="*80)
    result
  }
}


