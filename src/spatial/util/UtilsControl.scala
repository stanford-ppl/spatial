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

  def takesEnables(x: Sym[_]): Boolean = x.op.exists{
    case _:EnPrimitive[_] | _:EnControl[_] => true
    case _ => false
  }

  implicit class SymControl(x: Sym[_]) {
    def toCtrl: Ctrl = if (isControl(x)) Controller(x,-1) else x.parent

    def parent: Ctrl = metadata[ParentController](x).map(_.parent).getOrElse(Host)
    def parent_=(p: Ctrl): Unit = metadata.add(x, ParentController(p))

    def blk: Ctrl = metadata[ParentBlk](x).map(_.blk).getOrElse{Host}
    def blk_=(blk: Ctrl): Unit = metadata.add(x, ParentBlk(blk))

    @stateful def children: Seq[Controller] = {
      if (!isControl(x)) throw new Exception(s"Cannot get children of non-controller.")
      metadata[Children](x).map(_.children).getOrElse(Nil)
    }
    def children_=(cs: Seq[Controller]): Unit = metadata.add(x, Children(cs))

    def ancestors: Seq[Ctrl] = Tree.ancestors(x.toCtrl){_.parent}
    def ancestors(stop: Ctrl => Boolean): Seq[Ctrl] = x.toCtrl.ancestors(stop)
    def ancestors(stop: Ctrl): Seq[Ctrl] = x.toCtrl.ancestors(stop)

    def cchains: Seq[CounterChain] = x.op match {
      case Some(op: Control[_]) => op.cchains.map(_._1).distinct
      case _ => Nil
    }

    def isUnitPipe: Boolean = x.op.exists(_.isInstanceOf[UnitPipe])
    def isAccelScope: Boolean = x.op.exists(_.isInstanceOf[AccelScope])
    def isStreamPipe: Boolean = styleOf(x) == Sched.Stream
    def isForever: Boolean = x.op match {
      case Some(op: Control[_]) => x.cchains.exists(_.isForever)
      case Some(op: CounterChainNew) => op.counters.exists(_.isForever)
      case Some(_: ForeverNew) => true
      case _ => false
    }
    @stateful def willRunForever: Boolean = x.isForever || x.children.exists(_.willRunForever)
  }
  implicit class CtrlControl(x: Ctrl) {
    def isStreamPipe: Boolean = x.s.exists(_.isStreamPipe)
    @stateful def willRunForever: Boolean = x.s.exists(_.willRunForever)
  }

  def getCChains(block: Block[_]): Seq[CounterChain] = getCChains(block.stms)
  def getCChains(stms: Seq[Sym[_]]): Seq[CounterChain] = stms.collect{case s: CounterChain => s}

  def ctrlIters(ctrl: Ctrl): Seq[I32] = Try(ctrl match {
    case Host => Nil
    case Controller(Op(loop: Loop[_]), -1) => loop.iters
    case Controller(Op(loop: Loop[_]), i)  => loop.bodies(i)._1
    case _ => Nil
  }).getOrElse(throw new Exception(s"$ctrl had invalid iterators"))

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
    def pars: Seq[I32] = ctrs.map(_.ctrPar)
    def shouldFullyUnroll: Boolean = ctrs.forall(_.shouldFullyUnroll)
    def mayFullyUnroll: Boolean = ctrs.forall(_.mayFullyUnroll)
    def isUnit: Boolean = ctrs.forall(_.isUnit)
    def isStatic: Boolean = ctrs.forall(_.isStatic)
  }

  implicit class CounterHelperOps[F](x: Counter[F]) {
    def start: Sym[F] = ctrDef(x).start
    def step: Sym[F] = ctrDef(x).step
    def end: Sym[F] = ctrDef(x).end
    def ctrPar: I32 = ctrDef(x).par
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
    def shouldFullyUnroll: Boolean = (nIters,ctrPar) match {
      case (Some(Final(nIter)), Final(par)) => par >= nIter
      case _ => false
    }
    def mayFullyUnroll: Boolean = (nIters,ctrPar) match {
      case (Some(Expect(nIter)), Expect(par)) => par >= nIter
      case _ => false
    }
    def isUnit: Boolean = nIters match {
      case (Some(Final(1))) => true
      case _ => false
    }
  }

  implicit class IndexHelperOps[W](i: Ind[W]) {
    @stateful def ctrStart: Ind[W] = ctrOf(i).start.unbox
    @stateful def ctrStep: Ind[W] = ctrOf(i).step.unbox
    @stateful def ctrEnd: Ind[W] = ctrOf(i).end.unbox
    @stateful def ctrPar: I32 = ctrOf(i).ctrPar
    @stateful def ctrParOr1: Int = ctrOf.get(i).map(_.ctrPar.toInt).getOrElse(1)
  }


  /**
    * Returns the least common ancestor (LCA) of the two controllers.
    * If the controllers have no ancestors in common, returns None.
    */
  def LCA(a: Sym[_], b: Sym[_]): Ctrl = LCA(a.parent,b.parent)
  def LCA(a: Ctrl, b: Ctrl): Ctrl= Tree.LCA(a, b){_.parent}

  def LCAWithPaths(a: Ctrl, b: Ctrl): (Ctrl, Seq[Ctrl], Seq[Ctrl]) = {
    Tree.LCAWithPaths(a,b){_.parent}
  }

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
  @stateful def LCAWithDistance(a: Sym[_], b: Sym[_]): (Ctrl,Int) = LCAWithDistance(a.parent,b.parent)
  @stateful def LCAWithDistance(a: Ctrl, b: Ctrl): (Ctrl,Int) = {
    if (a == b) (a,0)
    else {
      val (lca, pathA, pathB) = LCAWithPaths(a, b)
      if (isOuterControl(lca) && lca != a && lca != b) {
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

  /**
    * Returns the LCA and coarse-grained pipeline distance between accesses a and b.
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


  /** Returns true if either this symbol is a loop or exists within a loop. */
  def isInLoop(s: Sym[_]): Boolean = isLoop(s) || s.ancestors.exists(isLoop)
  def isInLoop(ctrl: Ctrl): Boolean = isLoop(ctrl) || ctrl.ancestors.exists(isLoop)

  /**
    * Returns true if symbols a and b occur in Parallel (but not metapipeline parallel).
    */
  def areInParallel(a: Sym[_], b: Sym[_]): Boolean = {
    val lca = LCA(a,b)
    // TODO[2]: Arbitrary dataflow graph for children
    isInnerControl(lca) || isParallel(lca)
  }


  def isStreamLoad(e: Sym[_]): Boolean = e match {
    case Op(_:FringeDenseLoad[_,_]) => true
    case _ => false
  }

  def isTileTransfer(e: Sym[_]): Boolean = e match {
    case Op(_:FringeDenseLoad[_,_]) => true
    case Op(_:FringeDenseStore[_,_]) => true
    case Op(_:FringeSparseLoad[_,_]) => true
    case Op(_:FringeSparseStore[_,_]) => true
    case _ => false
  }

  // TODO[3]: Should this just be any write?
  def isParEnq(e: Sym[_]): Boolean = e match {
    case Op(_:FIFOBankedEnq[_]) => true
    case Op(_:LIFOBankedPush[_]) => true
    case Op(_:SRAMBankedWrite[_,_]) => true
    case Op(_:FIFOEnq[_]) => true
    case Op(_:LIFOPush[_]) => true
    case Op(_:SRAMWrite[_,_]) => true
    //case Op(_:ParLineBufferEnq[_]) => true
    case _ => false
  }

  def isStreamStageEnabler(e: Sym[_]): Boolean = e match {
    case Op(_:FIFODeq[_]) => true
    case Op(_:FIFOBankedDeq[_]) => true
    case Op(_:LIFOPop[_]) => true
    case Op(_:LIFOBankedPop[_]) => true
    case Op(_:StreamInRead[_]) => true
    case Op(_:StreamInBankedRead[_]) => true
    //case Op(_:DecoderTemplateNew[_]) => true
    //case Op(_:DMATemplateNew[_]) => true
    case _ => false
  }

  def isStreamStageHolder(e: Sym[_]): Boolean = e match {
    case Op(_:FIFOEnq[_]) => true
    case Op(_:FIFOBankedEnq[_]) => true
    case Op(_:LIFOPush[_]) => true
    case Op(_:LIFOBankedPush[_]) => true
    case Op(_:StreamOutWrite[_]) => true
    case Op(_:StreamOutBankedWrite[_]) => true
    //case Op(_:BufferedOutWrite[_]) => true
    //case Op(_:DecoderTemplateNew[_]) => true
    case _ => false
  }

}
