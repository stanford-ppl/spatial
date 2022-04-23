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

  case class RWTriple(writer: Option[Sym[_]], reader: Sym[_], edgeType: RWEdge) {
    writer.foreach {
      wr =>
        assert(wr.writtenMem == reader.readMem, s"Expected Writer and Reader to read and write to the same location. Instead, got: Write($wr = ${wr.writtenMem}) != Read($reader = ${reader.readMem})")
    }
    if (writer.isEmpty) {
      assert(edgeType == Forward, s"Cannot have backwards initialization")
    }

    def isInternal: Boolean = writer match {
      case Some(wr) => wr.parent == reader.parent
      case None => false
    }

    def mem: Sym[_] = reader.readMem.get
  }


  case class MemInfoMap(triples: Seq[RWTriple]) {
    def toDotGraph: String = {
      val mems = triples.map(_.mem).toSet

      // print all relevant inner controllers
      val nodes = triples.flatMap(triple => Seq(triple.reader) ++ triple.writer.toSeq)
      val asInnerControls = nodes.groupBy(_.parent)
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
        case RWTriple(Some(writer), reader, edgeType) =>
          val source = s"struct_${writer.parent.s.get}:$writer"
          val dest = s"struct_${reader.parent.s.get}:$reader"
          val style = edgeType match {
            case Forward => "solid"
            case Backward => "dashed"
          }

          val position = edgeType match {
            case Forward => "w"
            case Backward => "e"
          }

          s"$source:$position -> $dest:$position [style = $style]"
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

  // (mem, Source, Dest) -> FIFO
  private val regFIFOs = cm.Map[(Sym[_], Sym[_], Sym[_]), FIFO[_]]()

  private var accelHandle: BundleHandle = null

  def getRegisterFIFO[T: Bits](mem: Sym[_], src: Sym[_], dest: Sym[_]): FIFO[T] = {
    val key = (mem, src, dest)
    (regFIFOs.get(key) match {
      case Some(fifo) => fifo
      case None =>
        IR.withScope(accelHandle) {
          val newFIFO = FIFO[T](I32(32))
          regFIFOs(key) = newFIFO
          newFIFO.explicitName = s"comm_${src}_${dest}"
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

  private def getRegValue(sym: Sym[_], op: RegRead[_]): Sym[_] = {
    sym
  }

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

    def recurseHelper(chains: List[CounterChain]): Unit = {
      chains match {
        case cchain :: Nil =>
          // innermost iteration
          cchain.counters.flatMap(_.inputs) foreach {
            case sym@Op(rr:RegRead[_]) =>
              register(sym -> getRegValue(sym,  rr))
          }
          val newChains = cchain.counters.map(mirrorSym(_).unbox)
          val newCChain = CounterChain(newChains)
          val newIters = makeIters(newChains)
          register(cchain.counters.flatMap(_.iter), newChains.flatMap(_.iter))

          // TODO: Parallelize the innermost loop by as much as possible in order to not cause slowdowns here
          stageWithFlow(OpForeach(Set.empty, newCChain, stageBlock {

            val oldItersAsI32 = allOldIters.map(_.unbox.asInstanceOf[I32])
            val allNewIters = f(oldItersAsI32)
            val isFirsts = isFirstIters(allNewIters: _*)
            val isLasts = isLastIters(allNewIters: _*)
            val indexData = allNewIters.zip(isFirsts.zip(isLasts)).map {
              case (iter, (first, last)) => PseudoIter(iter, first, last)
            }
            val pIters = PseudoIters(Vec.fromSeq(indexData))
            iterFIFO.enq(pIters)
          }, newIters.asInstanceOf[Seq[I32]], None)) {
            lhs2 =>
              lhs2.explicitName = s"CounterGen_${cchain.owner}"
          }

        case cchain :: rest =>
          // Fetch all dependencies of the chain
          cchain.counters.flatMap(_.inputs) foreach {
            case sym@Op(rr:RegRead[_]) =>
              register(sym -> getRegValue(sym,  rr))
          }
          val newChains = cchain.counters.map(mirrorSym(_).unbox)
          val newCChain = CounterChain(newChains)
          val newIters = makeIters(newChains)
          register(cchain.counters.flatMap(_.iter), newChains.flatMap(_.iter))

          stageWithFlow(OpForeach(Set.empty, newCChain, stageBlock {
            recurseHelper(rest)
          }, newIters.asInstanceOf[Seq[I32]], None)) {
            lhs2 =>
              lhs2.explicitName = s"CounterGen_${cchain.owner}"
          }
      }
    }
    isolateSubst() { recurseHelper(allChains.toList) }

    IterFIFOWithInfo(iterFIFO, allOldIters.zipWithIndex.toMap)
  }

  def handleIntakeRegisters(lhs: Sym[_], firstIterMap: Map[Sym[_], Bit], handle: BundleHandle): Sym[_] = {
    intakeRegisters(lhs).foreach {
      reg =>
        assert(!reg.isNonBuffer, s"Register $reg was marked nonBuffer -- this breaks when streamifying.")

        type RT = reg.RT
        implicit def bEV: Bits[RT] = reg.A.asInstanceOf[Bits[RT]]
        val castedReg = reg.asInstanceOf[Reg[RT]]

        // Get all triples which write to the register within LHS, but deduplicate in case there are multiple RegReads / RegWrites
        val allRelevant = memInfo.triples.filter { case RWTriple(_, reader, _) => reader.parent.s.contains(lhs) && reader.readMem.contains(reg) }.map {triple => (triple.writer.flatMap(_.parent.s), triple.edgeType)}.toSet
        dbgs(s"Relevant Triples: $allRelevant")

        val intakes = indent {
          allRelevant.toSeq map {
            case (Some(writer), edgeType) =>
              val (lca, writerPath, readerPath) = LCAWithPaths(writer.toCtrl, lhs.toCtrl)
              dbgs(s"Writer: $writer ; LCA: $lca ; writerPath: $writerPath ; readerPath: $readerPath")
              val fifo = getRegisterFIFO[RT](reg, writer, lhs)

              // we should read if it's the first iteration within the LCA
              val isFirstIterInLCA = getOutermostIter(readerPath.drop(1)).map(firstIterMap(_)).getOrElse(Bit(true))

              val en = edgeType match {
                case Forward => isFirstIterInLCA
                case Backward =>
                  // We read from the backward one if it's not the first iteration of the LCA
                  val isFirstIterOfLCA = getOutermostIter(lca.ancestors(reg.parent).reverse).map(firstIterMap(_)).get
                  isFirstIterInLCA & !isFirstIterOfLCA
              }
              (en, fifo.deq(en))
            case (None, Forward) =>
              // This counts as initialization
              val (_, _, pathToReg) = LCAWithPaths(reg.parent, lhs.toCtrl)
              val shouldInit = getOutermostIter(pathToReg.drop(1)).map(firstIterMap(_)).getOrElse(Bit(true))
              val init = castedReg match {case Op(RegNew(init)) => init.unbox}
              (shouldInit, init)
          }
        }

        dbgs(s"Intakes: ${intakes.mkString(", ")}")
        assert(intakes.map(_._1).toSet.size == intakes.size, s"Intake values should have distinct enable signals.")
        val wrVal = oneHotMux(intakes.map(_._1), intakes.map(_._2.asInstanceOf[RT]))
        val wrEn = intakes.map(_._1).reduceTree(_ | _)

        // mirror the reg
        val replacement = IR.withScope(handle) { mirrorSym(castedReg) }
        replacement.ctx = withPreviousCtx(replacement.ctx)

        // holdReg holds onto the output of the FIFODeq since it's enabled every few cycles
        val holdReg = IR.withScope(handle) { mirrorSym(castedReg) }
        holdReg.ctx = withPreviousCtx(holdReg.ctx)
        holdReg.explicitName = s"${castedReg.explicitName.getOrElse(castedReg.toString)}_${lhs}_holdReg"
        holdReg.unbox.nonbuffer

        holdReg.unbox.write(wrVal, wrEn)

        replacement.unbox.write(holdReg.unbox.value)
        register(reg -> replacement)
        regValues(reg) = mux(wrEn, wrVal, replacement.unbox.value)
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
          case RWTriple(Some(writer), _, _) =>
            // Writers within lhs that write to reg
            writer.parent.s.contains(lhs) && writer.writtenMem.contains(reg)
          case _ => false
        }.map {
          triple =>
            // Deduplicate based on reader's parent controller and edge type
            (triple.reader.parent.s.get, triple.edgeType)
        }.toSet
        dbgs(s"Relevant Triples: $allRelevant")

//        val wrData = f(castedReg).value
        val wrData = regValues(castedReg).asInstanceOf[RT]

        allRelevant.foreach {
          case (dest, edgeType) =>
            val fifo = getRegisterFIFO[RT](reg, lhs, dest)

            val (lca, writerPath, readerPath) = LCAWithPaths(lhs.toCtrl, dest.toCtrl)
            dbgs(s"Writer: $lhs ; LCA: $lca ; writerPath: $writerPath ; readerPath: $readerPath")

            val isLastIterWithinLCA = getOutermostIter(writerPath.drop(1)).map(lastIterMap(_)).getOrElse(Bit(true))
            val wrEnable = edgeType match {
              case Forward => isLastIterWithinLCA
              case Backward =>
                // we send the value backward if it isn't the last iteration of the LCA
                val isLastIterOfLCA = getOutermostIter(lca.ancestors(reg.parent).reverse).map(lastIterMap(_)).get
                isLastIterOfLCA & !isLastIterOfLCA
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
    val IterFIFOWithInfo(iterFIFO, iterToIndexMap) = createCounterGenerator(lhs)

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
      case None => Reg[Bit](Bit(false))
    }

    val foreachHandle = IR.getCurrentHandle()

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
        handleIntakeRegisters(lhs, firstIterMap.toMap, foreachHandle)
      }


      // Fetch Buffer Tokens

      // Restage the actual innards of the foreach
      foreachOp.block.stms.foreach {
        case rr@Op(RegRead(reg)) if regValues.contains(reg) =>
          dbgs(s"Forwarding read: $rr = ${rr.op.get} => ${regValues(reg)}")
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
      retimeGate()
      val endOfWorld = getOutermostIter(lhs.ancestors).map(lastIterMap(_)).getOrElse(Bit(true))
      stopWhen.write(endOfWorld, endOfWorld)

    }, Seq(foreverIter) ++ newInnerIters.map(_.unbox.asInstanceOf[I32]), Some(stopWhen))) {
      newForeach =>
        transferData(lhs, newForeach)
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case accelScope: AccelScope => inAccel {
      memInfo = computeMemInfoMap(lhs)
      dbgs(s"============MemInfoMap============")
      dbgs(memInfo.toDotGraph)
      dbgs(s"==================================")
      stageWithFlow(AccelScope(stageBlock {
        accelHandle = IR.getCurrentHandle()
        accelScope.block.nestedStms.foreach {
          case loop@Op(foreachOp: OpForeach) if inHw && loop.isInnerControl =>
            dbgs(s"Transforming Inner: $loop = $foreachOp")
            visitInnerForeach(loop, foreachOp)
          case mem if isInternalMem(mem) =>
            dbgs(s"Mirroring Internal Mem: $mem")
            super.visit(mem)
          case _ =>
        }
      })) {
        lhs2 => transferData(lhs, lhs2)
      }
    }

    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}


