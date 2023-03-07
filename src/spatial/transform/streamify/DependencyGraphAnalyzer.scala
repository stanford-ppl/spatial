package spatial.transform.streamify

import argon._
import forge.tags.stateful
import spatial.lang._
import spatial.node._

import scala.collection.{breakOut, mutable => cm}
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.access._
import spatial.traversal.AccelTraversal
import spatial.util.TransformUtils._
import spatial.util.{assertUniqueAndTake, crossJoin, oneAndOthers}
import spatial.util.modeling._

sealed trait EdgeType


object EdgeType {
  @stateful def apply(src: Ctrl, dst: Ctrl): EdgeType = {
    val lca = LCA(src, dst)
    getStageDistance(lca, src, dst) match {
      case None | Some(0) => Inner
      case Some(x) if x < 0 => Backward
      case Some(x) if x > 0 => Forward
    }
  }

  case object Forward extends EdgeType
  case object Backward extends EdgeType
  case object Inner extends EdgeType
  trait Initial extends EdgeType
  case class Initialize[T](v: T) extends Initial
  case object Return extends Initial
}

case class InferredDependencyEdge(src: Ctrl, dst: Ctrl, mem: Sym[_], edgeType: EdgeType)(implicit state: argon.State) extends DependencyEdge with CoherentEdge with StartEndEdge {

  override def isPseudoEdge: Boolean = edgeType match {
    case EdgeType.Initialize(v) => true
    case _ => false
  }

  lazy val (lca, pathSrc, pathDst) = LCAWithPaths(src, dst)
  // Order of loops from outside in
  private lazy val surroundingLoops = {
    val parentChain = lca.ancestors(mem.parent)
    // We recycle tokens as long as the write isn't killed by a different ones.
    // Because this is a back edge, any write between the src and dst above the LCA is a killer.
    // surroundingLoops is ordered inside-out
    val surroundingLoops = lca.ancestors(mem.parent).dropRight(1).reverse.takeWhile {
      case Ctrl.Node(ctrl, _) =>
        // Are there other children that write to mem?
        val hasOtherChildren = ctrl.children.exists {
          child =>
            !parentChain.contains(child) && (child match {
              case Ctrl.Node(s, _) => s.effects.writes contains mem
              case _ => false
            })
        }
        !hasOtherChildren
      case _ => false
    }
    surroundingLoops.reverse ++ Seq(lca)
  }

  private lazy val surroundingCounters = surroundingLoops.flatMap(_.cchains).flatMap(_.counters)
  private lazy val surroundingIters = surroundingCounters.flatMap(_.iter)
  private def isFirstSurroundingIter(ts: TimeStamp): Bit = ts.isFirst(surroundingIters.headOption).getOrElse(Bit(true))
  private def isLastSurroundingIter(ts: TimeStamp): Bit = ts.isLast(surroundingIters.headOption).getOrElse(Bit(true))

  private lazy val pathSrcIters = pathSrc.drop(1).flatMap(_.cchains).flatMap(_.counters).map(_.iter.get)

  override lazy val srcIterators: Set[Sym[Num[_]]] = {
    (edgeType match {
      case EdgeType.Forward | _:EdgeType.Initialize[_] => pathSrcIters
      case EdgeType.Backward | EdgeType.Return => surroundingCounters.map(_.iter.get) ++ pathSrcIters
      case EdgeType.Inner => Set.empty
    }).toSet.asInstanceOf[Set[Sym[Num[_]]]]
  }

  private lazy val pathDstIters = pathDst.drop(1).flatMap(_.cchains).flatMap(_.counters).map(_.iter.get)
  override lazy val dstIterators: Set[Sym[Num[_]]] = {
    (edgeType match {
      case EdgeType.Forward | _:EdgeType.Initial => pathDstIters
      case EdgeType.Backward => surroundingCounters.map(_.iter.get) ++ pathDstIters
      case EdgeType.Inner => Set.empty
    }).toSet.asInstanceOf[Set[Sym[Num[_]]]]
  }

  override def dstRecv(ts: TimeStamp)(implicit state: argon.State): Bit = {
    lazy val isFirstIterInLCA = ts.isFirst(pathDstIters.headOption).getOrElse(Bit(true))
    edgeType match {
      case EdgeType.Forward | _:EdgeType.Initial => isFirstIterInLCA
      case EdgeType.Backward =>
        if (surroundingCounters.nonEmpty) {
          isFirstIterInLCA & !isFirstSurroundingIter(ts)
        } else {
          Bit(false)
        }
      case EdgeType.Inner => Bit(true)
    }
  }

  override def srcSend(ts: TimeStamp)(implicit state: argon.State): Bit = {
//    lazy val isLastIterWithinLCA = isLastIter(ts, pathSrc.drop(1).flatMap(_.cchains).flatMap(_.counters))
    lazy val isLastIterWithinLCA = ts.isLast(pathSrcIters.headOption).getOrElse(Bit(true))
    edgeType match {
      case EdgeType.Forward | _:EdgeType.Initial => isLastIterWithinLCA
      case EdgeType.Backward =>
        // If we're a back-edge, There must be an outer loop. Find the outer loop where there isn't also another
        // access, and release as long as there isn't another access.
        isLastIterWithinLCA & !isLastSurroundingIter(ts)
      case EdgeType.Return =>
        isLastIterWithinLCA
      case EdgeType.Inner => Bit(true)
    }
  }

  override def toString: String = {
    s"CoherentEdge[$edgeType]($src -> $dst, $mem)"
  }
}

case class DependencyGraphAnalyzer(IR: State)(implicit isl: poly.ISL) extends AccelTraversal {

  private def isKilled(candidate: Ctrl, killer: Ctrl, observer: Ctrl): Boolean = {
    def isSequenced(ctrl: Ctrl): Boolean = {
      Seq(Sequenced, Pipelined) contains ctrl.schedule
    }
    // Check if candidate -> killer -> observer
    val (accessLCA, relDist) = LCAWithDataflowDistance(candidate, killer)
    if (!isSequenced(accessLCA)) {
      return false
    }

    val (jointLCA, jointDist) = LCAWithDataflowDistance(accessLCA, observer)

    // If the LCAs are the same (i.e. all 3 have the same LCA), then we need to check the relative orderings.
    if (accessLCA == jointLCA) {
      if (!isSequenced(jointLCA)) {
        return false
      }
      val candToObserver = getStageDistance(jointLCA, candidate, observer).get
      val killerToObserver = getStageDistance(jointLCA, killer, observer).get

      // cand -> observer, killer -> observer, cand -> killer
      return (candToObserver > 0, killerToObserver > 0, relDist > 0) match {
        case (true, true, candBeforeKill) =>
          // cand -> killer -> observer OR killer -> cand -> observer
          candBeforeKill
        case (true, false, _) => false // cand -> observer -> killer
        case (false, false, true) => true // observer -> cand -> killer
        case (true, true, false) => false // killer -> cand -> observer
        case (false, true, _) => true // killer -> observer -> cand
        case (false, false, false) => false // observer -> killer -> cand
      }
    } else {
      // If the killer is after the candidate in their LCA, and the LCA isn't shared with the observer, then
      // we know that it's (candidate -> killer) -> observer OR observer -> (candidate -> killer)
      // In either case, we know that the candidate is killed.

      // Case 1: (candidate, killer), observer
      // This can be detected by checking if accessLCA is a child of jointLCA
      if (accessLCA.hasAncestor(jointLCA)) {
        if (!isSequenced(accessLCA)) {
          return false
        }
        // In this case, we just need to know whether killer is after candidate
        return relDist > 0
      }

      // Case 2: (candidate, observer), killer
      val candObsLCA = LCA(candidate, observer)
      if (candObsLCA.hasAncestor(jointLCA) && candObsLCA != jointLCA) {
        if (!isSequenced(candObsLCA))
        // check whether candidate happens before observer
        // If the candidate happens before the observer, then it's not killed
        return getStageDistance(candObsLCA, candidate, observer).get < 0
      }

      // Case 3: (killer, observer), candidate
      val killerObsLCA = LCA(killer, observer)
      if (killerObsLCA.hasAncestor(jointLCA) && killerObsLCA != jointLCA) {
        if (!isSequenced(killerObsLCA)) return false
        // If the killer happens before the observer, then it kills any potential loopback
        return getStageDistance(killerObsLCA, killer, observer).get > 0
      }

      throw new Exception(s"This shouldn't be possible! Error occurred when checking isKilled($candidate, $killer, $observer")
    }
  }

  def computeDependencyGraph(mem: Sym[_]): Seq[_ <: DependencyEdge] = {

    dbgs(s"Computing graph for $mem = ${mem.op.get} (${mem.ctx})")
    indent {
      val allAccesses = mem.readers ++ mem.writers
      dbgs(s"Accesses: $allAccesses")
      // group accesses by their parent
      val groupedByParent = allAccesses.groupBy(_.parent)
      oneAndOthers(groupedByParent.toSeq).flatMap {
        case ((parent, accesses), others) =>
          dbgs(s"Parent: $parent Accesses: $accesses others: $others")
          val preceding = accesses.flatMap {
            access =>
              if (mem.isReg) {
                val otherWrites = others.flatMap(_._2).filter(_.isWriter).toSet
                reachingWritesToReg(access, otherWrites, writesAlwaysKill = true).map(_.parent)
              } else {
                // Does A happen before B with respect to C?
                val otherCtrls = others.map(_._1)
                // consider a ctrl to be killed by another ctrl if there exists another control in between
                val visibleCtrls = oneAndOthers(otherCtrls).flatMap {
                  case (candidate, killCtrls) if killCtrls.exists {killCtrl => isKilled(candidate, killCtrl, parent)} =>
                    None
                  case (candidate, killCtrls) =>
                    // Only consider a candidate if it isn't clearly killed.
                    val (lca, dist) = LCAWithStageDistance(candidate, parent)
                    if (dist > 0 || lca.willRunMultiple) {
                      Some(candidate)
                    } else {
                      None
                    }
                }
                visibleCtrls
              }
          }
          dbgs(s"Preceding: $preceding")
          val precedingEdges = preceding.map {
            otherParent => InferredDependencyEdge(otherParent, parent, mem, EdgeType(otherParent, parent))
          }

          val shouldInitialize = !precedingEdges.exists(_.edgeType == EdgeType.Forward)
          dbgs(s"Should Initialize: $shouldInitialize")
          if (shouldInitialize) {
            mem match {
              case Op(RegNew(init)) =>
                Set(InferredDependencyEdge(mem.parent, parent, mem, EdgeType.Initialize(init))) ++ precedingEdges
              case _ =>
                // If we're not a register, then we're passing tokens around instead.
                if (precedingEdges.exists(_.edgeType == EdgeType.Backward)) {
                  precedingEdges.map {
                    case ide@InferredDependencyEdge(src, dst, mem, EdgeType.Backward) => ide.copy(edgeType = EdgeType.Return)
                    case e => e
                  }
                } else {
                  // This memory literally only ever goes forward, which means that it should just be initialized
                  Set(InferredDependencyEdge(mem.parent, parent, mem, EdgeType.Initialize(I32(0)))) ++ precedingEdges
                }
            }
          } else precedingEdges
      }
    }
  }.toSeq

  var dependencyEdges: Seq[DependencyEdge] = Seq.empty

  def compressEdges(edges: Seq[DependencyEdge]): Seq[DependencyEdge] = {
    edges filter {
      case InferredDependencyEdge(_, _, _, EdgeType.Inner) => false
      case _ => true
    }
  }

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case AccelScope(blk) =>
      blk.nestedStms.filter(_.isMem).filter(StreamifyUtils.getConflictGroups(_).nonEmpty).foreach {
        mem =>
          val prodCons = computeDependencyGraph(mem)
          dbgs(s"${stm(mem)}-> $prodCons")
          if (prodCons.size > 1) {
            dependencyEdges ++= prodCons
          } else {
            dbgs(s"Eliding prodcons $prodCons")
          }
      }


    case _ => super.visit(lhs, rhs)
  }

  override def postprocess[R](block: Block[R]): Block[R] = {
    dependencyEdges.foreach {
      dEdge => dbgs(s"$dEdge")
    }

    globals.add(DependencyEdges(compressEdges(dependencyEdges)))
    dependencyEdges = Seq.empty
    super.postprocess(block)
  }
}
