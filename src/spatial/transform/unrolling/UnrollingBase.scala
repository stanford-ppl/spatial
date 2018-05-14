package spatial.transform.unrolling

import argon._
import argon.transform.MutateTransformer
import utils.tags.instrument
import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.util._
import spatial.internal.spatialConfig
import spatial.traversal.AccelTraversal

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
  var unrollNum: Map[Idx, Int] = Map.empty
  def withUnrollNums[A](ind: Seq[(Idx, Int)])(blk: => A): A = {
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
  def withMemories[A](mems: Seq[((Sym[_],Int), Sym[_])])(blk: => A): A = {
    val saveMems = memories
    memories ++= mems
    val result = blk
    memories = saveMems
    result
  }



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
    logs(s"Unrolling $lhs = $rhs")
    if (rhs.isControl) duplicateController(lhs,rhs)
    else lanes.duplicate(lhs,rhs)
  }

  def unrollCtrl[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[_] = {
    val lhs2 = cloneOp(lhs,rhs)
    dbgs(s"Created ${stm(lhs2)}")
    lhs2
  }

  /** Duplicate the controller using the given call-by-name unroll function.
    * Duplication is done based on the global Unroller helper instance lanes.
    */
  final def duplicateController[A:Type](lhs: Sym[A], rhs: Op[A]): List[Sym[_]] = {
    dbgs(s"Duplicating controller $lhs = $rhs")
    def duplicate() = transferMetadataIfNew(lhs){ unrollCtrl(lhs,rhs).asInstanceOf[Sym[A]] }._1
    if (lanes.size > 1) {
      val block = stageBlock {
        lanes.foreach{p =>
          dbgs(s"$lhs = $rhs [duplicate ${p+1}/${lanes.size}]")
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
      val first = lanes.inLane(0){ duplicate() }
      lanes.unify(lhs, first)
    }
  }

  final override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case _:AccelScope => inAccel{ super.transform(lhs,rhs) }
    case _ =>
      val duplicates: List[Sym[_]] = if (rhs.isControl) duplicateController(lhs,rhs) else unroll(lhs, rhs)
      if (duplicates.length == 1) duplicates.head.asInstanceOf[Sym[A]]
      else Invalid.asInstanceOf[Sym[A]]
  }


  def inReduce[T](red: Option[ReduceFunction], isInner: Boolean)(blk: => T): T = duringMirror{e =>
    if (spatialConfig.noInnerLoopUnroll && !isInner) e.reduceType = None
    else e.reduceType = red
  }{ blk }


  override protected def inlineBlock[T](block: Block[T]): Sym[T] = {
    inlineBlockWith(block){stms =>
      stms.foreach(visit)
      lanes.inLane(0){ f(block.result) }
    }
  }

  def cloneOp[A](lhs: Sym[A], rhs: Op[A]): Sym[A] = {
    val (lhs2, isNew) = transferMetadataIfNew(lhs){ mirror(lhs, rhs) }
    if (isNew) mirrorFuncs.foreach{func => func(lhs2) }

    //logs(s"Cloning $lhs = $rhs")
    //logs(s"  Created ${stm(lhs2)}")
    lhs2
  }

  override def mirrorNode[A](rhs: Op[A]): Op[A] = rhs match {
    case e:Enabled[_] => e.mirrorEn(f, enables).asInstanceOf[Op[A]]
    case _ => super.mirrorNode(rhs)
  }
  override def updateNode[A](node: Op[A]): Unit = node match {
    case e:Enabled[_] => e.updateEn(f, enables)
    case _ => super.updateNode(node)
  }


  /** Helper objects for unrolling
    * Tracks multiple substitution contexts in 'contexts' array
    */
  trait Unroller {
    type MemContext = ((Sym[_],Int), Sym[_])
    val name: String

    def inds: Seq[Idx]
    def Ps: Seq[Int]

    def P: Int = Ps.product
    def N: Int = Ps.length
    def size: Int = P
    def prods: List[Int] = List.tabulate(N){i => Ps.slice(i+1,N).product }
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

    def inLanes[A](lns: Seq[Int])(block: Int => A): Seq[A] = lns.map{ln => inLane(ln)(block(ln)) }

    def inLane[A](i: Int)(block: => A): A = {
      val save     = subst
      val saveMems = memories
      val addr     = parAddr(i)
      withMemories(memContexts(i)) {
        withUnrollNums(inds.zip(addr)) {
          dbgs(s"Entering $name lane #$i: ")
          //memContexts(i).foreach{case (k,v) => dbgs(s"  $k -> $v") }
          contexts(i).foreach{case (k,v) => dbgs(s"  $k -> $v") }
          dbgs(s"---")
          //memories.foreach{case (k,v) => dbgs(s"  $k -> $v") }


          val result = isolateSubstWith(contexts(i)) {
            withValids(valids(i)) {

              subst.foreach{case (k,v) => dbgs(s"  $k -> $v") }

              val result = block
              // Retain only the substitutions added within this scope
              contexts(i) ++= subst.filterNot(save contains _._1)
              memContexts(i) ++= memories.filterNot(saveMems contains _._1)

              dbgs(s"Exiting $name lane #$i: ")
              //memContexts(i).foreach{case (k,v) => dbgs(s"  $k -> $v") }
              contexts(i).foreach{case (k,v) => dbgs(s"  $k -> $v") }
              dbgs(s"---")

              result
            }
          }

          subst.foreach{case (k,v) => dbgs(s"  $k -> $v") }
          //memories.foreach{case (k,v) => dbgs(s"  $k -> $v") }

          result
        }
      }
    }

    def map[A](block: Int => A): List[A] = List.tabulate(P){p => inLane(p){ block(p) } }

    def foreach(block: Int => Unit): Unit = { map(block) }

    // --- Each unrolling rule should do at least one of three things:

    // 1. Split a given vector as the substitution for the single original symbol
    def duplicate[A](s: Sym[A], d: Op[A]): List[Sym[_]] = {
      if (size > 1 || shouldCopy) map{_ =>
        val s2 = cloneOp(s, d)
        register(s -> s2)
        s2
      }
      else inLane(0){
        List(update(s, d))
      }
    }
    // 2. Make later stages depend on the given substitution across all lanes
    // NOTE: This assumes that the node has no meaningful return value (i.e. all are Pipeline or Unit)
    // Bad things can happen here if you're not careful!
    def split[T:Type](orig: Sym[T], vec: Vec[T])(implicit ctx: SrcCtx): List[T] = map{p =>
      val element = vec(p)
      register(orig -> element)
      element
    }
    def splitLanes[T:Type](lns: List[Int])(orig: Sym[_], vec: Vec[T])(implicit ctx: SrcCtx): List[T] = {
      lns.zipWithIndex.map{case (ln,i) =>
        inLane(ln){
          val element = vec(i)
          register(orig -> element)
          element
        }
      }
    }

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

    def duplicateMem(mem: Sym[_])(blk: Int => Seq[(Sym[_],Int)]): Unit = foreach{p =>
      val duplicates = blk(p)
      dbgs(s"  Registering duplicates for memory: $mem")
      memContexts(p) ++= duplicates.map{case (mem2,d) =>
        dbgs(s"  ($mem,$d) -> $mem2")
        (mem,d) -> mem2
      }
    }

    // Same symbol for all lanes
    def isCommon(e: Sym[_]): Boolean = contexts.map{p => f(e)}.forall{e2 => e2 == f(e)}
  }


  case class PartialUnroller(name: String, cchain: CounterChain, inds: Seq[Idx], isInnerLoop: Boolean) extends Unroller {
    // HACK: Don't unroll inner loops for CGRA generation
    val Ps: Seq[Int] = if (isInnerLoop && spatialConfig.noInnerLoopUnroll) inds.map{_ => 1}
                       else cchain.pars.map(_.toInt)

    val fs: Seq[Boolean] = cchain.counters.map(_.isForever)

    val indices: Seq[Seq[I32]] = Ps.map{p => List.fill(p){ boundVar[I32] }}
    val indexValids:  Seq[Seq[Bit]] = Ps.map{p => List.fill(p){ boundVar[Bit] }}

    // Valid bits corresponding to each lane
    protected def createLaneValids(): Seq[Seq[Bit]] = List.tabulate(P){p =>
      val laneIdxValids = indexValids.zip(parAddr(p)).map{case (vec,i) => vec(i)}
      laneIdxValids ++ validBits
    }

    // Substitution for each duplication "lane"
    val contexts: Array[Map[Sym[_],Sym[_]]] = Array.tabulate(P){p =>
      val inds2 = indices.zip(parAddr(p)).map{case (vec, i) => vec(i) }
      Map.empty[Sym[_],Sym[_]] ++ inds.zip(inds2)
    }
  }



  case class FullUnroller(name: String, cchain: CounterChain, inds: Seq[Idx], isInnerLoop: Boolean) extends Unroller {
    val Ps: Seq[Int] = cchain.pars.map(_.toInt)

    val indices: Seq[Seq[I32]] = cchain.counters.map{ctr =>
      List.tabulate(ctr.ctrPar.toInt){i => I32(ctr.start.toInt + ctr.step.toInt*i) }
    }
    val indexValids: Seq[Seq[Bit]] = indices.zip(cchain.counters).map{case (is,ctr) =>
      is.map{case Const(i) => Bit(i < ctr.end.toInt) }
    }

    protected def createLaneValids(): Seq[Seq[Bit]] = List.tabulate(P){p =>
      val laneIdxValids = indexValids.zip(parAddr(p)).map{case (vec,i) => vec(i) }
      laneIdxValids ++ validBits
    }

    val contexts: Array[Map[Sym[_], Sym[_]]] = Array.tabulate(P){p =>
      val inds2 = indices.zip(parAddr(p)).map{case (vec, i) => vec(i) }
      Map.empty[Sym[_],Sym[_]] ++ inds.zip(inds2)
    }
  }

  case class UnitUnroller(name: String, isInnerLoop: Boolean) extends Unroller {
    val Ps: Seq[Int] = Seq(1)
    val inds: Seq[I32] = Nil
    val indices: Seq[Seq[I32]] = Seq(Nil)
    val indexValids: Seq[Seq[Bit]] = Seq(Nil)
    protected def createLaneValids(): Seq[Seq[Bit]] = Seq(Nil)
    val contexts: Array[Map[Sym[_], Sym[_]]] = Array.tabulate(1){_ => Map.empty[Sym[_],Sym[_]] }
  }

}
