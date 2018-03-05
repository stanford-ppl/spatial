package spatial.util

import core._
import spatial.data._
import spatial.issues.AmbiguousMetaPipes
import spatial.lang._
import spatial.node._
import forge.tags._
import utils.implicits.collections._
import utils.DAG

trait UtilsControl {

  def takesEnables(x: Sym[_]): Boolean = x.op.exists{
    case _:EnPrimitive[_] | _:EnControl[_] => true
    case _ => false
  }

  implicit class SymControl(x: Sym[_]) {
    @stateful def getParent: Option[Ctrl] = parentOf.get(x)
    @stateful def parent: Ctrl = parentOf(x)
  }

  def getCChains(block: Block[_]): Seq[CounterChain] = getCChains(block.stms)
  def getCChains(stms: Seq[Sym[_]]): Seq[CounterChain] = stms.collect{case s: CounterChain => s}

  def ctrlIters(ctrl: Ctrl): Seq[I32] = ctrl match {
    case Ctrl(Op(loop: Loop[_]), -1) => loop.iters
    case Ctrl(Op(loop: Loop[_]), i)  => loop.bodies(i)._1
    case _ => Nil
  }

  def ctrDef[F](x: Counter[F]): CounterNew[F] = x match {
    case Op(c: CounterNew[_]) => c.asInstanceOf[CounterNew[F]]
    case _ => throw new Exception(s"Could not find counter definition for $x")
  }
  def cchainDef(x: CounterChain): CounterChainNew = x match {
    case Op(c: CounterChainNew) => c
    case _ => throw new Exception(s"Could not find counterchain definition for $x")
  }

  implicit class CounterChainHelperOps(x: CounterChain) {
    def ctrs: Seq[Counter[_]] = cchainDef(x).counters
  }

  implicit class CounterHelperOps[F](x: Counter[F]) {
    def start: F = ctrDef(x).start.unbox
    def step: F = ctrDef(x).step.unbox
    def end: F = ctrDef(x).end.unbox
    def ctrPar: I32 = ctrDef(x).par
  }

  implicit class IndexHelperOps[W](i: I[W]) {
    @stateful def ctrStart: I[W] = ctrOf(i).start
    @stateful def ctrStep: I[W] = ctrOf(i).step
    @stateful def ctrEnd: I[W] = ctrOf(i).end
    @stateful def ctrPar: I32 = ctrOf(i).ctrPar
    @stateful def ctrParOr1: Int = ctrOf.get(i).map(_.ctrPar.toInt).getOrElse(1)
  }


  /**
    * Returns the least common ancestor (LCA) of the two controllers.
    * If the controllers have no ancestors in common, returns None.
    */
  @stateful def LCA(a: Sym[_], b: Sym[_]): Option[Ctrl] = LCA(parentOf.get(a),parentOf.get(b))
  @stateful def LCA(a: Ctrl, b: Ctrl): Option[Ctrl] = DAG.LCA(a, b){ctrlParent}
  @stateful def LCA(a: Option[Ctrl], b: Option[Ctrl]): Option[Ctrl] = (a,b) match {
    case (Some(c1),Some(c2)) => LCA(c1,c2)
    case _ => None
  }

  @stateful def LCAWithPaths(a: Ctrl, b: Ctrl): (Option[Ctrl], Seq[Ctrl], Seq[Ctrl]) = DAG.LCAWithPaths(a,b){ctrlParent}

  /**
    * Returns the LCA between two controllers a and b along with their pipeline distance.
    *
    * Pipeline distance between controllers a and b:
    *   If a and b have a least common ancestor which is neither a nor b,
    *   this is defined as the dataflow distance between the LCA's children which contain a and b
    *   When a and b are equal, the distance is defined as zero.
    *
    *   The distance is undefined when the LCA is a xor b, or if a and b occur in parallel
    *   The distance is positive if a comes before b, negative otherwise
    */
  @stateful def LCAWithDistance(a: Sym[_], b: Sym[_]): (Ctrl,Int) = LCAWithDistance(parentOf(a),parentOf(b))
  @stateful def LCAWithDistance(a: Ctrl, b: Ctrl): (Ctrl,Int) = {
    if (a == b) (a,0)
    else {
      val (lca, pathA, pathB) = LCAWithPaths(a, b)
      lca match {
        case None => throw new Exception(s"Undefined distance between $a and $b (no LCA)")

        case Some(parent) if isOuterControl(parent) && parent != a && parent != b  =>
          val topA = pathA.find{c => c.sym != parent.sym }.get
          val topB = pathB.find{c => c.sym != parent.sym }.get
          //dbg(s"PathA: " + pathA.mkString(", "))
          //dbg(s"PathB: " + pathB.mkString(", "))
          //dbg(s"LCA: $parent")
          // TODO[2]: Update with arbitrary children graph once defined
          val idxA = childrenOf(parent.sym).indexOf(topA.sym)
          val idxB = childrenOf(parent.sym).indexOf(topB.sym)
          if (idxA < 0 || idxB < 0) throw new Exception(s"Undefined distance between $a and $b (idxA=$idxA,idxB=$idxB)")
          val dist = idxB - idxA
          (parent,dist)

        case Some(parent) => (parent,0)
      }
    }
  }

  /**
    * Returns the LCA and coarse-grained pipeline distance between accesses a and b.
    *
    * Coarse-grained pipeline distance:
    *   If the LCA controller of a and b is a metapipeline, the pipeline distance
    *   is the distance between the respective controllers for a and b. Otherwise zero.
    **/
  @stateful def LCAWithCoarseDistance(a: Sym[_], b: Sym[_]): (Ctrl,Int) = LCAWithCoarseDistance(parentOf(a),parentOf(b))
  @stateful def LCAWithCoarseDistance(a: Ctrl, b: Ctrl): (Ctrl,Int) = {
    val (lca,dist) = LCAWithDistance(a,b)
    val coarseDist = if (isOuterControl(lca) && isLoop(lca)) dist else 0
    (lca, coarseDist)
  }

  /**
    * Returns all metapipeline parents between all pairs of (w in writers, a in readers U writers)
    */
  @stateful def findAllMetaPipes(readers: Set[Sym[_]], writers: Set[Sym[_]]): Map[Ctrl,Set[(Sym[_],Sym[_])]] = {
    if (writers.isEmpty && readers.isEmpty) Map.empty
    else {
      val ctrlGrps: Set[(Ctrl,(Sym[_],Sym[_]))] = writers.flatMap{w =>
        (readers ++ writers).flatMap{a =>
          val (lca,dist) = LCAWithCoarseDistance(w,a)
          if (dist != 0) Some(lca,(w,a)) else None
        }
      }
      ctrlGrps.groupBy(_._1).mapValues(_.map(_._2))
    }
  }


  @stateful def findMetaPipe(mem: Sym[_], readers: Set[Sym[_]], writers: Set[Sym[_]]): (Option[Ctrl], Map[Sym[_],Option[Int]]) = {
    val accesses = readers ++ writers
    val metapipeLCAs = findAllMetaPipes(readers, writers)
    if (metapipeLCAs.keys.size > 1) raiseIssue(AmbiguousMetaPipes(mem, metapipeLCAs))

    metapipeLCAs.keys.headOption match {
      case Some(metapipe) =>
        val group  = metapipeLCAs(metapipe)
        val anchor = group.head._1
        val dists = accesses.map{a =>
          val (lca,dist) = LCAWithCoarseDistance(anchor,a)
          if (lca == metapipe) a -> Some(dist) else a -> None
        }
        val buffers = dists.filter{_._2.isDefined}.map(_._2.get)
        val minDist = buffers.minOrElse(0)
        val ports = dists.map{case (a,dist) => a -> dist.map{d => d - minDist} }.toMap
        (Some(metapipe), ports)

      case None =>
        (None, accesses.map{a => a -> Some(0)}.toMap)
    }
  }


  /**
    * Returns true if either this symbol is a loop or exists within a loop.
    */
  @stateful def isInLoop(s: Sym[_]): Boolean = isLoop(s) || ctrlParents(s).exists(isLoop)
  @stateful def isInLoop(ctrl: Ctrl): Boolean = isLoop(ctrl) || ctrlParents(ctrl).exists(isLoop)

  /**
    * Returns true if symbols a and b occur in Parallel (but not metapipeline parallel).
    */
  @stateful def areInParallel(a: Sym[_], b: Sym[_]): Boolean = {
    val lca = LCA(a,b)
    // TODO[2]: Arbitrary dataflow graph for children
    lca.exists{c => isInnerControl(c) }
  }

  /**
    * Returns the controller parent of this controller
    */
  @stateful def ctrlParent(ctrl: Ctrl): Option[Ctrl] = ctrl.id match {
    case -1 => parentOf.get(ctrl.sym)
    case _  => Some(Ctrl(ctrl.sym, -1))
  }

  /**
    * Returns the symbols of all ancestor controllers of this symbol
    * Ancestors are ordered outermost to innermost
    */
  @stateful def symParents(sym: Sym[_], stop: Option[Sym[_]] = None): Seq[Sym[_]] = {
    DAG.ancestors(sym, stop){p => parentOf.get(p).map(_.sym) }
  }

  /**
    * Returns all ancestor controllers from this controller (inclusive) to optional stop (inclusive)
    * Ancestors are ordered outermost to innermost
    */
  @stateful def ctrlParents(ctrl: Ctrl): Seq[Ctrl] = DAG.ancestors(ctrl,None){ctrlParent}
  @stateful def ctrlParents(ctrl: Ctrl, stop: Option[Ctrl]): Seq[Ctrl] = DAG.ancestors(ctrl,stop){ctrlParent}
  @stateful def ctrlParents(sym: Sym[_], stop: Option[Ctrl] = None): Seq[Ctrl] = {
    parentOf.get(sym).map(p => ctrlParents(p,stop)).getOrElse(Nil)
  }

  /**
    * Returns a list of controllers from the start (inclusive) to the end (exclusive).
    * Controllers are ordered outermost to innermost.
    */
  @stateful def ctrlBetween(start: Option[Ctrl], end: Option[Ctrl]): Seq[Ctrl] = {
    if (start.isEmpty) Nil
    else {
      val path = ctrlParents(start.get, end)
      if (path.headOption == end) path.drop(1) else path
    }
  }

  @stateful def ctrlBetween(access: Sym[_], mem: Sym[_]): Seq[Ctrl] = {
    ctrlBetween(parentOf.get(access), parentOf.get(mem))
  }

}
