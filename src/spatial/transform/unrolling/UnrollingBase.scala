package spatial.transform.unrolling

import argon._
import argon.node.Enabled
import argon.transform.MutateTransformer

import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal

import utils.tags.instrument

/** Options when transforming a statement:
  *   0. Remove it: s -> Nil. Statement will not appear in resulting graph.
  *   1. Update it: s -> List(s). Statement will not change except for inputs.
  *   2. Subst. it: s -> List(s'). Substitution s' will appear instead.
  *   3. Expand it: s -> List(a,b,c). Substitutions will appear instead.
  *                                   Downstream users must specify which subst. to use.
  */
abstract class UnrollingBase extends MutateTransformer with AccelTraversal {

  /** Valid bits - tracks all valid bits associated with the current scope to handle edge cases
    * e.g. cases where parallelization is not an even divider of counter max
    */
  private var validBits: Set[Bit] = Set.empty
  def withValids[T](valids: Seq[Bit])(blk: => T): T = {
    val prevValids = validBits
    validBits = valids.toSet // TODO: Should this be ++= ?
    val result = blk
    validBits = prevValids
    result
  }

  /** Set of valid bits associated with current unrolling scope */
  def enables: Set[Bit] = validBits

  /** Unroll numbers - gives the unroll index of each pre-unrolled (prior to transformer) index
    * Used to determine which duplicate a particular memory access should be associated with
    */
  var unrollNum: Map[Idx, Seq[Int]] = Map.empty
  def withUnrollNums[A](ind: Seq[(Idx, Seq[Int])])(blk: => A): A = {
    val prevUnroll = unrollNum
    unrollNum ++= ind
    val result = blk
    unrollNum = prevUnroll
    result
  }

  /** Memory duplicate substitution rules - gives a mapping from a pre-unrolled memory
    * and dispatch index to an unrolled memory instance
    */
  var memories: Map[(Sym[_], Int), Sym[_]] = Map.empty


  /** Lanes tracking duplications in this scope */
  var lanes: Unroller = UnitUnroller("Accel", false)
  def inLanes[T](l: Unroller)(scope: => T): T = {
    val saveLanes = lanes
    lanes = l
    val result = scope
    lanes = saveLanes
    result
  }

  def unrollWithoutResult(block: Block[_], lanes: Unroller): Unit = inLanes(lanes){
    state.logTab += 1
    block.stms.foreach(visit)
    state.logTab -= 1
  }

  def unroll[A](block: Block[A], lanes: Unroller): List[A] = inLanes(lanes){
    // Note: We don't use inlineBlock because f(block.result) is meaningless outside a lane
    unrollWithoutResult(block, lanes)
    lanes.map{_ => f(block.result).unbox }
  }

  def unroll[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): List[Sym[_]] = {
    dbgs(s"Unrolling $lhs = $rhs")
    val lhs2 =  if (rhs.isControl) duplicateController(lhs,rhs)
                else lanes.duplicate(lhs,rhs)
    dbgs(s"[$lhs] ${lhs2.zipWithIndex.map{case (l2,i) => s"$i: $l2"}.mkString(", ")}")
    lhs2
  }

  def unrollCtrl[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[_] = {
    // By default, use a unit unroller (only one lane)
    inLanes(UnitUnroller(lhs.fullname,lhs.isInnerControl)){ mirror(lhs,rhs) }
  }

  /** Duplicate the given controller based on the global Unroller helper instance lanes.
    * For parallelization greater than 1, this adds a Parallel node around the control copies.
    */
  final def duplicateController[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): List[Sym[_]] = {
    dbgs(s"Duplicating controller $lhs = $rhs")
    def duplicate(): Sym[A] = unrollCtrl(lhs,rhs).asInstanceOf[Sym[A]]
    if (lanes.size > 1) {
      val block = stageBlock {
        lanes.foreach{ ln =>
          dbgs(s"$lhs = $rhs [duplicate $ln/${lanes.size}]")
          if (rhs.blocks.nonEmpty) {
            rhs.blocks.head.stms.take(15).foreach{s => dbgs(s"  ${stm(s)} [${subst.getOrElse(s,s)}]")}
            dbgs("")
          }

          duplicate()
        }
      }
      val lhs2 = stage(ParallelPipe(enables,block))
      lanes.unify(lhs, lhs2)
    }
    else {
      dbgs(s"$lhs = $rhs [duplicate 1/1]")
      val first = lanes.mapFirst{ duplicate() }
      lanes.unify(lhs, first)
    }
  }

  final override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case _:AccelScope => inAccel{ super.transform(lhs,rhs) }
    case _ =>
      val duplicates: List[Sym[_]] = unroll(lhs, rhs)
      if (duplicates.length == 1) duplicates.head.asInstanceOf[Sym[A]]
      else Invalid.asInstanceOf[Sym[A]]
  }


  def inReduce[T](red: Option[ReduceFunction], isInner: Boolean)(blk: => T): T = withFlow("inReduce",{e =>
    e.reduceType = red
  }){ blk }


  override protected def inlineBlock[T](block: Block[T]): Sym[T] = {
    inlineWith(block){stms =>
      stms.foreach(visit)
      lanes.mapFirst { f(block.result) }
    }
  }

  override def mirrorNode[A](rhs: Op[A]): Op[A] = rhs match {
    case e:Enabled[_] => e.mirrorEn(f, enables).asInstanceOf[Op[A]]
    case _ => super.mirrorNode(rhs)
  }
  override def updateNode[A](node: Op[A]): Unit = node match {
    case e:Enabled[_] => e.updateEn(f, enables)
    case _ => super.updateNode(node)
  }

  override def isolateSubstIf[A](cond: Boolean, escape: Seq[Sym[_]])(block: => A): A = {
    super.isolateSubstIf(cond, escape){ lanes.isolateIf(cond, escape){ block } }
  }

  override def usedRemovedSymbol[T](x: T): Nothing = {
    dbgs(s"Used removed symbol $x!")
    lanes.contexts.zipWithIndex.foreach{case (ctx,i) =>
      dbgs(s"Lane #$i:")
      ctx.foreach{case (k,v) => dbgs(s"  $k -> $v") }
    }
    dbgs(s"Subst:")
    subst.foreach{case (k,v) => dbgs(s"  $k -> $v")}
    throw new Exception(s"Used removed symbol: $x")
  }


  /** Helper objects for unrolling
    * Tracks multiple substitution contexts in 'contexts' array
    */
  trait Unroller {
    type Lane = List[Int]
    type MemContext = ((Sym[_],Int), Sym[_])
    val name: String

    def inds: Seq[Idx]
    def Ps: Seq[Int]
    def vectorize:Boolean

    def P: Int = if (vectorize) 1 else Ps.product
    def V: Int = if (vectorize) Ps.product else 1
    def N: Int = Ps.length
    def size: Int = P
    def prods: List[Int] = List.tabulate(N){i => Ps.slice(i+1,N).product }

    lazy val ulanes:List[Lane] = {
      if (vectorize) List(List.tabulate(P)(i => i))
      else List.tabulate(P) { p => List(p) }
    }
    /*
     * @param p is lane index. 
     * Return the iter/valid index of each counter for lane p
     * */
    def parAddr(p: Int): List[Int] = List.tabulate(N){d => (p / prods(d)) % Ps(d) }

    def contexts: Array[ Map[Sym[_],Sym[_]] ]

    private var __memContexts: Option[Array[Seq[MemContext]]] = None
    def memContexts: Array[Seq[MemContext]] = {
      if (__memContexts.isEmpty) { __memContexts = Some(Array.fill(P)(Nil)) }
      __memContexts.get
    }

    private var __valids: Option[ Seq[Seq[Bit]] ] = None
    protected def createLaneValids(): Seq[Seq[Bit]]

    final def valids: Seq[Seq[Bit]] = __valids.getOrElse{
      val vlds = createLaneValids()
      __valids = Some(vlds)
      vlds
    }

    def isolateIf[A](cond: Boolean, escape: Seq[Sym[_]])(block: => A): A = {
      /*contexts.zipWithIndex.foreach{case (ctx,i) =>
        dbgs(s"[Enter] Lane #$i: " + ctx.map{case (s1,s2) => s"$s1->$s2" }.mkString(","))
      }*/

      val saveContexts = Array.tabulate(P){i => contexts(i) }
      val saveMemories = Array.tabulate(P){i => memContexts(i) }
      val result = block

      /*contexts.zipWithIndex.foreach{case (ctx,i) =>
        dbgs(s"[Inside] Lane #$i: " + ctx.map{case (s1,s2) => s"$s1->$s2" }.mkString(","))
      }*/

      if (cond) {
        saveContexts.indices.foreach{i => contexts(i) = contexts(i).filter{s => escape.contains(s._1) } ++ saveContexts(i) }
        saveMemories.indices.foreach{i => memContexts(i) = saveMemories(i) }
      }

      /*contexts.zipWithIndex.foreach{case (ctx,i) =>
        dbgs(s"[Exit] Lane #$i: " + ctx.map{case (s1,s2) => s"$s1->$s2" }.mkString(","))
      }*/

      result
    }

    def inLanes[A](lns: Seq[Int])(block: Int => A): Seq[A] = lns.map{ln => inLane(ulanes(ln))(block(ln)) }

    /*
     * @param i unrolled lane id
     * @param ln lane id
     * */
    def inLane[A](lane:Lane)(block: => A): A = {
      // Note that we don't use isolateSubst (or similar here) because that would also save/restore lanes
      val i = ulanes.indexOf(lane)
      val save     = subst
      val saveMems = memories
      val addr     = lane.map { l => parAddr(l) }.transpose

      subst ++= contexts(i)
      memories ++= memContexts(i)

      val result = withUnrollNums(inds.zip(addr)) {
        withValids(valids(i)) {
          block
        }
      }

      // Retain the substitutions added within this scope
      contexts(i) ++= subst.filterNot(save contains _._1)
      memContexts(i) ++= memories.filterNot(saveMems contains _._1)

      subst = save
      memories = saveMems
      result
    }

    def map[A](block: Lane => A): List[A] = ulanes.map { lane => inLane(lane) { block(lane) } }

    def foreach(block: Lane => Unit): Unit = ulanes.foreach { lane => inLane(lane) { block(lane) } }

    def mapFirst[A](block: => A):A = {
      inLane(ulanes.head) { block }
    }

    // --- Each unrolling rule should do at least one of three things:

    // 1. Split a given vector as the substitution for the single original symbol
    def duplicate[A](s: Sym[A], d: Op[A]): List[Sym[_]] = {
      if (size > 1 || copyMode) map{ lns =>
        dbgs(s"Lane #$lns: ")
        val s2 = mirror(s, d)
        register(s -> s2)
        dbgs(s"${stm(s2)}")
        s2
      }
      else mapFirst {
        val s2 = mirror(s,d) // TODO[5]: Can change to update?
        dbgs(s"${stm(s2)}")
        register(s -> s2)
        List(s2)
      }
    }
    // 2. Make later stages depend on the given substitution across all lanes
    // NOTE: This assumes that the node has no meaningful return value (i.e. all are Pipeline or Unit)
    // Bad things can happen here if you're not careful!
    //def split[T:Type](orig: Sym[T], vec: Vec[T])(implicit ctx: SrcCtx): List[T] = map{p =>
      //val element = vec(p)
      //register(orig -> element)
      //element
    //}
    //def splitLanes[T:Type](lns: List[Int])(orig: Sym[_], vec: Vec[T])(implicit ctx: SrcCtx): List[T] = {
      //lns.zipWithIndex.map{case (ln,i) =>
        //inLane(ln){
          //val element = vec(i)
          //register(orig -> element)
          //element
        //}
      //}
    //}

    // 3. Create an unrolled mapping of symbol (orig -> unrolled) for each lane
    def unify[T](orig: Sym[T], unrolled: Sym[T]): List[Sym[T]] = {
      foreach{p => register(orig -> unrolled) }
      List(unrolled)
    }
    def unifyLanes[T](lns: Seq[Int])(orig: Sym[T], unrolled: Sym[T]): List[Sym[T]] = {
      inLanes(lns){p => register(orig -> unrolled) }
      List(unrolled)
    }

    def unifyUnsafe[A,B](orig: Sym[A], unrolled: Sym[B]): List[Sym[B]] = {
      foreach{_ => register(orig -> unrolled) }
      List(unrolled)
    }

    def duplicateMem(mem: Sym[_])(blk: Int => Seq[(Sym[_],Int)]): Unit = ulanes.zipWithIndex.foreach{ case (lane, p) =>
      val duplicates = lane.flatMap { l => blk(l) }.distinct
      dbgs(s"  Registering duplicates for memory: $mem")
      memContexts(p) ++= duplicates.map{case (mem2,d) =>
        dbgs(s"  ($mem,$d) -> $mem2")
        (mem,d) -> mem2
      }
    }

    // Same symbol for all lanes
    def isCommon(e: Sym[_]): Boolean = contexts.map{p => f(e)}.forall{e2 => e2 == f(e)}
  }

  trait LoopUnroller extends Unroller {
    def isInnerLoop:Boolean
    def cchain:CounterChain
    // Counters[Pars]
    def indices: Seq[Seq[I32]]
    def indexValids: Seq[Seq[Bit]]

    val vectorize = isInnerLoop && spatialConfig.vecInnerLoop
    val Ps: Seq[Int] = cchain.pars.map(_.toInt)
    // Valid bits corresponding to each lane
    protected def createLaneValids(): Seq[Seq[Bit]] = ulanes.map { lane =>
      val laneIdxValids = lane match {
        case List(p) => indexValids.zip(parAddr(p)).map{case (vec,i) => vec(i)}
        case lanes => indexValids.map { vec => vec.head }
      }
      laneIdxValids ++ validBits
    }

    // Substitution for each duplication "lane"
    val contexts: Array[Map[Sym[_], Sym[_]]] = ulanes.map { lane =>
      val inds2 = lane match {
        case List(p) => indices.zip(parAddr(p)).map{case (vec, i) => vec(i) }
        case lanes => indices.map { ind => ind.head }
      }
      Map.empty[Sym[_],Sym[_]] ++ inds.zip(inds2)
    }.toArray

    // Create bound symbols. bound takes in a counter and a list of counter indices. When not
    // vectorize, counter indices is just List(ctr idx), when it's vectorized, it's vec number of
    // indices indicating the counter index for each vectorized lane.
    def createBounds[T<:Bits[_]](bound:(Counter[_], Lane) => T) = {
      cchain.counters.zipWithIndex.map { case (ctr, ci) =>
        val par = if (vectorize) 1 else ctr.ctrPar.toInt
        List.tabulate(par) { i =>
          val ctrIdxs = if (vectorize) List.tabulate(V) { p => parAddr(p)(ci) } else List(i)
          val b = bound(ctr, ctrIdxs)
          b.counter = IndexCounterInfo(ctr, ctrIdxs)
          b
        } 
      }
    }
  }

  case class PartialUnroller(name: String, cchain: CounterChain, inds: Seq[Idx], isInnerLoop: Boolean) extends LoopUnroller {
    lazy val indices: Seq[Seq[I32]] = createBounds { (ctr, lane) => boundVar[I32] }
    lazy val indexValids:  Seq[Seq[Bit]] = createBounds { (ctr, lane) => boundVar[Bit] }
  }

  case class FullUnroller(name: String, cchain: CounterChain, inds: Seq[Idx], isInnerLoop: Boolean) extends LoopUnroller {
    lazy val indices: Seq[Seq[I32]] = createBounds{ 
      case (ctr, List(i)) => I32(ctr.start.toInt + ctr.step.toInt*i)
      case (ctr, ctrIdxs) => stage(FixVecConstNew[TRUE,_32,_0](ctrIdxs.map{i => ctr.start.toInt + ctr.step.toInt*i }))
    }
    lazy val indexValids: Seq[Seq[Bit]] = indices.zip(cchain.counters).map{case (is,ctr) =>
      is.map{
        case Const(i) => Bit(i < ctr.end.toInt)
        case Def(FixVecConstNew(is)) => stage(BitVecConstNew(is.map { _ < ctr.end.toInt }))
      }
    }

  }

  case class UnitUnroller(name: String, isInnerLoop: Boolean) extends Unroller {
    val Ps: Seq[Int] = Seq(1)
    val inds: Seq[Idx] = Nil
    val vectorize = false
    protected def createLaneValids(): Seq[Seq[Bit]] = Seq(Nil)
    val contexts: Array[Map[Sym[_], Sym[_]]] = Array.tabulate(P){_ => Map.empty[Sym[_],Sym[_]] }
  }

}
