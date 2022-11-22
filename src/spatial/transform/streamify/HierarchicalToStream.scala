package spatial.transform.streamify

import argon._
import argon.lang.Bits
import argon.node.IfThenElse
import argon.tags.struct
import argon.transform.ForwardTransformer
import forge.tags.stateful
import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.bounds.Final
import spatial.traversal.AccelTraversal
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.access._
import spatial.metadata.transform._
import spatial.node._
import spatial.util.TransformUtils.{isFirstIter, isLastIter, makeIters, willRunUT}
import spatial.util._

import scala.collection.{mutable => cm}

trait TypeCarrier {
  implicit def state: argon.State
  type T
  def bitsEV: Bits[this.T]
  def zero: Bits[this.T]
}

object TypeCarrier {
  @forge.tags.api def apply(mem: Sym[_]): TypeCarrier = {
    val IRState = implicitly[argon.State]
    mem match {
      case r: Reg[_] =>
        new TypeCarrier {
          override type T = r.RT
          override implicit val state: State = IRState
          override val bitsEV: Bits[T] = r.A
          override val zero: Bits[T] = r.A.zero.asInstanceOf[Bits[T]]
        }
    }
  }
}

/**
  * A PseudoIter represents an iteration variable that is sent along a FIFO in order to handle variable bounds
  * @param i: the value of the iterator
  * @param isFirst: whether this is the first iteration of this particular iterator
  * @param isLast: whether this is the last iteration of this particular iterator
  */
@struct case class PseudoIter[IType: Bits](i: IType, isFirst: Bit, isLast: Bit)

object PseudoIter {
  @stateful def makeProto(counter: Counter[_]): PseudoIter[_] = {
    implicit def bEV: Bits[counter.CT] = counter.CTeV.asInstanceOf[Bits[counter.CT]]
    proto(PseudoIter[counter.CT](counter.CTeV.asInstanceOf[Bits[counter.CT]].zero, Bit(false), Bit(false)))
  }
}

/**
  * A bundle of pseudoIters, representing the leading iterators of a variable-iteration controller
  * @param iters: a vector of PseudoIter
  */
@struct case class PseudoIters[IterVec:Bits](iters: IterVec)

case class StreamBundle(pIterType: VecStructType[Sym[Num[_]]], tokenType: VecStructType[Sym[_]], genToMainDepth: I32, genToReleaseDepth: I32, mainToReleaseDepth: I32)(implicit state: argon.State) {
  val genToMainIters = pIterType.VFIFO(genToMainDepth)
  val genToMainTokens = tokenType.VFIFO(genToMainDepth)

  val genToReleaseIters = pIterType.VFIFO(genToReleaseDepth)
  val genToReleaseTokens = tokenType.VFIFO(genToReleaseDepth)

  val mainToReleaseTokens = pIterType.VFIFO(mainToReleaseDepth)
  val source = FIFO[Bit](genToReleaseDepth)
}

case class HierarchicalToStream(IR: argon.State) extends ForwardTransformer with AccelTraversal {

  def willRunForAtLeastOneIter(counter: Counter[_]): Boolean = {
    (counter.start, counter.end, counter.step) match {
      case (Final(start), Final(end), Final(step)) =>
        if (step > 0) {
          start < end
        } else if (step < 0) {
          start > end
        } else {
          throw new IllegalArgumentException(s"Cannot have a step size of 0, encountered in $counter at ${counter.asSym.ctx}")
        }
      case _ => false
    }
  }

  private var edgeFIFOMap: Map[DependencyEdge, FIFO[_]] = Map.empty

  private lazy val edges: Set[DependencyEdge] = globals[DependencyEdges].get.edges.toSet

  type IMap = Map[Sym[Num[_]], Num[_]]
  class DependencyManager(var fetchedEdges: Set[DependencyEdge] = Set.empty, var currentValues: Map[Sym[_], Reg[_]] = Map.empty) {
    def updateValues(iteratorMap: IMap): Unit = {
      // Fetch all of the edges that the iterator map permits
      val remainingEdges = (edges -- fetchedEdges).filter(_.dstIterators.subsetOf(iteratorMap.keySet))
      fetchedEdges ++= remainingEdges
      val triplets = remainingEdges.toSeq.flatMap {
        edge =>
          dbgs(s"Iterator Map: $iteratorMap")
          dbgs(s"Required Iterators: ${edge}: ${edge.dstIterators}")
          val enabled = edge.dstRecv(TimeStamp(iteratorMap))
          val value = edgeFIFOMap(edge).deq(enabled)
          edge.dst.map {
            target => (target, enabled, value)
          }
      }
      triplets.groupBy(_._1).foreach {
        case (dst, valsAndEns) =>
          val newEns = valsAndEns.map(_._2)
          val firstValue = valsAndEns.head._3.asInstanceOf[Bits[_]]
          type VT = Bits[firstValue.R]
          implicit def bEV: Bits[VT] = firstValue.asInstanceOf[Bits[VT]]
          val newValues = valsAndEns.map(_._3).map {
            v => v.asInstanceOf[VT]
          }
          val result = oneHotMux(newEns, newValues)
          val isEnabled = newEns.reduceTree(_ & _)
          val newValue = currentValues.get(dst) match {
            case Some(reg: Reg[VT]) =>
              mux(isEnabled, result, reg.value)
            case None => result
          }
          // Create a register to hold this value
          val valueRegIntake = Reg[VT]
          valueRegIntake.write(newValue.asInstanceOf[VT], isEnabled)
          valueRegIntake.nonbuffer

          val valueRegBuf = Reg[VT]
          valueRegBuf := valueRegIntake.value
          currentValues += dst -> valueRegBuf
      }
    }
    def mirrorRecursive[T](s: Sym[T], cache: cm.Map[Sym[_], Sym[_]] = cm.Map.empty): (Sym[T], cm.Map[Sym[_], Sym[_]]) = {
      s match {
        case Op(RegRead(mem)) if currentValues contains mem =>
          // Don't cache reg reads
          (currentValues(mem).value.asInstanceOf[Sym[T]], cache)
        case _ if cache contains s => (cache(s).asInstanceOf[Sym[T]], cache)
        case ctr: Idx if ctr.getCounter.nonEmpty => (f(ctr), cache)
        case _ =>
          s.inputs.foreach(mirrorRecursive(_, cache))
          val mirrored = mirrorSym(s)
          cache += s -> mirrored
          (mirrored, cache)
      }
    }

    def copy(): DependencyManager = new DependencyManager(fetchedEdges, currentValues)
  }

  private def genCounterGen(ctrl: Sym[_], fifoBundle: StreamBundle) = {
    dbgs(s"Ancestors of $ctrl = ${ctrl.ancestors}")
    def recurse(currentAncestors: List[Ctrl], iteratorMap : IMap, dependencyManager: DependencyManager): Unit = {
      currentAncestors match {
        case Ctrl.Host :: Nil =>
          throw new UnsupportedOperationException(s"AccelScope was an inner controller, we shouldn't have done anything.")
        case Ctrl.Host :: remaining =>
          dbgs(s"Ignoring Host because there are inner controllers.")
          recurse(remaining, iteratorMap, dependencyManager)
        case Ctrl.Node(Op(AccelScope(_)), _) :: remaining =>
          dbgs(s"Peeling Accel Scope")
          recurse(remaining, iteratorMap, dependencyManager)
        case Ctrl.Node(outer@Op(UnitPipe(ens, block, None)), stage) :: remaining if ens.isEmpty =>
          // Controller
          dbgs(s"Peeling: $outer = UnitPipe(Set.empty, $block, None)")
          recurse(remaining, iteratorMap, dependencyManager)

        case Ctrl.Node(loop@Op(OpForeach(ens, cchain, block, iters, None)), stage) :: remaining =>
          dbgs(s"Peeling Foreach Loop: ${stm(loop)}")
          def stageRunBranch(): Void = {
            isolateSubst() {
              val newDependencyManager = dependencyManager.copy()
              val cache = cm.Map.empty[Sym[_], Sym[_]]
              val newChains = cchain.counters.map(newDependencyManager.mirrorRecursive(_, cache)._1).map(_.unbox)
              val newIters = makeIters(newChains).map(_.unbox.asInstanceOf[I32])
              val newCChain = CounterChain(newChains)
              val newEns = ens.map(dependencyManager.mirrorRecursive(_, cache)._1)
              stageWithFlow(OpForeach(newEns.map(_.unbox), newCChain, stageBlock {
                val newIMap = iters.map(_.asSym).zip(newIters)
                recurse(remaining, iteratorMap ++ newIMap, newDependencyManager)
              }, newIters, None)) {
                lhs2 =>
                  dbgs(s"CounterGen: $loop -> $lhs2")
              }
            }
          }

          if (cchain.isStatic && ens.isEmpty) {
            dbgs(s"Static Chain and empty Ens, continuing")
            stageRunBranch()
          } else {
            val tempIters = iters.map {
              iter => iter.asSym -> iter.counter.ctr.start.asInstanceOf[I32]
            }
            dependencyManager.updateValues(iteratorMap ++ tempIters)
            // Do a deep mirroring of the dependencies
            val willRun =
              isolateSubst() {
                val tempCache = cm.Map.empty[Sym[_], Sym[_]]
                val newEns = ens.map(dependencyManager.mirrorRecursive(_, tempCache)._1)
                val cchainDeps = cchain.counters.flatMap(_.inputs).map(dependencyManager.mirrorRecursive(_, tempCache)._1)
                register(tempCache.toSeq)
                val cchainWillRun = cchain.counters.map(willRunUT(_, this))
                val newEnsUnboxed = newEns.map(_.unbox).toSeq
                (cchainWillRun ++ newEnsUnboxed).reduceTree(_ & _)
            }

            stageWithFlow(IfThenElse(willRun, stageBlock {
              // True Branch
              stageRunBranch()
            }, stageBlock {
              // False Branch
              isolateSubst() {
                val newDependencyManager = dependencyManager.copy()
                val cache = cm.Map.empty[Sym[_], Sym[_]]
                throw new UnsupportedOperationException(s"Haven't implemented the false branch yet")
              }
            })) {
              ite =>
            }
          }

        case Nil =>
          // Done
          dbgs(s"Handling Inner")
          indent {
            dbgs(s"Acquired Iterators: $iteratorMap")
            dependencyManager.updateValues(iteratorMap)
            dbgs(s"Values: ${dependencyManager.currentValues}")
            dbgs(s"Fetched: ${dependencyManager.fetchedEdges}")

            fifoBundle.source.enq(Bit(true))

            val pIters = fifoBundle.pIterType((iteratorMap.map {
              case (iter, value) =>
                val counter: Counter[iter.R] = iter.unbox.counter.ctr.asInstanceOf[Counter[iter.R]]
                type TP = counter.CT

                implicit def tpEV: Num[TP] = counter.CTeV.asInstanceOf[Num[TP]]
                implicit def bitsEV: Bits[TP] = counter.CTeV.asInstanceOf[Bits[TP]]
                val isFirst = isFirstIter(iter.unbox.asInstanceOf[Num[TP]])
                val isLast = isLastIter(iter.unbox.asInstanceOf[Num[TP]])
                implicit def bitsEV2: Bits[Num[TP]] = counter.CTeV.asInstanceOf[Bits[Num[TP]]]
                iter -> PseudoIter[TP](value.asInstanceOf[TP], isFirst, isLast)
            }).toMap)
            fifoBundle.genToMainIters.enq(pIters)
            fifoBundle.genToReleaseIters.enq(pIters)

            val fetchedValues = dependencyManager.currentValues.mapValues(_.value.asInstanceOf[Bits[_]])
            fifoBundle.genToMainTokens.enq(fifoBundle.tokenType(fetchedValues, {
              case reg@Op(RegNew(init)) =>
                dbgs(s"Initializing ${stm(reg)} with value $init")
                init
            }))
          }
      }
    }

    recurse(ctrl.ancestors.toList, Map.empty, new DependencyManager())
  }

  private def computeDepth(edge: DependencyEdge): Int = 8

  private def createFIFOs(): Unit = {
    edges.foreach {
      case IDE@InferredDependencyEdge(src, dst, edgeType) =>
        val tCarrier = TypeCarrier(IDE.mem)
        implicit def bEV = tCarrier.bitsEV
        edgeFIFOMap += IDE -> FIFO[tCarrier.T](I32(computeDepth(IDE)))
        dbgs(s"Creating FIFO: $IDE -> ${stm(edgeFIFOMap(IDE))}")
    }
  }

  private def createInternalFIFOBundle(inner: Sym[_]): StreamBundle = {
    val genToMain = I32(8)
    val genToRelease = I32(16)
    val mainToRelease = I32(8)

    // FIFO Typing
    val counters = inner.ancestors.flatMap(_.cchains).flatMap(_.counters)
    val pIterType = VecStructType[Sym[Num[_]]](counters.map {
      counter => counter.iter.get.asInstanceOf[Sym[Num[_]]] -> PseudoIter.makeProto(counter)
    })

    val allAccessedMems = (inner.effects.reads ++ inner.effects.writes) intersect syncMems

    val tokenType = VecStructType[Sym[_]](allAccessedMems.toSeq map {
      mem => mem -> TypeCarrier(mem).zero
    })

    StreamBundle(pIterType, tokenType, genToMain, genToRelease, mainToRelease)
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

  private var syncMems: Set[Sym[_]] = null

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case _: AccelScope if lhs.isInnerControl => lhs
    case accel: AccelScope => inAccel {
      syncMems = (accel.block.nestedStms.filter{ x => x.isMem && !isInternalMem(x)}).toSet
      dbgs(s"Synchronizing: $syncMems")
      dbgs(s"Creating FIFOs:")
      indent { createFIFOs() }
      val result = super.transform(lhs, rhs)
      throw new Exception("Stop here for now")
      result
    }
    case _ if lhs.isInnerControl =>
      dbgs(s"Processing: Inner Controller ${stm(lhs)}")
      // Create internal bundles
      val bundle = createInternalFIFOBundle(lhs)
      indent {
        genCounterGen(lhs, bundle)
      }
      super.transform(lhs, rhs)
    case _: CounterNew[_] | _: CounterChainNew => dbgs(s"Skipping ${stm(lhs)}"); lhs
    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}
