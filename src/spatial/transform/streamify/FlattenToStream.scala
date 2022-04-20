package spatial.transform.streamify

import argon.transform.MutateTransformer
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

case class FlattenToStream(IR: State)(implicit isl: poly.ISL) extends MutateTransformer with AccelTraversal with spatial.util.CounterIterUpdateMixin {

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
            case Backward => "dotted"
          }

          s"$source:w -> $dest:w [style = $style]"
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
                val reachingWrites = reachingWritesToReg(reader, reg.writers)
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

  // (Source, Dest) -> FIFO
  private val regFIFOs = cm.Map[(Sym[_], Sym[_]), FIFO[_]]()

  private var accelHandle: BundleHandle = null

  def getRegisterFIFO[T: Bits](src: Sym[_], dest: Sym[_]): FIFO[T] = {
    (regFIFOs.get((src, dest)) match {
      case Some(fifo) => fifo
      case None =>
        IR.withScope(accelHandle) {
          regFIFOs((src, dest)) = FIFO[T](I32(16))
          regFIFOs((src, dest))
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
            val allNewIters = f(allOldIters)
            val oldItersAsI32 = allOldIters.map(_.unbox.asInstanceOf[I32])
            val isFirsts = isFirstIters(oldItersAsI32: _*)
            val isLasts = isLastIters(oldItersAsI32: _*)
            val indexData = allNewIters.zip(isFirsts.zip(isLasts)).map {
              case (iter, (first, last)) => PseudoIter(iter.unbox.asInstanceOf[I32], first, last)
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

  def handleIntakeRegisters(lhs: Sym[_], firstIterMap: Map[Sym[_], Bit]): Sym[_] = {
    val stms = lhs.blocks.flatMap(_.stms)
    intakeRegisters(lhs).foreach {
      reg =>
        assert(!reg.isNonBuffer, s"Register $reg was marked nonBuffer -- this breaks when streamifying.")

        type RT = reg.RT
        implicit def bEV: Bits[RT] = reg.A.asInstanceOf[Bits[RT]]
        val castedReg = reg.asInstanceOf[Reg[RT]]

        // Get all triples which write to the register within LHS, but deduplicate in case there are multiple RegReads
        val allRelevant = memInfo.triples.filter { case RWTriple(_, reader, _) => reader.parent.s.contains(lhs) && reader.readMem.contains(reg) }.map {triple => (triple.writer, triple.edgeType)}.toSet
        dbgs(s"Relevant Triples: $allRelevant")

        val intakes = indent {
          allRelevant.toSeq map {
            case (Some(writer), edgeType) =>
              val (lca, writerPath, readerPath) = LCAWithPaths(writer.parent, lhs.toCtrl)
              dbgs(s"Writer: $writer ; LCA: $lca ; writerPath: $writerPath ; readerPath: $readerPath")
              val fifo = getRegisterFIFO[RT](writer.parent.s.get, lhs)

              // we should read if it's the first iteration within the LCA
              val isFirstIterInLCA = getOutermostIter(readerPath.drop(1)).map(firstIterMap(_)).getOrElse(Bit(true))

              val en = edgeType match {
                case Forward => isFirstIterInLCA
                case Backward =>
                  // We read from the backward one if it's not the first iteration of the LCA
                  val isFirstIterOfLCA = getOutermostIter(lca.ancestors(reg.parent)).map(firstIterMap(_)).get
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

        dbgs(s"Intakes: $intakes")
        assert(intakes.map(_._1).toSet.size == intakes.size, s"Intake values should have distinct enable signals.")
        val wrVal = oneHotMux(intakes.map(_._1), intakes.map(_._2.asInstanceOf[RT]))
        val wrEn = intakes.map(_._1).reduceTree(_ | _)

        // mirror the reg
        val replacement = mirrorSym(castedReg)

        val holdReg = mirrorSym(castedReg)
        holdReg.explicitName = s"${castedReg.explicitName.getOrElse(castedReg.toString)}_${lhs}_holdReg"
        holdReg.unbox.nonbuffer

        holdReg.unbox.write(wrVal, wrEn)

        replacement.unbox.write(holdReg.unbox.value)
        register(reg -> replacement)
    }
  }

//  def handleOutputRegisters(lhs: argon.Sym[_], lastIterMap: Map[argon.Sym[_], Bit]): Unit = {
//    val stms = lhs.blocks.flatMap(_.stms)
//    outputRegisters(lhs).foreach {
//      reg =>
//        assert(!reg.isNonBuffer, s"Register $reg was marked nonBuffer -- this breaks when streamifying.")
//
//        type RT = reg.RT
//        implicit def bEV: Bits[RT] = reg.A.asInstanceOf[Bits[RT]]
//        val castedReg = reg.asInstanceOf[Reg[RT]]
//        val replacementReg = f(castedReg)
//
//        // Where do we write to?
//        val dependentReaders = reg.asSym.readers.flatMap {
//          reader =>
//            val (before, after) = reachingWritesToReg(reader, reg.writers)
//            // we're either a forward writer or a backedge (or not a reaching writer at all).
//            if (before.exists(stms.contains)) {
//              Seq((true, reader))
//            } else if (after.exists(stms.contains)) {
//              Seq((false, reader))
//            } else {
//              Seq.empty
//            }
//        }
//        dependentReaders.foreach {
//          case (isForward, reader) =>
//            val readerParent = reader.parent.s.get
//            val fwdFIFO = getRegisterFIFO[RT](lhs, readerParent)
//
//            val (lca, writerPath, readerPath) = LCAWithPaths(lhs.toCtrl, readerParent.toCtrl)
//            dbgs(s"Writer: $lhs -> LCA: $lca, writerPath: $writerPath, readerPath: $readerPath")
//            val isLastIterWithin = getOutermostIter(writerPath.drop(1)) match {
//              case Some(iter) => lastIterMap(iter)
//              case None => Bit(true)
//            }
//            val shouldWrite = if (isForward) {
//              // If we're a forward edge, then we write on the last iteration within the LCA
//              // (i.e. outermost iterator NOT part of the LCA)
//              isLastIterWithin
//            } else {
//              // If we're a backwards edge, then we write on every iteration within EXCEPT the last iter of the LCA
//              // We're guaranteed one somewhere between the LCA up to the memory's parent.
//              val isLastIterOfLCA = getOutermostIter(lca.ancestors.reverse) match {
//                case Some(iter) => lastIterMap(iter)
//              }
//              isLastIterWithin & !isLastIterOfLCA
//            }
//
//            fwdFIFO.enq(replacementReg.value, shouldWrite)
//        }
//    }
//  }

  private def visitInnerForeach(lhs: Sym[_], foreachOp: OpForeach): Sym[_] = {
    dbgs(s"Visiting Inner Foreach: $lhs = $foreachOp")
    val ancestralChains = lhs.ancestors.flatMap(_.cchains)
    val ancestralCounters = ancestralChains.flatMap(_.counters)

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
        handleIntakeRegisters(lhs, firstIterMap.toMap)
      }


      // Fetch Buffer Tokens

      // Restage the actual innards of the foreach
      foreachOp.block.stms.foreach(visit)


      // Release Register values if it was written by this controller
//      dbgs(s"Writing Register Values")
//      indent {
//        handleOutputRegisters(lhs, lastIterMap.toMap)
//      }

      // Release Buffer Tokens

    }, Seq(foreverIter) ++ newInnerIters.map(_.unbox.asInstanceOf[I32]), f(foreachOp.stopWhen))) {
      newForeach =>
        transferData(lhs, newForeach)
    }
  }

  private def visitForeach(lhs: Sym[_], foreachOp: OpForeach): Sym[_] = {
    // TODO: Compute initTokens
    val linearizedUses = computeLinearizedUses(lhs, foreachOp.block)
    lhs
  }

  /**
    * Captures a writer and all readers which see that value. We consider writers to kill the previous writer
    *   even if it is a conditional write. Here, we consider only inner pipelines as readers/writers
    *   instead of the actual Writer/Reader node.
    * @param writer -- The Inner Controller which writes to Mem
    * @param readers -- The Inner Controllers which read that value of Mem
    */
  case class LinearizedUse(writer: Option[Sym[_]], readers: cm.ArrayBuffer[Sym[_]])
  private def computeLinearizedUses(parent: Sym[_], block: Block[_]) = {
    val mems = block.internalMems
    val memUseMap = cm.Map[Sym[_], cm.ArrayBuffer[LinearizedUse]](
      (mems map {mem => (mem -> cm.ArrayBuffer[LinearizedUse]())}):_*
    )
    parent.nestedChildren.filter({x => x.isInnerControl || x.isCounter}).foreach {
      ctrl =>
        val allReads = ctrl.readMems ++ ctrl.writtenMems
        val allWrites = ctrl.writtenMems

        allReads.intersect(mems.toSet) foreach {
          mem =>
            val useMap = memUseMap(mem)
            useMap.lastOption match {
              case Some(uses) => uses.readers.append(ctrl.sym)
              case None => useMap.append(LinearizedUse(None, cm.ArrayBuffer(ctrl.sym)))
            }
        }

        allWrites.intersect(mems.toSet) foreach {
          mem =>
            memUseMap(mem).append(LinearizedUse(Some(ctrl.sym), cm.ArrayBuffer.empty))
        }
    }
    dbgs(s"Linearized Uses: $memUseMap")
    memUseMap
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case accelScope: AccelScope => inAccel {
      memInfo = computeMemInfoMap(lhs)
      dbgs(s"============MemInfoMap============")
      dbgs(memInfo.toDotGraph)
      dbgs(s"==================================")
      inCopyMode(true) {
        stageWithFlow(AccelScope(stageBlock {
          accelHandle = IR.getCurrentHandle()
          accelScope.block.stms.foreach(visit)
        })) {
          lhs2 => transferData(lhs, lhs2)
        }
      }
    }

    case foreachOp: OpForeach if inHw && lhs.isInnerControl =>
      dbgs(s"Transforming Inner: $lhs = $foreachOp")
      visitInnerForeach(lhs, foreachOp)

    case genericControl: Control[_] =>
      dbgs(s"Transforming Outer: $lhs = $genericControl")
      super.transform(lhs, rhs)

    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}


