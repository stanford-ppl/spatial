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
  def MemToBitsCarrier(mem: Sym[_]): BitsCarrier = {
    mem match {
      case r: Reg[_] =>
        new BitsCarrier {
          override type T = r.RT
          override val bitsEV: Bits[T] = r.A

          @stateful override def zero: Bits[T] = r.A.zero.asInstanceOf[Bits[T]]
        }
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

case class StreamBundle(pIterType: VecStructType[Sym[Num[_]]], intakeTokenType: VecStructType[Sym[_]], outputTokenType: VecStructType[Sym[_]], genToMainDepth: I32, genToReleaseDepth: I32, mainToReleaseDepth: I32)(implicit state: argon.State) {
  val genToMainIters = pIterType.VFIFO(genToMainDepth)
  val genToMainTokens = intakeTokenType.VFIFO(genToMainDepth)

  val genToReleaseIters = pIterType.VFIFO(genToReleaseDepth)
  val genToReleaseTokens = outputTokenType.VFIFO(genToReleaseDepth)

  val mainToReleaseTokens = outputTokenType.VFIFO(mainToReleaseDepth)
  val source = FIFO[Bit](genToReleaseDepth)
}

class DependencyFIFOManager(implicit state: argon.State) {
  case class RegistryEntry(src: Ctrl, dst: Ctrl, edge: DependencyEdge, fifo: FIFO[_])
  private val fifoRegistry = cm.Set[RegistryEntry]()

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

      targets foreach {
        inner =>
          val newFIFO = FIFO[tCarrier.T](I32(computeDepth(coherent)))
          newFIFO.explicitName = s"FIFO_${edge.src}_${inner}_${coherent.mem}"
          fifoRegistry.add(RegistryEntry(edge.src, inner, edge, newFIFO))
      }
  }

  def computeDepth(edge: DependencyEdge): Int = 8

  def getIntakeFIFOs(ctrl: Ctrl): Map[DependencyEdge, FIFO[_]] = (fifoRegistry.collect {
    case RegistryEntry(src, dst, edge, fifo) if dst == ctrl => edge -> fifo
  }).toMap

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
  private var fifoManager = new DependencyFIFOManager()

  private lazy val edges: Set[DependencyEdge] = globals[DependencyEdges].get.edges.toSet

  type IMap = Map[Sym[Num[_]], Num[_]]
  class DependencyManager(ctrl: Ctrl, var fetchedEdges: Set[(DependencyEdge, FIFO[_])] = Set.empty, var currentValues: Map[Sym[_], Reg[_]] = Map.empty) {

    def fetchedCoherentEdges: Set[CoherentEdge] = fetchedEdges.collect {
      case (edge: CoherentEdge, _) => edge
    }

    def updateValues(timeStamp: TimeStamp): Unit = {
      // Fetch all of the edges that the iterator map permits
      val fetchable = fifoManager.getIntakeFIFOs(ctrl).filter {
        case (edge, _) => edge.dstIterators.subsetOf(timeStamp.support)
      }

      dbgs(s"Fetchable: $fetchable")
//      val remainingEdges = (edges -- fetchedEdges).filter(_.dstIterators.subsetOf(iteratorMap.keySet))
      val remainingEdges = fetchable.filterNot(fetchedEdges.contains)
      fetchedEdges ++= remainingEdges
      val triplets = remainingEdges.toSeq.flatMap {
        case (edge: CoherentEdge, fifo) =>
          dbgs(s"Iterator Map: $timeStamp")
          dbgs(s"Required Iterators: ${edge}: ${edge.dstIterators}")
          val enabled = edge.dstRecv(timeStamp)
          val value = fifo.deq(enabled)
          Seq((edge.mem, enabled, value))
      }
      triplets.groupBy(_._1).foreach {
        case (mem, valsAndEns) =>
          val newEns = valsAndEns.map(_._2)
          val firstValue = valsAndEns.head._3.asInstanceOf[Bits[_]]
          type VT = Bits[firstValue.R]
          implicit def bEV: Bits[VT] = firstValue.asInstanceOf[Bits[VT]]
          val newValues = valsAndEns.map(_._3).map {
            v => v.asInstanceOf[VT]
          }
          val result = oneHotMux(newEns, newValues)
          val isEnabled = newEns.reduceTree(_ & _)
          val newValue = currentValues.get(mem) match {
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
          dbgs(s"Updating Current Values: $mem -> $valueRegBuf")
          currentValues += mem -> valueRegBuf
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

    def copy(): DependencyManager = new DependencyManager(ctrl, fetchedEdges, currentValues)
  }

  private def counterGen(ctrl: Sym[_], fifoBundle: StreamBundle) = {
    dbgs(s"Ancestors of $ctrl = ${ctrl.ancestors}")
    def recurse(currentAncestors: List[Ctrl], timeMap : TimeMap, dependencyManager: DependencyManager): Unit = {
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

            val pIterMap = newIters.zip(isFirsts zip isLasts).map {
              case (iter: Sym[Num[_]], (first, last)) =>
                val unboxed: Num[_] = iter.unbox
                type NT = unboxed.R
                implicit def bEV: Bits[NT] = unboxed.asInstanceOf[Bits[NT]]
                iter -> PseudoIter(unboxed.asInstanceOf[NT], first, last)
            }

            val pIters = fifoBundle.pIterType(pIterMap.toMap)
            fifoBundle.genToMainIters.enq(pIters)
            fifoBundle.genToReleaseIters.enq(pIters)

            val fetchedValues = (dependencyManager.currentValues.mapValues(_.value).map {
              case (mem, value) =>
                val isEnabled = (dependencyManager.fetchedCoherentEdges.filter(_.mem == mem).map {
                  case edge: DependencyEdge => edge.dstRecv(timeMap)
                }).toSeq.reduceTree(_ | _)
                val typeCarrier = CoherentUtils.MemToBitsCarrier(mem)
                implicit def bitsEV: Bits[typeCarrier.T] = typeCarrier.bitsEV
                mem -> TokenWithValid(value.asInstanceOf[typeCarrier.T], isEnabled).asInstanceOf[Bits[_]]
            }).toMap[Sym[_], Bits[_]]
            fifoBundle.genToMainTokens.enq(fifoBundle.intakeTokenType(fetchedValues))
          }
      }
    }

    recurse(ctrl.ancestors.toList, TimeMap.empty, new DependencyManager(ctrl.toCtrl))
  }

  private def mainGen(ctrl: Sym[_], fifoBundle: StreamBundle): Unit = {
    import fifoBundle._
    val mirroredRegs = (intakeTokenType.structFields.map {
      case (key, tpProto: TokenWithValid[_]) =>
        val typeInfo: BitsCarrier = tpProto.typeInfo

        implicit def bEV: Bits[typeInfo.T] = typeInfo.bitsEV
        val holdReg = Reg[typeInfo.T]

        val regName = key.explicitName.getOrElse(key.toString)

        holdReg.explicitName = s"HoldReg_${ctrl}_${regName}"

        val nonBufferReg = Reg[typeInfo.T]
        nonBufferReg.nonbuffer
        nonBufferReg.explicitName = s"NonBufferReg_${ctrl}_$regName"

        key -> (holdReg, nonBufferReg)
    }).toMap[Sym[_], (Reg[_], Reg[_])]

    val mirrorableChains = {
      val ancestralChains = ctrl.ancestors.flatMap(_.cchains).flatMap(_.counters)
      ancestralChains.reverse.takeWhile(_.isStatic).reverse
    }
    val mirroredChains = mirrorableChains.map(mirrorSym(_))
    val oldIters = mirrorableChains.flatMap(_.iter)
    val mirroredIters = makeIters(mirroredChains.map(_.unbox))
    register(mirrorableChains, mirroredChains)
    register(mirrorableChains.flatMap(_.iter), mirroredIters)

    val foreverCtr = stage(ForeverNew())
    val newIters = Seq(makeIter(foreverCtr).unbox) ++ mirroredIters.map(_.unbox.asInstanceOf[I32])

    val newCChain = CounterChain(Seq(foreverCtr) ++ mirroredChains.map(_.unbox))
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

      tokens.unpack.foreach {
        case (mem, value: TokenWithValid[_]) =>
          // First, we buffer the value into an unbuffered hold register
          val (reg, tmp) = mirroredRegs(mem)
          tmp.asInstanceOf[Reg[tmp.RT]].write(value.value.asInstanceOf[tmp.RT], value.valid)
          reg.asInstanceOf[Reg[reg.RT]].write(tmp.value.asInstanceOf[reg.RT])

          mem match {
            case _:Reg[_] => register(mem -> reg)
            case _ =>
          }

          dbgs(s"Loading memory $mem [${value.typeInfo}]")
      }

      ctrl.blocks.flatMap(_.stms).foreach {
        case stm => register(stm -> mirrorSym(stm))
      }

      retimeGate()

      // write tokens out
      {
        val outputTokens = outputTokenType((outputTokenType.structFields.map {
          case (field, tp: TokenWithValid[_]) =>
            val currentValue = mirroredRegs(field)._1.value
            val typeCarrier: BitsCarrier = tp.typeInfo
            implicit def bEV: Bits[typeCarrier.T] = typeCarrier.bitsEV
            field -> TokenWithValid(currentValue.asInstanceOf[typeCarrier.T], Bit(true))
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
          val recv: outputTokenType.VecStruct = mux[outputTokenType.VecStruct](tokenSource, mtr, gtr)
          recv.unpack.toMap.mapValues {
            case t: TokenWithValid[_] => t
          }
        }
        dbgs(s"Tokens: $tokens")

        fifoManager.getOutputFIFOs(lhs.toCtrl).foreach {
          case (edge: CoherentEdge, fifos) =>
            val shouldSend = edge.srcSend(tMap)
            val token = tokens(edge.mem)
            fifos.foreach {
              fifo => fifo.asInstanceOf[FIFO[fifo.A.R]].enq(token.asInstanceOf[fifo.A.R], shouldSend)
            }
        }
    }
  }

  private def createFIFOs(): Unit = {
    dbgs("=" * 10 + s"Adding FIFO Edges" + "=" * 10)
    edges.foreach(fifoManager.addEdge(_))
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
    val pIterType = VecStructType[Sym[Num[_]]](counters.map {
      counter =>
        val iterSym = counter.iter.get.asInstanceOf[Sym[Num[_]]]
        implicit def bEV: Bits[counter.CT] = counter.CTeV.asInstanceOf[Bits[counter.CT]]
        iterSym -> proto(PseudoIter[counter.CT](counter.CTeV.zero.asInstanceOf[counter.CT], Bit(false), Bit(false)))
    })

    // We create an intake token for each edge which has a destination inside us.
    val intakeEdges = edges.collect {
      case edge: CoherentEdge if edge.dst == inner.toCtrl => edge
    }
    val intakeTokenType = VecStructType[Sym[_]](intakeEdges.map {
      coherent: CoherentEdge =>
        val carrier: BitsCarrier = CoherentUtils.MemToBitsCarrier(coherent.mem)

        implicit def bEV: Bits[carrier.T] = carrier.bitsEV

        val packed = TokenWithValid[carrier.T](carrier.zero.as[carrier.T], Bit(false))

        coherent.mem -> packed.asInstanceOf[Bits[_]]
    }.toSeq)

    val releaseEdges = edges.collect {
      case edge: CoherentEdge if edge.src == inner.toCtrl => edge
    }
    val releaseTokenType = VecStructType[Sym[_]](releaseEdges.map {
      coherent: CoherentEdge =>
        val carrier: BitsCarrier = CoherentUtils.MemToBitsCarrier(coherent.mem)

        implicit def bEV: Bits[carrier.T] = carrier.bitsEV

        val packed = TokenWithValid[carrier.T](carrier.zero.as[carrier.T], Bit(false))

        coherent.mem -> packed.asInstanceOf[Bits[_]]
    }.toSeq)

    val streamBundle = StreamBundle(pIterType, intakeTokenType, releaseTokenType, genToMain, genToRelease, mainToRelease)
    streamBundleRegistry += inner.toCtrl -> streamBundle
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

  private var syncMems: Set[Sym[_]] = _

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case _: AccelScope if lhs.isInnerControl => lhs
    case accel: AccelScope => inAccel {
      syncMems = (accel.block.nestedStms.filter{ x => x.isMem && !isInternalMem(x)}).toSet
      dbgs(s"Synchronizing: $syncMems")
      dbgs(s"Creating FIFOs:")
      indent { createFIFOs() }
      val result = super.transform(lhs, rhs)
      result
    }
    case _ if lhs.isInnerControl =>
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

      }
    case _: CounterNew[_] | _: CounterChainNew => dbgs(s"Skipping ${stm(lhs)}"); lhs
    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]

  override def postprocess[R](block: Block[R]): Block[R] = {
    val blk = super.postprocess(block)
//    throw new Exception("Stop here for now")
    blk
  }
}
