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
import spatial.util.TransformUtils.{isFirstIter, isFirstIters, isLastIter, isLastIters, makeIter, makeIters, willRunUT}
import spatial.util._

import scala.collection.{mutable => cm}
import scala.reflect.runtime.universe.typeOf

trait BitsCarrier {
  type T
  def bitsEV: Bits[this.T]
  @stateful def zero: Bits[this.T]

  override def toString: String = {
    val typeString = bitsEV.typeName
    s"TypeCarrier { type T = ${typeString}, def bitsEV: $bitsEV}"
  }

  def =:=(other: BitsCarrier): Boolean = {
    bitsEV =:= other.bitsEV
  }
}

object BitsCarrier {
  def from[TP: Bits]: BitsCarrier = {
    val bEV: Bits[TP] = implicitly[Bits[TP]]

    new BitsCarrier {
      override type T = TP
      override val bitsEV: Bits[this.T] = bEV
      @stateful override def zero: Bits[this.T] = bitsEV.zero
    }
  }
}

object CoherentUtils {

  object intBitsCarrier extends BitsCarrier {
    override type T = I32
    override val bitsEV: Bits[T] = proto(I32(0))

    @stateful override def zero: Bits[T] = I32(0)
  }

  object boolBitsCarrier extends BitsCarrier {
    override type T = Bit
    override val bitsEV: Bits[T] = proto(Bit(false))

    @stateful override def zero: Bits[T] = Bit(false)
  }

  def MemToBitsCarrier(mem: Sym[_]): BitsCarrier = {
    mem match {
      case r: Reg[_] =>
        new BitsCarrier {
          override type T = r.RT
          override val bitsEV: Bits[T] = r.A

          @stateful override def zero: Bits[T] = r.A.zero.asInstanceOf[Bits[T]]
        }
      case m: SRAM[_, _] => intBitsCarrier
      case sr: StreamOut[_] => boolBitsCarrier
      case sr: StreamIn[_] => boolBitsCarrier
    }
  }
}

@struct case class TokenWithValid[TP: Bits](value: TP, valid: Bit) {
  def typeInfo: BitsCarrier = BitsCarrier.from[TP]
}

@struct case class PseudoIter[TP: Bits](iter: TP, isFirst: Bit, isLast: Bit) {
  def typeInfo: BitsCarrier = new BitsCarrier {
    override type T = TP
    override val bitsEV: Bits[this.T] = implicitly[Bits[TP]]

    @stateful override def zero: Bits[this.T] = bitsEV.zero
  }

  @stateful def toTimeTriplet: TimeTriplet = {
    TimeTriplet(iter.asInstanceOf[Num[_]], isFirst, isLast)
  }
}

case class StreamBundle(pIterType: MapStructType[Sym[Num[_]]], intakeTokenType: MapStructType[Sym[_]], outputTokenType: MapStructType[Sym[_]], genToMainDepth: I32, genToReleaseDepth: I32, mainToReleaseDepth: I32)(implicit state: argon.State) {
  val genToMainIters = pIterType.VFIFO(genToMainDepth)
  val genToMainTokens = intakeTokenType.VFIFO(genToMainDepth)

  val genToReleaseIters = pIterType.VFIFO(genToReleaseDepth)
  val genToReleaseTokens = outputTokenType.VFIFO(genToReleaseDepth)

  val mainToReleaseTokens = outputTokenType.VFIFO(mainToReleaseDepth)

  // True for main, False for bypass
  val source = FIFO[Bit](genToReleaseDepth)
}

class DependencyFIFOManager(implicit state: argon.State) {
  case class RegistryEntry(src: Ctrl, dst: Ctrl, edge: DependencyEdge, fifo: FIFO[_])
  private val fifoRegistry = cm.Set[RegistryEntry]()

  case class PseudoEdgeEntry(src: Ctrl, dst: Ctrl, edge: DependencyEdge)
  private val pseudoEdges = cm.Set[PseudoEdgeEntry]()

  def addEdge(edge: DependencyEdge): Unit = edge match {
    case coherent: CoherentEdge =>
      val tCarrier = CoherentUtils.MemToBitsCarrier(coherent.mem)
      implicit def beV: Bits[tCarrier.T] = tCarrier.bitsEV

      dbgs(s"Adding FIFOs for $edge")

      val targets = if (edge.dst.isInnerControl) {
        Seq(edge.dst)
      } else {
        edge.dst.nestedChildren.filter(_.isInnerControl)
      }
      dbgs(s"Targets: $targets")

      if (edge.isPseudoEdge) {
        targets foreach {
          inner =>
            dbgs(s"Registering PseudoEdge: $edge")
            pseudoEdges.add(PseudoEdgeEntry(edge.src, inner, edge))
        }
      } else {
        targets foreach {
          inner =>
            dbgs(s"Registering Edge: $edge")
            val newFIFO = FIFO[tCarrier.T](I32(computeDepth(coherent)))
            newFIFO.explicitName = s"FIFO_${edge.src.s.get}_${inner.s.get}_${coherent.mem}"
            fifoRegistry.add(RegistryEntry(edge.src, inner, edge, newFIFO))
        }
      }
  }

  def createBufferInits(): Unit = {
    'FIFOInitializers.Stream {
      fifoRegistry.collect {
        case entry@RegistryEntry(src, dst, edge: InferredDependencyEdge, fifo) if edge.edgeType == EdgeType.Return =>
          dbgs(s"Initializing entry: $entry")

      }
    }
  }

  def computeDepth(edge: DependencyEdge): Int = 8

  def getIntakeFIFOs(ctrl: Ctrl): Map[DependencyEdge, FIFO[_]] = (fifoRegistry.collect {
    case RegistryEntry(src, dst, edge, fifo) if dst == ctrl => edge -> fifo
  }).toMap

  def getPseudoIntakes(ctrl: Ctrl): Set[DependencyEdge] = pseudoEdges.filter(_.dst == ctrl).map(_.edge).toSet

  def getOutputFIFOs(ctrl: Ctrl): Map[DependencyEdge, Set[FIFO[_]]] = {
    fifoRegistry.filter(_.src == ctrl).groupBy(_.edge).mapValues(_.toSet.map{x: RegistryEntry => x.fifo})
  }

  def printEdges(): Unit = {
    fifoRegistry.foreach {
      case RegistryEntry(src, dst, edge, fifo) =>
        dbgs(s"$edge -> ${stm(fifo)}")
    }
  }
}

case class HierarchicalToStream(IR: argon.State) extends ForwardTransformer with AccelTraversal {

//  private var edgeFIFOMap: Map[(DependencyEdge, Ctrl), FIFO[_]] = Map.empty
  private val fifoManager = new DependencyFIFOManager()

  private lazy val edges: Set[DependencyEdge] = globals[DependencyEdges].get.edges.toSet

  private lazy val bufferDepths = {
    // In order to compute buffer depths, we need to count how many controllers there are on the critical path.
    // For now, just use the current buffer depth
    Map.empty[Sym[_], Int].withDefault {
      case mem: Reg[_] => 1
      case mem =>
        val parents = mem.accesses.map(_.parent)
        parents.size
    }
  }

  class DependencyManager(ctrl: Ctrl, var fetchedEdges: Set[DependencyEdge] = Set.empty, var currentValues: Map[Sym[_], Reg[_]] = Map.empty) {
    def copy(): DependencyManager = new DependencyManager(ctrl, fetchedEdges, currentValues)

    def fetchedCoherentEdges: Set[CoherentEdge] = fetchedEdges.collect {
      case edge: CoherentEdge => edge
    }

    def updateValues(timeStamp: TimeStamp): Unit = {
      // Fetch all of the edges that the iterator map permits
      val fetchable = fifoManager.getIntakeFIFOs(ctrl).filter {
        case (edge, _) => edge.dstIterators.subsetOf(timeStamp.support)
      }

      val pseudoEdges = (fifoManager.getPseudoIntakes(ctrl) -- fetchedEdges).filter(_.dstIterators.subsetOf(timeStamp.support))

      val remainingEdges = fetchable.filterNot(x => fetchedEdges.contains(x._1))
      fetchedEdges ++= remainingEdges.keys
      fetchedEdges ++= pseudoEdges

      dbgs(s"Fetching Edges: $remainingEdges")
      dbgs(s"Fetching initializers: $pseudoEdges")

      val triplets = remainingEdges.toSeq.map {
        case (edge: CoherentEdge, fifo) =>
          dbgs(s"Iterator Map: $timeStamp")
          dbgs(s"Required Iterators: ${edge}: ${edge.dstIterators}")
          val enabled = edge.dstRecv(timeStamp)
          val value = fifo.deq(enabled)
          (edge.mem, enabled, value)
      } ++ pseudoEdges.toSeq.map {
        case edge@InferredDependencyEdge(_, _, mem, EdgeType.Initialize(v)) =>
          dbgs(s"Inferred Initialization Edge: $mem = RegNew($v)")
          (mem, edge.dstRecv(timeStamp), v)
      }

      triplets.groupBy(_._1).foreach {
        case (mem, valsAndEns) =>
          val newEns = valsAndEns.map(_._2)
          val newValues = valsAndEns.map(_._3)
          val tInfo: BitsCarrier = CoherentUtils.MemToBitsCarrier(mem)
          val castedValues = newValues.map {
            case x: tInfo.T => x
          }
          implicit def bEV: Bits[tInfo.T] = tInfo.bitsEV
          val result = oneHotMux(newEns, castedValues)
          val isEnabled = newEns.reduceTree(_ | _)
          val newValue = currentValues.get(mem) match {
            case Some(reg: Reg[tInfo.T]) =>
              mux(isEnabled, result, reg.value)
            case None => result
          }
          // Create a register to hold this value
          val valueRegIntake = Reg[tInfo.T]
          valueRegIntake.explicitName = s"RegIntake_$mem"
          valueRegIntake.write(newValue, isEnabled)
          valueRegIntake.nonbuffer

          val valueRegBuf = Reg[tInfo.T]
          valueRegBuf.explicitName = s"RegIntakeBuf_$mem"
          valueRegBuf := valueRegIntake.value
          dbgs(s"Updating Current Values: $mem -> $valueRegBuf")
          currentValues += mem -> valueRegBuf
      }
    }
    def mirrorRecursive[T](s: Sym[T], cache: cm.Map[Sym[_], Sym[_]] = cm.Map.empty)(implicit ctx: argon.SrcCtx): (Sym[T], cm.Map[Sym[_], Sym[_]]) = {
      s match {
        case Op(RegRead(mem)) if currentValues contains mem =>
          // Don't cache reg reads
          dbgs(s"Fetching read from currentValues: ${stm(s)}")
          (currentValues(mem).value.asInstanceOf[Sym[T]], cache)
        case _ if cache contains s =>
          dbgs(s"Fetching from cache: ${stm(s)}")
          (cache(s).asInstanceOf[Sym[T]], cache)
        case iter: Idx if iter.getCounter.nonEmpty =>
          dbgs(s"Fetching iterator ${stm(iter)}")
          (f(iter), cache)
        case _ =>
          dbgs(s"Recursively fetching inputs for ${stm(s)}: ${s.inputs}")
          val newInputs = s.inputs.map(mirrorRecursive(_, cache)._1)
          val mirrored = isolateSubst() {
            register(s.inputs, newInputs)
            mirrorSym(s)
          }
          mirrored.ctx += ctx
          cache += s -> mirrored
          (mirrored, cache)
      }
    }

    def getTokens(currentTime: TimeStamp): Map[Sym[_], Bits[_]] = {
      (currentValues.mapValues(_.value).map {
        case (mem, value) =>
          dbgs(s"Computing Values For: ${currentValues}")
          val isEnabled = (fetchedCoherentEdges.filter(_.mem == mem).map {
            case edge: DependencyEdge => edge.dstRecv(currentTime)
          }).toSeq.reduceTree(_ | _)
          val typeCarrier = CoherentUtils.MemToBitsCarrier(mem)

          implicit def bitsEV: Bits[typeCarrier.T] = typeCarrier.bitsEV

          dbgs(s"Fetching: $mem -> value = ${stm(value.asInstanceOf[Sym[_]])} [isEnabled = ${stm(isEnabled)}]")
          mem -> TokenWithValid(value.asInstanceOf[typeCarrier.T], isEnabled).asInstanceOf[Bits[_]]
      }).toMap[Sym[_], Bits[_]]
    }
  }

  private def counterGen(ctrl: Sym[_], fifoBundle: StreamBundle): Unit = {
    dbgs(s"Ancestors of $ctrl = ${ctrl.ancestors}")

    def recurse(currentAncestors: List[Ctrl], timeMap : TimeMap, dependencyManager: DependencyManager): Unit = {
      // This is called when the loop will not run -- branch not taken, not enabled, etc.

      def terminate(dependencyManager: DependencyManager): Unit = {
        val remainingCounters = currentAncestors.flatMap(_.cchains).flatMap(_.counters)
        dbgs(s"Remaining Counters: $remainingCounters")
        // In order to "skip" iterations, we need to first get all of the iterators that are needed by the remaining edges
        val remainingEdges =  fifoManager.getIntakeFIFOs(ctrl.toCtrl).keySet -- dependencyManager.fetchedEdges

        dbgs(s"Remaining Edges: $remainingEdges")

        val (startEndEdges, generalEdges) = remainingEdges.partition {
          case _: StartEndEdge => true
          case _ => false
        }

        if (generalEdges.nonEmpty) {
          throw new NotImplementedError(s"Expected to not have remaining general edges, instead had: $generalEdges")
        }

        val support = startEndEdges.flatMap(_.dstIterators)

        dbgs(s"Support Iterators: $support")
        // If the source of the edge and ourselves are both inside the disabled LCA, then the source enqueued once, and we dequeue once.
        // If the source of the edge is *outside* the disabled LCA, then we need to dequeue as-normal (which will happen by default).

        // Tell the release to dequeue from bypass
        fifoBundle.source.enq(Bit(false))

        val cache = cm.Map.empty[Sym[_], Sym[_]]
        isolateSubst () {
          // Map all counters to their first iter in order to get all of the remaining edges

          remainingCounters.foreach {
            ctr =>
              val iter = ctr.iter.get
              register(iter -> dependencyManager.mirrorRecursive(ctr.start, cache)._1)
          }

          val triplets = remainingCounters.flatMap(_.iter).map(_.unbox).map {
            case iter: Num[_] => iter -> TimeTriplet(f(iter), Bit(true), Bit(true))
          }

          val newTime = timeMap ++ TimeMap(triplets)
          val tokens = (fifoManager.getIntakeFIFOs(ctrl.toCtrl).collect {
            case (edge: CoherentEdge with StartEndEdge, fifo) =>
              type TP = fifo.A.R
              implicit val tpEV: Bits[TP] = fifo.A.asInstanceOf[Bits[TP]]
              val tokenWithValid = TokenWithValid[TP](fifo.deq().asInstanceOf[TP], Bit(true))
              edge.mem -> tokenWithValid.asInstanceOf[Bits[_]]
          }).toMap[Sym[_], Bits[_]]

          val fetchedValues = dependencyManager.getTokens(newTime)

          val encodedTokens = fifoBundle.outputTokenType(tokens ++ fetchedValues)
          fifoBundle.genToReleaseTokens.enq(encodedTokens)
        }

        // Map all counters to their last iter in order to release the tokens as well
        isolateSubst() {
          val cache = cm.Map.empty[Sym[_], Sym[_]]
          remainingCounters.foreach {
            ctr =>
              register(ctr.iter.get -> dependencyManager.mirrorRecursive(ctr.end, cache)._1)
          }
          val triplets = remainingCounters.flatMap(_.iter).map(_.unbox).map {
            case iter: Num[_] => iter -> TimeTriplet(f(iter), Bit(true), Bit(true))
          }

          val newTime = timeMap ++ TimeMap(triplets)
          val pIters = fifoBundle.pIterType((newTime.triplets.map {
            case (iter: Sym[Num[_]], TimeTriplet(current: Bits[_], isFirst, isLast)) =>
              type CT = current.R
              implicit def bEV: Bits[CT] = current.asInstanceOf[Bits[CT]]
              iter -> PseudoIter(current.asInstanceOf[CT], isFirst, isLast)
          }).toMap)
          fifoBundle.genToReleaseIters.enq(pIters)
        }
      }

      currentAncestors match {
        case Ctrl.Host :: Nil =>
          throw new UnsupportedOperationException(s"AccelScope was an inner controller, we shouldn't have done anything.")
        case Ctrl.Host :: remaining =>
          dbgs(s"Ignoring Host because there are inner controllers.")
          recurse(remaining, timeMap, dependencyManager)
        case Ctrl.Node(Op(AccelScope(_)), _) :: remaining =>
          dbgs(s"Peeling Accel Scope")
          recurse(remaining, timeMap, dependencyManager)
        case Ctrl.Node(outer@Op(UnitPipe(ens, block, None)), stage) :: remaining if ens.isEmpty =>
          // Controller
          dbgs(s"Peeling: $outer = UnitPipe(Set.empty, $block, None)")
          recurse(remaining, timeMap, dependencyManager)

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
              register(iters -> newIters)
              register(cchain -> newCChain)
              register(cchain.counters -> newChains)
              stageWithFlow(OpForeach(newEns.map(_.unbox), newCChain, stageBlock {
                val newFirsts = isFirstIters(newIters)
                val newLasts = isLastIters(newIters)
                val newMap = TimeMap(iters.map(_.asSym).zip(newIters).zip(newFirsts).zip(newLasts).map {
                  case (((oldIter, newIter), newFirst), newLast) =>
                    oldIter -> TimeTriplet(newIter, newFirst, newLast)
                })
                recurse(remaining, timeMap ++ newMap, newDependencyManager)
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


            val tempMap = TimeMap(iters.map {
                iter =>
                  iter.asSym -> TimeTriplet(iter.counter.ctr.start.asInstanceOf[I32], Bit(true), Bit(false))
            })

            dependencyManager.updateValues(timeMap ++ tempMap)
            // Do a deep mirroring of the dependencies
            val willRun =
              isolateSubst() {
                val tempCache = cm.Map.empty[Sym[_], Sym[_]]
                val newEns = ens.map(dependencyManager.mirrorRecursive(_, tempCache)._1)
                val allInputs = cchain.counters.flatMap(_.inputs)
                val cchainDeps = allInputs.map(dependencyManager.mirrorRecursive(_, tempCache)._1)
                register(tempCache.toSeq)
                register(allInputs, cchainDeps)
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
                terminate(newDependencyManager)
              }
            })) {
              ite =>
            }
          }

        case Nil =>
          // Done
          dbgs(s"Handling Inner")
          indent {
            dbgs(s"Acquired Iterators: $timeMap")
            dependencyManager.updateValues(timeMap)
            dbgs(s"Values: ${dependencyManager.currentValues}")
            dbgs(s"Fetched: ${dependencyManager.fetchedEdges}")

            fifoBundle.source.enq(Bit(true))

            // compute isFirst and isLast for each iter in order
            val allIters = ctrl.ancestors.flatMap(_.cchains).flatMap(_.counters).flatMap(_.iter)
            val newIters = f(allIters)
            val isFirsts = isFirstIters(newIters.map(_.unbox.asInstanceOf[Num[_]]))
            val isLasts = isLastIters(newIters.map(_.unbox.asInstanceOf[Num[_]]))

            val pIterMap = allIters.zip(newIters.zip(isFirsts zip isLasts)).map {
              case (oldIter: Sym[Num[_]], (iter: Sym[Num[_]], (first, last))) =>
                val unboxed: Num[_] = iter.unbox
                type NT = unboxed.R
                implicit def bEV: Bits[NT] = unboxed.asInstanceOf[Bits[NT]]
                oldIter -> PseudoIter(unboxed.asInstanceOf[NT], first, last)
            }

            dbgs(s"Encoding PIters: $pIterMap")
            val pIters = fifoBundle.pIterType(pIterMap.toMap)
            fifoBundle.genToMainIters.enq(pIters)
            fifoBundle.genToReleaseIters.enq(pIters)

            val fetchedValues = (dependencyManager.currentValues.mapValues(_.value).map {
              case (mem, value) =>
                dbgs(s"Computing Values For: ${dependencyManager.currentValues}")
                val isEnabled = (dependencyManager.fetchedCoherentEdges.filter(_.mem == mem).map {
                  case edge: DependencyEdge => edge.dstRecv(timeMap)
                }).toSeq.reduceTree(_ | _)
                val typeCarrier = CoherentUtils.MemToBitsCarrier(mem)
                implicit def bitsEV: Bits[typeCarrier.T] = typeCarrier.bitsEV
                dbgs(s"Fetching: $mem -> value = ${stm(value.asInstanceOf[Sym[_]])} [isEnabled = ${stm(isEnabled)}]")
                mem -> TokenWithValid(value.asInstanceOf[typeCarrier.T], isEnabled).asInstanceOf[Bits[_]]
            }).toMap[Sym[_], Bits[_]]
            dbgs(s"Fetched Values: $fetchedValues")
            fifoBundle.genToMainTokens.enq(fifoBundle.intakeTokenType(fetchedValues))
          }
      }
    }

    Pipe {
      recurse(ctrl.ancestors.toList, TimeMap.empty, new DependencyManager(ctrl.toCtrl))
    }
  }

  private def mainGen(ctrl: Sym[_], fifoBundle: StreamBundle): Unit = {
    import fifoBundle._
    val ancestralChains = ctrl.ancestors.flatMap(_.cchains).flatMap(_.counters)
    val mirrorableChains = ancestralChains.reverse.takeWhile(_.isStatic).reverse

    val mirroredChains = mirrorableChains.map(mirrorSym(_))
    val oldIters = mirrorableChains.flatMap(_.iter)
    val mirroredIters = makeIters(mirroredChains.map(_.unbox))
    register(mirrorableChains, mirroredChains)
    register(mirrorableChains.flatMap(_.iter), mirroredIters)

    val needsForeverCtr = !ancestralChains.forall(_.isStatic)

    val (newIters, newCChain) = if (needsForeverCtr) {
      val foreverCtr = stage(ForeverNew())
      val newIters = Seq(makeIter(foreverCtr).unbox) ++ mirroredIters.map(_.unbox.asInstanceOf[I32])

      val newCChain = CounterChain(Seq(foreverCtr) ++ mirroredChains.map(_.unbox))
      (newIters, newCChain)
    } else {
      val newIters = mirroredIters.map(_.unbox.asInstanceOf[I32])
      val newCChain = CounterChain(mirroredChains.map(_.unbox))
      (newIters, newCChain)
    }

    val escapeScope = state.getCurrentHandle()

    stageWithFlow(OpForeach(Set.empty, newCChain, stageBlock {
      val pIters = fifoBundle.genToMainIters.deq()

      val tokens = fifoBundle.genToMainTokens.deq()

      pIters.unpack.foreach {
        case (iter, value: PseudoIter[_]) if oldIters contains iter =>
          dbgs(s"Iter: $iter -> $value [Ignored as it was mirrored already]")
        case (iter, value: PseudoIter[_]) =>
          dbgs(s"Iter: $iter -> $value")
          register(iter -> value.iter)
      }

      val tokenValues = cm.Map[Sym[_], Sym[_]]()
      tokens.unpack.foreach {
        case (mem: Reg[_], value: TokenWithValid[_]) =>

          // create an unbuffered register and immediately read it.
          val typeInfo: BitsCarrier = value.typeInfo

          implicit def bEV: Bits[typeInfo.T] = typeInfo.bitsEV
          val regName = mem.explicitName.getOrElse(mem.toString)

          val nonBufferReg = state.withScope(escapeScope) {
            val nonBufferReg = Reg[typeInfo.T]
            nonBufferReg.nonbuffer
            nonBufferReg.explicitName = s"NonBufferReg_${ctrl}_$regName"
            nonBufferReg
          }

          nonBufferReg.write(value.value.asInstanceOf[typeInfo.T], value.valid)

          val writesToReg = ctrl.writtenMems.contains(mem)
          dbgs(s"$ctrl writes to $mem: $writesToReg")
          if (writesToReg) {
            // If we write to the reg, then we have to mirror the reg
            val mirroredReg = state.withScope(escapeScope) {Reg[typeInfo.T]}
            mirroredReg.explicitName = s"MirroredReg_${ctrl}_$regName"

            val oldValue = mirroredReg.value
            tokenValues(mem) = mux[typeInfo.T](value.valid, nonBufferReg.value, oldValue)

            register(mem -> mirroredReg)
          } else {
            tokenValues(mem) = nonBufferReg.value.asSym
          }
          dbgs(s"Loading memory $mem [${value.typeInfo}]")
      }

      ctrl.blocks.flatMap(_.stms).foreach {
        case stmt@Op(RegRead(mem)) if tokenValues contains mem =>
          dbgs(s"Eliding RegRead($mem) with token value ${tokenValues(mem)}")
          register(stmt -> tokenValues(mem))
        case stmt@Op(RegWrite(mem: Reg[_], data, ens)) if tokenValues contains mem =>
          val alwaysEnabled = (ens.forall {
            case Const(c) => c.value
            case _ => false
          })
          if (alwaysEnabled) {
            tokenValues(mem) = f(data)
          } else {
            tokenValues(mem) = mux(f(ens).toSeq.reduceTree(_ & _), f(data).asInstanceOf[Bits[mem.RT]], tokenValues(mem).asInstanceOf[Bits[mem.RT]]).asInstanceOf[Sym[mem.RT]]
          }
          dbgs(s"Updating $mem <- ${stm(tokenValues(mem))}")
          // We still need to stage the write anyways
          val mirrored = f(mem).asInstanceOf[Reg[mem.RT]]
          mirrored.write(f(data).asInstanceOf[mem.RT], f(ens).toSeq:_*)
        case stmt =>
          val newStm = mirrorSym(stmt)
          newStm.ctx += implicitly[argon.SrcCtx]
          dbgs(s"Mirroring: ${stm(stmt)} -> ${stm(newStm)}")
          register(stmt -> newStm)
      }

      retimeGate()

      // write tokens out
      {
        val outputTokens = outputTokenType((outputTokenType.structFields.map {
          case (reg: Reg[_], tp: TokenWithValid[_]) =>
            val currentValue = tokenValues(reg)
            val typeCarrier: BitsCarrier = tp.typeInfo
            implicit def bEV: Bits[typeCarrier.T] = typeCarrier.bitsEV
            reg -> TokenWithValid(currentValue.asInstanceOf[typeCarrier.T], Bit(true))
        }).toMap)
        mainToReleaseTokens.enq(outputTokens)
      }

    }, newIters, None)) {
      newForeach =>
        newForeach.name = Some(s"Main_$ctrl")
    }
  }

  def releaseGen(lhs: Sym[_], fifoBundle: StreamBundle, stopWhen: Reg[Bit]): Unit = {
    import fifoBundle._

    Foreach(*) {
      ignore =>
        val tMap = TimeMap(genToReleaseIters.deq().unpack.map {
          case (iter, pIter: PseudoIter[_]) =>
          iter -> pIter.toTimeTriplet
        })
        dbgs(s"Recieved tMap: $tMap")

        val tokenSource = source.deq()

        val tokens: Map[Sym[_], TokenWithValid[_]] = {
          val mtr = mainToReleaseTokens.deq(tokenSource)
          val gtr = genToReleaseTokens.deq(!tokenSource)
          val recv: outputTokenType.MapStruct = mux[outputTokenType.MapStruct](tokenSource, mtr, gtr)
          recv.unpack.toMap.mapValues {
            case t: TokenWithValid[_] => t
          }
        }
        dbgs(s"Tokens: $tokens")

        tokens.foreach {
          case (s: Sym[_], twv: TokenWithValid[_]) =>
            val tpInfo = twv.typeInfo
            implicit def bEV: Bits[tpInfo.T] = tpInfo.bitsEV
            val debugReg = Reg[tpInfo.T]
            debugReg.dontTouch
            debugReg.explicitName = s"DebugReg_${lhs}_$s"
            debugReg.write(twv.value.asInstanceOf[Bits[_]].as[tpInfo.T], twv.valid)
        }

        fifoManager.getOutputFIFOs(lhs.toCtrl).foreach {
          case (edge: CoherentEdge, fifos) =>
            val shouldSend = edge.srcSend(tMap)
            val token = tokens(edge.mem)
            dbgs(s"Processing Edge: $edge -> Send: $shouldSend, token: $token")
            val read = token.value

            val typeInfo = token.typeInfo
            implicit def bEV: Bits[typeInfo.T] = typeInfo.bitsEV

            fifos.foreach {
              tf =>
                (tf, read) match {
                  case (fifo: FIFO[typeInfo.T], r: typeInfo.T) =>

                    debug.tagValue(r, s"EnqValue_${lhs}_${edge.mem}")

                    fifo.enq(r, shouldSend)
                }
            }
//            fifos.foreach {
//              fifo =>
//                assert(read.isInstanceOf[fifo.A.R])
//                fifo.asInstanceOf[FIFO[fifo.A.R]].enq(read.asInstanceOf[fifo.A.R], shouldSend)
//            }
        }

        retimeGate()

        tMap.triplets.headOption match {
          case None =>
            dbgs(s"No TimeMap found, stopping immediately.")
            stopWhen.write(Bit(true))
          case Some((_, TimeTriplet(_, _, isLast))) =>
            dbgs(s"Stopping on signal: $isLast")
            stopWhen.write(isLast, isLast)
        }
    }
  }

  private def createFIFOs(): Unit = {
    dbgs("=" * 10 + s"Adding FIFO Edges" + "=" * 10)
    edges.foreach(fifoManager.addEdge)
    fifoManager.printEdges()
    dbgs("="*30)
  }

  private var streamBundleRegistry = Map.empty[Ctrl, StreamBundle]

  private def createInternalFIFOBundle(inner: Sym[_]): StreamBundle = {
    val genToMain = I32(8)
    val genToRelease = I32(16)
    val mainToRelease = I32(8)

    // FIFO Typing
    val counters = inner.ancestors.flatMap(_.cchains).flatMap(_.counters)
    val pIterType = MapStructType[Sym[Num[_]]](counters.map {
      counter =>
        val iterSym = counter.iter.get.asInstanceOf[Sym[Num[_]]]
        implicit def bEV: Bits[counter.CT] = counter.CTeV.asInstanceOf[Bits[counter.CT]]
        iterSym -> proto(PseudoIter[counter.CT](counter.CTeV.zero.asInstanceOf[counter.CT], Bit(false), Bit(false)))
    }, typeName = Some(s"PIter_$inner"))

    // We create an intake token for each edge which has a destination inside us.
    val intakeEdges = edges.collect {
      case edge: CoherentEdge if edge.dst == inner.toCtrl => edge
    }
    val intakeTokenType = MapStructType[Sym[_]](intakeEdges.map {
      coherent: CoherentEdge =>
        val carrier: BitsCarrier = CoherentUtils.MemToBitsCarrier(coherent.mem)

        implicit def bEV: Bits[carrier.T] = carrier.bitsEV

        val packed = TokenWithValid[carrier.T](carrier.zero.as[carrier.T], Bit(false))

        coherent.mem -> packed.asInstanceOf[Bits[_]]
    }.toSeq, typeName = Some(s"IntakeTokenType_$inner"))

    val releaseEdges = edges.collect {
      case edge: CoherentEdge if edge.src == inner.toCtrl => edge
    }
    val releaseTokenType = MapStructType[Sym[_]](releaseEdges.map {
      coherent: CoherentEdge =>
        val carrier: BitsCarrier = CoherentUtils.MemToBitsCarrier(coherent.mem)

        implicit def bEV: Bits[carrier.T] = carrier.bitsEV

        val packed = TokenWithValid[carrier.T](carrier.zero.as[carrier.T], Bit(false))

        coherent.mem -> packed.asInstanceOf[Bits[_]]
    }.toSeq, typeName = Some(s"ReleaseTokenType_$inner"))

    val streamBundle = StreamBundle(pIterType, intakeTokenType, releaseTokenType, genToMain, genToRelease, mainToRelease)
    streamBundleRegistry += inner.toCtrl -> streamBundle
    streamBundle.genToReleaseTokens.explicitName = s"${inner}_g2rTokens"
    streamBundle.genToReleaseIters.explicitName = s"${inner}_g2rIter"
    streamBundle.genToMainTokens.explicitName = s"${inner}_g2mTokens"
    streamBundle.genToMainIters.explicitName = s"${inner}_g2mIters"
    streamBundle.source.explicitName = s"${inner}_source"
    streamBundle.mainToReleaseTokens.explicitName = s"${inner}_m2rTokens"
    streamBundle
  }

  def isInternalMem(mem: Sym[_]): Boolean = {
    val allAccesses = (mem.readers union mem.writers)
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

  private var syncMems: Set[Sym[_]] = Set.empty

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case _: AccelScope if lhs.isInnerControl => lhs

    case accel: AccelScope => inAccel {
      stage(AccelScope(stageBlock {
        syncMems = (accel.block.nestedStms.filter { x => x.isMem && !isInternalMem(x) }).toSet
        dbgs(s"Synchronizing: $syncMems")
        dbgs(s"Creating FIFOs:")
        indent {
          createFIFOs()
        }
        Stream {
          inlineBlock(accel.block)
        }
      }))
    }

    case acc: Accessor[_, _] if syncMems contains acc.mem =>
      dbgs(s"Skipping: $lhs = $rhs since ${acc.mem} is in syncMems")
      lhs

    case acc: Accessor[_, _] =>
      dbgs(s"Not skipping $lhs = $rhs since ${acc.mem} is not in $syncMems")
      super.transform(lhs, rhs)

    case _ if lhs.isInnerControl && inHw =>
      dbgs(s"Processing: Inner Controller ${stm(lhs)}")
      // Create internal bundles

      val stopWhen = Reg[Bit]
      stopWhen.explicitName = s"${lhs}_stop"

      stageWithFlow(UnitPipe(Set.empty, stageBlock {
        val bundle = createInternalFIFOBundle(lhs)
        indent {
          dbgs(s"Creating CounterGen")
          isolateSubst() {
            indent {
              counterGen(lhs, bundle)
            }
          }
          dbgs(s"Creating Main")
          isolateSubst() {
            indent {
              mainGen(lhs, bundle)
            }
          }

          dbgs(s"Creating Release")
          isolateSubst() {
            indent {
              releaseGen(lhs, bundle, stopWhen)
            }
          }
        }
      }, Some(stopWhen))) {
        lhs2 =>
          lhs2.userSchedule = Streaming
      }
    case _: CounterNew[_] | _: CounterChainNew => dbgs(s"Skipping ${stm(lhs)}"); lhs

    case ctrlOp: Control[_] if inHw && lhs.isOuterControl =>
      dbgs(s"Skipping Control: $lhs = $ctrlOp")
      stageWithFlow(UnitPipe(Set.empty, stageBlock {
        ctrlOp.blocks.foreach(inlineBlock(_))
        spatial.lang.void
      }, None)) {
        lhs2 =>
          transferDataIfNew(lhs, lhs2)
          lhs2.userSchedule = Streaming
      }

    case _ if syncMems contains lhs =>
      dbgs(s"Skipping $lhs since it's in syncMems")
      lhs

    case _ =>
      if (inHw) { dbgs(s"Default Mirroring: ${stm(lhs)}") }
      super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]

  override def postprocess[R](block: Block[R]): Block[R] = {
    val blk = super.postprocess(block)
    blk
  }
}
