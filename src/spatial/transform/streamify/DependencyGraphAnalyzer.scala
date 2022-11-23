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
case object Forward extends EdgeType
case object Backward extends EdgeType
case object Return extends EdgeType
case object Inner extends EdgeType
case object Initialize extends EdgeType

object EdgeType {
  @stateful def apply(src: Sym[_], dst: Sym[_]): EdgeType = {
    val lca = LCA(src, dst)
    getStageDistance(lca, src, dst) match {
      case None | Some(0) => Inner
      case Some(x) if x < 0 => Backward
      case Some(x) if x > 0 => Forward
    }
  }
}

case class InferredDependencyEdge(src: Set[Sym[_]], dst: Set[Sym[_]], edgeType: EdgeType)(implicit state: argon.State) extends DependencyEdge with CoherentEdge {
  // check that all srcs have the same parent, and all dsts have the same parent
  private val srcParent = assertUniqueAndTake(src.map(_.parent))

  private val dstParent = assertUniqueAndTake(dst.map(_.parent))

  lazy val (lca, pathSrc, pathDst) = LCAWithPaths(srcParent, dstParent)

  override lazy val mem: Sym[_] = {
    assertUniqueAndTake((src ++ dst).flatMap(_.accessedMem))
  }

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

  lazy val pathSrcIters = pathSrc.drop(1).flatMap(_.cchains).flatMap(_.counters).map(_.iter.get)

  override lazy val srcIterators: Set[Sym[Num[_]]] = {
    (edgeType match {
      case Forward | Initialize => pathSrcIters
      case Backward => surroundingCounters.map(_.iter.get) ++ pathSrcIters
      case Inner => Set.empty
//      case Return =>
    }).toSet.asInstanceOf[Set[Sym[Num[_]]]]
  }

  lazy val pathDstIters = pathDst.drop(1).flatMap(_.cchains).flatMap(_.counters).map(_.iter.get)
  override lazy val dstIterators: Set[Sym[Num[_]]] = {
    (edgeType match {
      case Forward | Initialize => pathDstIters
      case Backward => surroundingCounters.map(_.iter.get) ++ pathDstIters
      case Inner => Set.empty
//      case Return =>
    }).toSet.asInstanceOf[Set[Sym[Num[_]]]]
  }

  override def dstRecv(ts: TimeStamp)(implicit state: argon.State): Bit = {
    val pathToLCA = pathDst.drop(1)
    // Are we the first iteration of the LCA?
    lazy val isFirstIterInLCA = isFirstIter(ts, pathToLCA.flatMap(_.cchains).flatMap(_.counters))
    edgeType match {
      case Forward | Initialize => isFirstIterInLCA
      case Backward =>
        if (surroundingCounters.nonEmpty) {
          isFirstIterInLCA & !isFirstIter(ts, surroundingCounters)
        } else {
          Bit(false)
        }
      case Return =>
        // On a Return Edge, we assume that the FIFO was either pre-populated or had a value written during
        // the previous outer iteration.
        isFirstIterInLCA
      case Inner => Bit(true)
    }
  }

  override def srcSend(ts: TimeStamp)(implicit state: argon.State): Bit = {
    lazy val isLastIterWithinLCA = isLastIter(ts, pathSrc.drop(1).flatMap(_.cchains).flatMap(_.counters))
    edgeType match {
      case Forward | Initialize => isLastIterWithinLCA
      case Backward =>
        // If we're a back-edge, There must be an outer loop. Find the outer loop where there isn't also another
        // access, and release as long as there isn't another access.
        isLastIterWithinLCA & !isLastIter(ts, surroundingCounters)
      case Return =>
        isLastIterWithinLCA
      case Inner => Bit(true)
    }
  }

  override def toString: String = {

    s"CoherentEdge[$edgeType](${src.mkString(", ")} -> ${dst.mkString(", ")})"
  }
}

case class DependencyGraphAnalyzer(IR: State)(implicit isl: poly.ISL) extends AccelTraversal {

  def computeDependencyGraph(mem: Sym[_]): Seq[_ <: DependencyEdge] = {

    dbgs(s"Computing graph for $mem = ${mem.op.get} (${mem.ctx})")
    indent {
      val allAccesses = mem.readers ++ mem.writers
      dbgs(s"Accesses: $allAccesses")
      // group accesses by their parent
      val groupedByParent = allAccesses.groupBy(_.parent)
      oneAndOthers(groupedByParent.toSeq).flatMap {
        case ((parent, accesses), others) =>
          val otherWrites = others.filter(_._2.exists(_.isWriter))
          val preceding = accesses.flatMap {
            access =>
              val otherWrites = others.flatMap(_._2).filter(_.isWriter).toSet
              if (mem.isReg) {
                reachingWritesToReg(access, otherWrites, writesAlwaysKill = true)
              } else {
                val reachingMatrices = reachingWrites(access.affineMatrices.toSet, otherWrites.flatMap(_.affineMatrices), mem.isGlobalMem)
                reachingMatrices.map(_.access)
              }
          }
          val groupedPreceding = preceding.groupBy(_.parent) map {
            case (otherParent, otherAccesses) =>
              InferredDependencyEdge(otherAccesses, accesses, EdgeType(otherAccesses.head, accesses.head))
          }

          val shouldInitialize = !groupedPreceding.exists(_.edgeType == Forward) && !mem.isGlobalMem

          if (shouldInitialize) {
            Seq(InferredDependencyEdge(Set(mem), accesses, Initialize)) ++ groupedPreceding
          } else {
            groupedPreceding
          }
      }
    }
  }.toSeq

  var dependencyEdges: Seq[DependencyEdge] = Seq.empty

  def compressEdges(edges: Seq[DependencyEdge]): Seq[DependencyEdge] = {
    edges filter {
      case InferredDependencyEdge(src, dst, Inner) => false
      case _ => true
    }
  }

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case _ if lhs.isMem =>
      val prodCons = computeDependencyGraph(lhs)
      dbgs(s"$lhs = $rhs -> $prodCons")
      dependencyEdges ++= prodCons

    case _ => {
      dbgs(s"Skipping: $lhs = $rhs")
      super.visit(lhs, rhs)
    }
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
