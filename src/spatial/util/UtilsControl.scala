package spatial.util

import argon._
import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.internal.spatialConfig
import spatial.issues.AmbiguousMetaPipes
import forge.tags._
import utils.implicits.collections._
import utils.Tree

import scala.util.Try

trait UtilsControl {

  /** Operations implicitly defined on both Sym[_] and Ctrl. */
  abstract class ControlOps(s: Option[Sym[_]]) {
    private def op: Option[Op[_]] = s.flatMap{sym => sym.op : Option[Op[_]] }
    def toCtrl: Ctrl

    def takesEnables: Boolean = op.exists{
      case _:EnPrimitive[_] | _:EnControl[_] => true
      case _ => false
    }

    def cchains: Seq[CounterChain] = op match {
      case Some(op: Control[_]) => op.cchains.map(_._1).distinct
      case _ => Nil
    }

    def isInnerControl: Boolean = toCtrl match {
      case ctrl @ Controller(sym,_) => sym.isControl && (!sym.isOuter || !ctrl.isOuterBlock)
      case Host => false
    }
    def isOuterControl: Boolean = toCtrl match {
      case ctrl @ Controller(sym,_) => sym.isControl && sym.isOuter && ctrl.isOuterBlock
      case Host => true
    }

    def isPipeline: Boolean = s.exists(_.schedule == Sched.Pipe)
    def isSequential: Boolean = s.exists(_.schedule == Sched.Seq)
    def isStreamPipe: Boolean = s.exists(_.schedule == Sched.Stream)

    def isInnerPipe: Boolean = isInnerControl && isPipeline && toCtrl.isLoop
    def isMetaPipe: Boolean  = isOuterControl && isPipeline

    def isInnerStream: Boolean = isInnerControl && isStreamPipe
    def isOuterStream: Boolean = isOuterControl && isStreamPipe


    def isForever: Boolean = op match {
      case Some(op: Control[_]) => op.cchains.exists(_._1.isForever)
      case Some(op: CounterChainNew) => op.counters.exists(_.isForever)
      case Some(_: ForeverNew) => true
      case _ => false
    }

    def ancestors: Seq[Ctrl] = Tree.ancestors(this.toCtrl){_.parent}
    def ancestors(stop: Ctrl => Boolean): Seq[Ctrl] = Tree.ancestors(toCtrl, stop){_.parent}
    def ancestors(stop: Ctrl): Seq[Ctrl] = Tree.ancestors[Ctrl](toCtrl, {c => c == stop}){_.parent}

    @stateful protected def controlChildren: Seq[Ctrl]

    @stateful def willRunForever: Boolean = isForever || controlChildren.exists(_.willRunForever)

    /** Returns true if either this symbol is a loop or occurs within a loop. */
    def isInLoop: Boolean = s.exists(_.isLoop) || ancestors.exists(_.isLoop)
  }


  implicit class SymControl(s: Sym[_]) extends ControlOps(Some(s)) {
    def toCtrl: Ctrl = if (s.isControl) Controller(s,-1) else s.parent

    @stateful protected def controlChildren: Seq[Ctrl] = s.children
  }


  implicit class CtrlControl(ctrl: Ctrl) extends ControlOps(ctrl.s) {
    def toCtrl: Ctrl = ctrl
    @stateful protected def controlChildren: Seq[Ctrl] = ctrl.children
  }




  implicit class CounterChainHelperOps(x: CounterChain) {
    def node: CounterChainNew = x match {
      case Op(c: CounterChainNew) => c
      case _ => throw new Exception(s"Could not find counterchain definition for $x")
    }

    def ctrs: Seq[Counter[_]] = x.node.counters
    def pars: Seq[I32] = ctrs.map(_.ctrPar)
    def willFullyUnroll: Boolean = ctrs.forall(_.willFullyUnroll)
    def isUnit: Boolean = ctrs.forall(_.isUnit)
    def isStatic: Boolean = ctrs.forall(_.isStatic)
  }


  implicit class CounterHelperOps[F](x: Counter[F]) {
    def node: CounterNew[F] = x match {
      case Op(c: CounterNew[_]) => c.asInstanceOf[CounterNew[F]]
      case _ => throw new Exception(s"Could not find counter definition for $x")
    }

    def start: Sym[F] = x.node.start
    def step: Sym[F] = x.node.step
    def end: Sym[F] = x.node.end
    def ctrPar: I32 = x.node.par
    def isStatic: Boolean = (start,step,end) match {
      case (Final(_), Final(_), Final(_)) => true
      case _ => false
    }
    def nIters: Option[Bound] = (start,step,end) match {
      case (Final(min), Final(stride), Final(max)) =>
        Some(Final(Math.ceil((max - min).toDouble / stride).toInt))

      case (Expect(min), Expect(stride), Expect(max)) =>
        Some(Expect(Math.ceil((max - min).toDouble / stride).toInt))

      case _ => None
    }
    def willFullyUnroll: Boolean = (nIters,ctrPar) match {
      case (Some(Expect(nIter)), Expect(par)) => par >= nIter
      case _ => false
    }
    def isUnit: Boolean = nIters match {
      case (Some(Final(1))) => true
      case _ => false
    }
  }

  implicit class IndexHelperOps[W](i: Ind[W]) {
    def ctrStart: Ind[W] = i.counter.start.unbox
    def ctrStep: Ind[W] = i.counter.step.unbox
    def ctrEnd: Ind[W] = i.counter.end.unbox
    def ctrPar: I32 = i.counter.ctrPar
    def ctrParOr1: Int = i.getCounter.map(_.ctrPar.toInt).getOrElse(1)
  }


  def getCChains(block: Block[_]): Seq[CounterChain] = getCChains(block.stms)
  def getCChains(stms: Seq[Sym[_]]): Seq[CounterChain] = stms.collect{case s: CounterChain => s}


  def ctrlIters(ctrl: Ctrl): Seq[I32] = Try(ctrl match {
    case Host => Nil
    case Controller(Op(loop: Loop[_]), -1) => loop.iters
    case Controller(Op(loop: Loop[_]), i)  => loop.bodies(i)._1
    case Controller(Op(loop: UnrolledLoop[_]), -1) => loop.iters
    case Controller(Op(loop: UnrolledLoop[_]), i)  => loop.bodiess.apply(i)._1.flatten
    case _ => Nil
  }).getOrElse(throw new Exception(s"$ctrl had invalid iterators"))



  /** Returns the least common ancestor (LCA) of the two controllers.
    * If the controllers have no ancestors in common, returns None.
    */
  def LCA(a: Sym[_], b: Sym[_]): Ctrl = LCA(a.parent,b.parent)
  def LCA(a: Ctrl, b: Ctrl): Ctrl= Tree.LCA(a, b){_.parent}

  def LCAWithPaths(a: Ctrl, b: Ctrl): (Ctrl, Seq[Ctrl], Seq[Ctrl]) = {
    Tree.LCAWithPaths(a,b){_.parent}
  }

  /** Returns the LCA between two symbols a and b along with their pipeline distance.
    *
    * Pipeline distance between a and b:
    *   If a and b have a least common control ancestor which is neither a nor b,
    *   this is defined as the dataflow distance between the LCA's children which contain a and b
    *   If a and b are equal, the distance is defined as zero.
    *
    *   The distance is undefined when the LCA is a xor b, or if a and b occur in parallel
    *   The distance is positive if a comes before b, negative otherwise
    */
  @stateful def LCAWithDistance(a: Sym[_], b: Sym[_]): (Ctrl,Int) = LCAWithDistance(a.toCtrl,b.toCtrl)


  @stateful def LCAWithDistance(a: Ctrl, b: Ctrl): (Ctrl,Int) = {
    if (a == b) (a,0)
    else {
      val (lca, pathA, pathB) = LCAWithPaths(a, b)
      if (lca.isOuterControl && lca != a && lca != b) {
        val topA = pathA.find{c => c.s != lca.s }.get
        val topB = pathB.find{c => c.s != lca.s }.get
        //dbg(s"PathA: " + pathA.mkString(", "))
        //dbg(s"PathB: " + pathB.mkString(", "))
        //dbg(s"LCA: $lca")
        // TODO[2]: Update with arbitrary children graph once defined
        val idxA = lca.children.indexOf(topA)
        val idxB = lca.children.indexOf(topB)
        if (idxA < 0 || idxB < 0) throw new Exception(s"Undefined distance between $a and $b (idxA=$idxA,idxB=$idxB)")
        val dist = idxB - idxA
        (lca,dist)
      }
      else {
        (lca, 0)
      }
    }
  }

  /** Returns the LCA and coarse-grained pipeline distance between accesses a and b.
    *
    * Coarse-grained pipeline distance:
    *   If the LCA controller of a and b is a metapipeline, the pipeline distance
    *   is the distance between the respective controllers for a and b. Otherwise zero.
    */
  @stateful def LCAWithCoarseDistance(a: Sym[_], b: Sym[_]): (Ctrl,Int) = {
    LCAWithCoarseDistance(a.parent, b.parent)
  }
  @stateful def LCAWithCoarseDistance(a: Ctrl, b: Ctrl): (Ctrl,Int) = {
    val (lca,dist) = LCAWithDistance(a,b)
    val coarseDist = if (lca.isMetaPipe || lca.isOuterStream) dist else 0
    (lca, coarseDist)
  }


  /** Returns all metapipeline parents between all pairs of (w in writers, a in readers U writers). */
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

          dbgs(s"$a <-> $anchor # LCA: $lca, Dist: $dist")

          if (lca == metapipe || anchor == a) a -> Some(dist) else a -> None
        }
        val buffers = dists.filter{_._2.isDefined}.map(_._2.get)
        val minDist = buffers.minOrElse(0)
        val ports = dists.map{case (a,dist) => a -> dist.map{d => d - minDist} }.toMap
        (Some(metapipe), ports)

      case None =>
        (None, accesses.map{a => a -> Some(0)}.toMap)
    }
  }



  /** Returns true if accesses a and b occur to the same buffer port.
    * This is true when any of the following hold:
    *   1. a and b are in the same inner pipeline
    *   2. a and b are in the same inner streaming pipeline
    *   3. a and b are in a Parallel controller
    */
  def requireParallelPortAccess(a: Sym[_], b: Sym[_]): Boolean = {
    val lca = LCA(a,b)
    // TODO[2]: Arbitrary dataflow graph for children
    lca.isInnerPipe || lca.isInnerStream || lca.isParallel
  }


}
