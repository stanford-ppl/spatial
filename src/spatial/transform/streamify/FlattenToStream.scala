package spatial.transform.streamify

import argon.transform.{ForwardTransformer, MutateTransformer}
import argon._
import argon.tags.struct
import spatial.lang._
import spatial.node._
import spatial.traversal.AccelTraversal

import scala.collection.{mutable => cm}
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.access._
import spatial.util.TransformUtils._

trait MemStrategy

// When a Token is needed and Buffers are needed
case object Buffered extends MemStrategy

// When the memory should be duplicated -- for registers.
case object Duplicate extends MemStrategy

// When the object needs ordering, but shouldn't be buffered or duplicated (FIFOs, LIFOs, etc.)
case object TokenOnly extends MemStrategy

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
sealed trait RWEdge
case object Forward extends RWEdge
case object Backward extends RWEdge

case class FlattenToStream(IR: State)(implicit isl: poly.ISL) extends ForwardTransformer with AccelTraversal {

  type IterFIFO = FIFO[PseudoIters[Vec[PseudoIter]]]

  abstract trait MemKey
  case class InnerMemKey(writeCtrl: Option[Sym[_]], readCtrl: Sym[_], mem: Sym[_]) extends MemKey
  case class OuterMemKey(writeCtrl: Option[Sym[_]], reader: Sym[_], mem: Sym[_]) extends MemKey

  case class RWTriple(writer: Option[Sym[_]], reader: Sym[_], edgeType: RWEdge) {
    writer.foreach {
      wr =>
        assert(wr.writtenMem == reader.readMem, s"Expected Writer and Reader to read and write to the same location. Instead, got: Write($wr = ${wr.writtenMem}) != Read($reader = ${reader.readMem})")
    }
    if (writer.isEmpty) {
      assert(edgeType == Forward, s"Cannot have backwards initialization")
    }

    def isInternal: Boolean = readParent == wrParent

    def mem: Sym[_] = reader.readMem.get

    def readParent: Option[Sym[_]] = reader.blk.s
    def wrParent: Option[Sym[_]] = writer.flatMap(_.blk.s)

    def toMemKey: MemKey = {
      // There's a mismatch if dest is a RegRead and is used in a counterchain (i.e. dest is a floating transient)
      val isInner = reader.blk.s == reader.parent.s
      if (isInner) {
        // This deduplicates based on the writing and reading InnerControllers
        InnerMemKey(wrParent, readParent.get, mem)
      } else {
        OuterMemKey(wrParent, reader, mem)
      }
    }
  }


  case class MemInfoMap(triples: Seq[RWTriple]) {
    def toDotGraph: String = {
      // print all relevant inner controllers
      val nodes = triples.flatMap(triple => Seq(triple.reader) ++ triple.writer.toSeq)
      val asInnerControls = nodes.groupBy(_.blk)
      val innerControlString = asInnerControls.map {
        case (ctrl, children) =>
          // Deduplicate the children
          val deduplicatedChildren = children.toSet.toList
          val sortedChildren = deduplicatedChildren.sortBy(_.progorder).map(child => s"""<$child> $child = ${child.op.get}]""")
          val ctrler = ctrl.s.get
          s"""subgraph cluster_$ctrler {
             | label = "$ctrler = ${ctrler.op.get}"
             | struct_${ctrler} [shape=record; label = "{${sortedChildren.mkString("|")}}"];
             | }""".stripMargin
      }.mkString(";\n")

      val edgeString = triples.map {
        case triple@RWTriple(Some(writer), reader, edgeType) =>
          val source = s"struct_${triple.wrParent.get}:$writer"
          val dest = s"struct_${triple.readParent.get}:$reader"
          val style = edgeType match {
            case Forward => "solid"
            case Backward => "dashed"
          }

          val position = edgeType match {
            case Forward => "w"
            case Backward => "e"
          }

          s"$source:$position -> $dest:$position [style = $style]"
        case RWTriple(None, reader, Forward) =>
          s"// Initialization for $reader"
      }.mkString(";\n")

      s"""digraph {
         |$innerControlString
         |$edgeString
         |}""".stripMargin
    }
  }

  var memInfo: MemInfoMap = null

  private def computeMemInfoMap(lhs: Sym[_]): MemInfoMap = {
    val allMems = lhs.blocks.flatMap(_.nestedStms).filter(_.isMem)
    val triples = allMems.flatMap {
      case reg: Reg[_] =>
        dbgs(s"Reg: $reg")
        indent {
          reg.readers.flatMap {
            reader =>
              dbgs(s"Read: $reader")
              indent {
                val reachingWrites = reachingWritesToReg(reader, reg.writers, writesAlwaysKill = true)
                // Exclude writers within the same controller -- those get restaged and will be handled internally.
                val writeTriples = reachingWrites.map {
                  write =>
                    val (_, dist) = LCAWithDataflowDistance(write, reader)
                    val isForward = dist > 0
                    RWTriple(Some(write), reader, if (isForward) Forward else Backward)
                }
                val canInit = writeTriples.forall(_.edgeType == Backward)
                writeTriples ++ (if (canInit) Seq(RWTriple(None, reader, Forward)) else Seq.empty)
              }
          }
        }
      case _ => Seq.empty
    }
    dbgs(s"Analyzed Triples: $triples")

    MemInfoMap(triples.filterNot(_.isInternal))
  }

  private val regFIFOs = cm.Map[MemKey, FIFO[_]]()

  private var accelHandle: BundleHandle = null

  def getRegisterFIFO[T: Bits](key: MemKey): FIFO[T] = {
    (regFIFOs.get(key) match {
      case Some(fifo) => fifo
      case None =>
        IR.withScope(accelHandle) {
          val newFIFO = FIFO[T](I32(32))
          regFIFOs(key) = newFIFO
          newFIFO
        }
    }).asInstanceOf[FIFO[T]]
  }

  // Maps registers to their 'latest' value
  private val regValues = cm.Map[Sym[_], Sym[_]]()

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

  private def intakeRegisters(lhs: Sym[_]): Set[Reg[_]] = {
    // if a register is read from and written to only inside this controller, we can ignore it.

    val reads = lhs.readMems.filter(_.isReg)

    def isConditionalWrite(reg: Sym[_]): Boolean = {
      // All writes to the register within lhs
      val regWriters = reg.writers.filter {
        reg =>
          reg.parent.s match {
            case Some(l) if l == lhs => true
            case _ => false
          }
      }
      regWriters.exists {
        case Op(RegWrite(_, _, ens)) => ens.nonEmpty
      }
    }

    val writes = lhs.writtenMems.filter(_.isReg).filter(isConditionalWrite)

    // We need the previous value of a register if:
    //   1. It's read (obviously)
    //   2. It's written to, but conditionally
    // However, we don't need it if it's an internal register -- all reads and writes to this register are within LHS
    (reads ++ writes).filterNot(isInternalMem).map { case reg: Reg[_] => reg }
  }

  private def outputRegisters(lhs: Sym[_]): Set[Reg[_]] = {
    // Output the register if it is written to and is not internal
    lhs.writtenMems.filter(_.isReg).filterNot(isInternalMem).map { case reg: Reg[_] => reg }
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
    val iterFIFO = FIFO[PseudoIters[Vec[PseudoIter]]](I32(32))
    iterFIFO.explicitName = s"IterFIFO_$lhs"

    def recurseHelper(chains: List[CounterChain], backlog: cm.Buffer[Sym[_]], firstIterMap: cm.Map[Sym[_], Bit]): Unit = {
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
                handleIntakeRegisters(cchain.blk.s.get, Seq(s), firstIterMap.toMap, Map.empty)
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

          val newChains = cchain.counters.map(mirrorSym(_).unbox)
          val newCChain = CounterChain(newChains)
          val newIters = makeIters(newChains)
          backlog.appendAll(newIters)
          register(cchain.counters.flatMap(_.iter), newChains.flatMap(_.iter))

          // TODO: Parallelize the innermost loop by as much as possible in order to not cause slowdowns here
          stageWithFlow(OpForeach(Set.empty, newCChain, stageBlock {
            updateFirstIterMap()
            val oldItersAsI32 = allOldIters.map(_.unbox.asInstanceOf[I32])
            val allNewIters = f(oldItersAsI32)
            val isLasts = isLastIters(allNewIters: _*)
            val indexData = allNewIters.zip(isLasts).map {
              case (iter, last) => PseudoIter(iter, firstIterMap(iter), last)
            }
            val pIters = PseudoIters(Vec.fromSeq(indexData))
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
            recurseHelper(rest, backlog, firstIterMap)
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

  def handleIntakeRegisters(lhs: Sym[_], reads: Seq[Sym[_]], firstIterMap: Map[Sym[_], Bit], hasBackWrite: Map[Sym[_], Boolean]): Sym[_] = {
    reads.foreach {
      case read@Op(RegRead(reg)) =>
        assert(!reg.isNonBuffer, s"Register $reg was marked nonBuffer -- this breaks when streamifying.")

        type RT = reg.RT
        implicit def bEV: Bits[RT] = reg.A.asInstanceOf[Bits[RT]]
        val castedReg = reg.asInstanceOf[Reg[RT]]

        // Get all triples which write to the register within LHS, but deduplicate in case there are multiple RegReads / RegWrites
        val allRelevant = memInfo.triples.filter(_.reader == read)
        dbgs(s"Relevant Triples: $allRelevant")

        val intakes = indent {
          allRelevant map {
            case triple@RWTriple(Some(writer), reader, edgeType) =>
              val (lca, writerPath, readerPath) = LCAWithPaths(writer.toCtrl, lhs.toCtrl)
              dbgs(s"Writer: $writer ; LCA: $lca ; writerPath: $writerPath ; readerPath: $readerPath")
              val fifo = getRegisterFIFO[RT](triple.toMemKey)

              // we should read if it's the first iteration within the LCA
              val isFirstIterInLCA = getOutermostIter(readerPath.drop(1)).map(firstIterMap(_)).getOrElse(Bit(true))

              val en = edgeType match {
                case Forward => isFirstIterInLCA
                case Backward =>
                  // We read from the backward one if it's not the first iteration of the LCA
                  val isFirstIterOfLCA = getOutermostIter(lca.ancestors(reg.parent).reverse).map(firstIterMap(_)).get
                  isFirstIterInLCA & !isFirstIterOfLCA
              }
              dbgs(s"Fetching: $reg <- $fifo.deq($en)")
              (en, fifo.deq(en))
            case RWTriple(None, _, Forward) =>
              // This counts as initialization
              val (_, _, pathToReg) = LCAWithPaths(reg.parent, lhs.toCtrl)
              val shouldInit = getOutermostIter(pathToReg.drop(1)).map(firstIterMap(_)).getOrElse(Bit(true))
              val init = castedReg match {case Op(RegNew(init)) => init.unbox}
              dbgs(s"Initializing: $reg <- $init (en = $shouldInit)")
              (shouldInit, init)
          }
        }

        dbgs(s"Intakes: ${intakes.mkString(", ")}")
        assert(intakes.map(_._1).toSet.size == intakes.size, s"Intake values should have distinct enable signals.")
        val wrVal = oneHotMux(intakes.map(_._1), intakes.map(_._2.asInstanceOf[RT]))
        val wrEn = intakes.map(_._1).reduceTree(_ | _)
        wrEn match {
          case Const(x) if x.toBoolean =>
            // If we're always reading, then no hold register is necessary
            regValues(reg) = mux(wrEn, wrVal, f(castedReg).unbox.value)
          case _ if hasBackWrite.getOrElse(reg, false) =>
            // If there's a backwards write, then it'll be respected here as well
            regValues(reg) = mux(wrEn, wrVal, f(castedReg).unbox.value)
          case _ =>
            // If we're not always reading, and there isn't a backwards write inside the same inner controller
            // then we need to hold the value.
            val holdReg = mirrorSym(castedReg).unbox
            holdReg.nonbuffer
            holdReg.write(wrVal, wrEn)
            val newCopy = f(castedReg)
            newCopy.write(holdReg.value)
            regValues(reg) = newCopy.value
        }
    }
  }

  def handleOutputRegisters(lhs: Sym[_], lastIterMap: Map[Sym[_], Bit]): Unit = {
    outputRegisters(lhs) foreach {
      reg =>
        assert(!reg.isNonBuffer, s"Register $reg was marked nonBuffer -- this breaks when streamifying.")
        type RT = reg.RT
        implicit def bEV: Bits[RT] = reg.A.asInstanceOf[Bits[RT]]
        val castedReg = reg.asInstanceOf[Reg[RT]]

        // Get all triples which write to the register within LHS, but deduplicate in case there are multiple RegWrites
        val allRelevant = memInfo.triples.filter {
          case tp@RWTriple(Some(writer), _, _) =>
            // Writers within lhs that write to reg
            tp.wrParent.contains(lhs) && writer.writtenMem.contains(reg)
          case _ => false
        }.map {
          triple =>
            // Deduplicate based on reader's parent controller and edge type
            triple.toMemKey -> triple
        }.toMap
        dbgs(s"Relevant Triples: $allRelevant")

        val wrData = regValues.get(castedReg) match {
          case Some(value) => value.asInstanceOf[RT]
          case None => f(castedReg).value
        }

        allRelevant.foreach {
          case (memKey, RWTriple(_, dest, edgeType)) =>
            val fifo = getRegisterFIFO[RT](memKey)

            val (lca, writerPath, readerPath) = LCAWithPaths(lhs.toCtrl, dest.toCtrl)
            dbgs(s"Writer: $lhs ; LCA: $lca ; writerPath: $writerPath ; readerPath: $readerPath")

            val isLastIterWithinLCA = getOutermostIter(writerPath.drop(1)).map(lastIterMap(_)).getOrElse(Bit(true))
            val wrEnable = edgeType match {
              case Forward => isLastIterWithinLCA
              case Backward =>
                // we send the value backward if it isn't the last iteration of the LCA
                val isLastIterOfLCA = getOutermostIter(lca.ancestors(reg.parent).reverse).map(lastIterMap(_)).get
                isLastIterWithinLCA & !isLastIterOfLCA
            }
            fifo.enq(wrData, wrEnable)
        }
    }
  }

  private def visitInnerForeach(lhs: Sym[_], foreachOp: OpForeach): Sym[_] = {
    dbgs(s"Visiting Inner Foreach: $lhs = $foreachOp")

    // TODO: Should we get rid of streamed iters if no chains require external input?
    // This pushes isFirst/isLast calcs in, potentially triggering delayed conditional dequeues if there's enough
    // counters.

    // Add a forever counter in order accommodate reading ancestral counters

    // Mirror the local chains, but stop at the innermost dynamic counter
    val staticInnerCtrs = foreachOp.cchain.counters.reverse.takeWhile(_.isStatic).reverse
    val newInnerCtrs = staticInnerCtrs.map(mirrorSym(_).unbox)
    val newInnerIters = makeIters(newInnerCtrs)
    val foreverCtr = stage(ForeverNew())
    val foreverIter = makeIter(foreverCtr).unbox

    val newCChain = CounterChain(Seq(foreverCtr) ++ newInnerCtrs)
    val IterFIFOWithInfo(iterFIFO, iterToIndexMap) = indent { createCounterGenerator(lhs) }

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

    regValues.clear()

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

      // Fetch Register values
      dbgs(s"Fetching Register Values")
      indent {
        // Filter to the FIRST read for each intake register
        val regs = intakeRegisters(lhs)
        val reads = regs.flatMap(r => foreachOp.block.stms.find(_.readMem.contains(r)))
        val backWriteMap = regs.map(reg => {
          reg -> lhs.writtenMems.contains(reg)
        }).toMap[Sym[_], Boolean]
        handleIntakeRegisters(lhs, reads.toSeq, firstIterMap.toMap, backWriteMap)
      }


      // Fetch Buffer Tokens

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
        case other => visit(other)
      }


      // Release Register values if it was written by this controller
      dbgs(s"Writing Register Values")
      indent {
        handleOutputRegisters(lhs, lastIterMap.toMap)
      }

      retimeGate()
      // Release Buffer Tokens
      // Update StopWhen -- on the last iteration, kill the controller.
      // By stalling this out, we can guarantee that the preceding writes happen before the controller gets killed
      retimeGate()
      val endOfWorld = getOutermostIter(lhs.ancestors).map(lastIterMap(_)).getOrElse(Bit(true))
      stopWhen.write(endOfWorld, endOfWorld)

    }, Seq(foreverIter) ++ newInnerIters.map(_.unbox.asInstanceOf[I32]), Some(stopWhen))) {
      newForeach =>
        transferData(lhs, newForeach)
        newForeach.ctx = augmentCtx(lhs.ctx)
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case accelScope: AccelScope => inAccel {
      memInfo = computeMemInfoMap(lhs)
      dbgs(s"============MemInfoMap============")
      dbgs(memInfo.toDotGraph)
      dbgs(s"==================================")
      stageWithFlow(AccelScope(stageBlock {
        Stream {
          accelHandle = IR.getCurrentHandle()
          accelScope.block.nestedStms.foreach {
            case loop@Op(foreachOp: OpForeach) if inHw && loop.isInnerControl =>
              dbgs(s"Transforming Inner: $loop = $foreachOp")
              indent { isolateSubst() { visitInnerForeach(loop, foreachOp) } }
            case _ =>
          }
        }
      })) {
        lhs2 => transferData(lhs, lhs2)
      }
    }

    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]

  override def postprocess[R](block: Block[R]): Block[R] = {
    val result = super.postprocess(block)
    accelHandle = null
    dbgs("="*80)
    dbgs(s"regFIFOs: $regFIFOs")
    dbgs("="*80)
    result
  }
}


