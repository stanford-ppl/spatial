package spatial.metadata

import argon._
import argon.node._
import forge.tags._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.math._
import spatial.metadata.retiming._
import poly.{ISL,ConstraintMatrix}
import emul.ResidualGenerator._

package object access {

  implicit class OpAccessOps(op: Op[_]) {
    // TODO[3]: Should this just be any write?

    def getLockNode: Option[LockWithKeys[I32]] = op match {
      case x: LockSRAMRead[_,_] => x.lock
      case x: LockSRAMWrite[_,_] => x.lock
      case x: LockDRAMRead[_,_] => x.lock
      case x: LockDRAMWrite[_,_] => x.lock
//      case x: LockSRAMBankedRead[_,_] => x.lock
//      case x: LockSRAMBankedWrite[_,_] => x.lock
      case _ => None
    }

    def isParEnq: Boolean = op match {
      // case _:LineBufferBankedEnq[_] => true
      case _:FIFOBankedEnq[_] => true
      case _:MergeBufferEnq[_] => true
      case _:MergeBufferBankedEnq[_] => true
      case _:LIFOBankedPush[_] => true
      case _:SRAMBankedWrite[_,_] => true
      case _:FIFOEnq[_] => true
      case _:LIFOPush[_] => true
      case _:SRAMWrite[_,_] => true
      //case _:ParLineBufferEnq[_] => true
      case _ => false
    }

    def isPriorityDeq: Boolean = op match {
      case _:FIFOBankedPriorityDeq[_] => true
      case _ => false
    }

    def isVectorAccess: Boolean = op match {
      case _: FIFOVecDeq[_] => true
      case _: FIFOVecEnq[_] => true
      case _ => false
    }

    def isDeqInterface: Boolean = op match {
      case _: FIFODeqInterface[_] => true
      case _ => false
    }

    def isStreamStageEnabler: Boolean = op match {
      case _:FIFODeq[_] => true
      case _:FIFORegDeq[_] => true
      case _:FIFOBankedDeq[_] => true
      case _:MergeBufferDeq[_] => true
      case _:MergeBufferBankedDeq[_] => true
      case _:LIFOPop[_] => true
      case _:LIFOBankedPop[_] => true
      case _:StreamInRead[_] => true
      case _:StreamInBankedRead[_] => true
      case _ => false
    }

    def isStreamStageHolder: Boolean = op match {
      case _:FIFOEnq[_] => true
      case _:FIFORegEnq[_] => true
      case _:FIFOBankedEnq[_] => true
      case _:MergeBufferEnq[_] => true
      case _:MergeBufferBankedEnq[_] => true
      case _:LIFOPush[_] => true
      case _:LIFOBankedPush[_] => true
      case _:StreamOutWrite[_] => true
      case _:StreamOutBankedWrite[_] => true
      case _ => false
    }
  }

  implicit class UsageOps(s: Sym[_]) {
    def isUnusedAccess: Boolean = metadata[UnusedAccess](s).exists(_.flag)
    def isUnusedAccess_=(flag: Boolean): Unit = metadata.add(s, UnusedAccess(flag))

    def users: Set[User] = metadata[Users](s).map(_.users).getOrElse(Set.empty)
    def users_=(use: Set[User]): Unit = metadata.add(s, Users(use))

    def readUses: Set[Sym[_]] = metadata[ReadUses](s).map(_.reads).getOrElse(Set.empty)
    def readUses_=(use: Set[Sym[_]]): Unit = metadata.add(s, ReadUses(use))
  }

  implicit class AccessPatternOps(s: Sym[_]) {
    def getAccessPattern: Option[Seq[AddressPattern]] = metadata[AccessPattern](s).map(_.pattern)
    def accessPattern: Seq[AddressPattern] = getAccessPattern.getOrElse{throw new Exception(s"No access pattern defined for $s")}
    def accessPattern_=(pattern: Seq[AddressPattern]): Unit = metadata.add(s, AccessPattern(pattern))
  }


  implicit class AffineDataOps(s: Sym[_]) {
    def getAffineMatrices: Option[Seq[AccessMatrix]] = metadata[AffineMatrices](s).map(_.matrices)
    def affineMatrices: Seq[AccessMatrix] = getAffineMatrices.getOrElse(Nil)
    def affineMatrices_=(matrices: Seq[AccessMatrix]): Unit = metadata.add(s, AffineMatrices(matrices))

    def getDomain: Option[ConstraintMatrix[Idx]] = metadata[Domain](s).map(_.domain)
    def domain: ConstraintMatrix[Idx] = getDomain.getOrElse{ ConstraintMatrix.empty }
    def domain_=(d: ConstraintMatrix[Idx]): Unit = metadata.add(s, Domain(d))

    def getOrElseUpdateDomain(els: => ConstraintMatrix[Idx]): ConstraintMatrix[Idx] = getDomain match {
      case Some(domain) => domain
      case None =>
        s.domain = els
        s.domain
    }
  }

  implicit class AccessOps(a: Sym[_]) {
    def prDeqGrp: Option[Int] = metadata[PriorityDeqGroup](a).map(_.grp)
    def prDeqGrp_=(grp: Int): Unit = metadata.add(a, PriorityDeqGroup(grp))

    def stuffed: Boolean = metadata[StuffedFIFO](a).exists(_.flag)
    def stuffed_=(flag: Boolean): Unit = metadata.add(a, StuffedFIFO(flag))

    def isParEnq: Boolean = a.op.exists(_.isParEnq)
    def isVectorAccess: Boolean = a.op.exists(_.isVectorAccess)
    def isDeqInterface: Boolean = a.op.exists(_.isDeqInterface)
    def isArgInRead: Boolean = a match {case Op(RegRead(Op(ArgInNew(_)))) => true; case _ => false}

    def isStreamStageEnabler: Boolean = a.op.exists(_.isStreamStageEnabler)
    def isStreamStageHolder: Boolean = a.op.exists(_.isStreamStageHolder)

    def isStatusReader: Boolean = StatusReader.unapply(a).isDefined
    def isReader: Boolean = Reader.unapply(a).isDefined || isUnrolledReader
    def isWriter: Boolean = Writer.unapply(a).isDefined || isUnrolledWriter || isSpecialWriter

    def isSpecialWriter: Boolean = a match {
      case Op(_:RegAccum[_]) => true
      case _ => false
    }
    def isUnrolledReader: Boolean = UnrolledReader.unapply(a).isDefined
    def isUnrolledWriter: Boolean = UnrolledWriter.unapply(a).isDefined

    def parOrElse1: Int = a match {
      case Op(x: UnrolledAccessor[_,_]) => x.enss.size
      case _ => 1
    }
    def isPeek: Boolean = a match {
      case Op(_:FIFOPeek[_]) => true
      case _ => false
    }

    @stateful def residualGenerators: List[List[ResidualGenerator]] = {
      if (a.banks.isEmpty) List(List(ResidualGenerator(1,0,0))) else a.banks.map(_.map(_.residual).toList).toList
    }

    /** Returns the sequence of enables associated with this symbol. */
    @stateful def enables: Set[Bit] = a match {
      case Op(d: Enabled[_]) => d.ens
      case _ => Set.empty
    }

    /** Returns the memory written to by this symbol, if applicable. */
    @stateful def writtenMem: Option[Sym[_]] = a match {
      case Op(d: Writer[_]) => Some(d.mem)
      case Op(d: VectorWriter[_]) => Some(d.mem)
      case Op(d: RegAccum[_]) => Some(d.mem)
      case _ => None
    }

    /** Returns the memory read from by this symbol, if applicable. */
    @stateful def readMem: Option[Sym[_]] = a match {
      case Op(d: Reader[_,_]) => Some(d.mem)
      case Op(d: VectorReader[_]) => Some(d.mem)
      case _ => None
    }

    /** Returns true if an execution of access a may occur before one of access b. */
    @stateful def mayPrecede(b: Sym[_]): Boolean = {
      val (ctrl,dist) = LCAWithDataflowDistance(b.parent, a.parent)
      dist <= 0 || (dist > 0 && ctrl.willRunMultiple)
    }

    /** Returns true if an execution of access a may occur after one of access b. */
    @stateful def mayFollow(b: Sym[_]): Boolean = {
      val (ctrl,dist) = LCAWithDataflowDistance(b.parent, a.parent)
      dist >= 0 || (dist < 0) && ctrl.willRunMultiple
    }

    /** Returns true if access b must happen every time the body of controller ctrl is run
      * This case holds when all of the enables of ctrl are true
      * and when b is not contained in a switch within ctrl
      *
      * NOTE: Usable only before unrolling (so enables will not yet include boundary conditions)
      */
    @stateful def mustOccurWithin(ctrl: Ctrl, considerSelfEns: Boolean = true): Boolean = {
      val parents = a.ancestors(ctrl)
      val consideredNodes = (if (considerSelfEns) Seq(a) else Seq.empty) ++ parents.flatMap(_.s)
      val enables = consideredNodes.flatMap(_.enables)
      !parents.exists(_.isSwitch) && enables.forall{case Const(b) => b.value; case _ => false }
    }

    /** Returns true if access a must always come after access b, relative to some observer access p
      * NOTE: Usable only before unrolling
      *
      * For orderings:
      *   0. b p a - false
      *   1. a b p - false
      *   2. p a b - false
      *   3. b a p - true if b does not occur within a switch
      *   4. a p b - true if in a loop and b does not occur within a switch
      *   5. p b a - true if b does not occur within a switch
      */
    @stateful def mustFollow(b: Sym[_], p: Sym[_], considerSelfEns: Boolean = true): Boolean = {
      val (ctrlA,distA) = LCAWithDataflowDistance(a, p) // Positive for a * p, negative otherwise
      val (ctrlB,distB) = LCAWithDataflowDistance(b, p) // Positive for b * p, negative otherwise

      dbgs(s"Mustfollow: $b -> $a -> $p [$considerSelfEns]")

      val isParA = ctrlA.isCtrl(sched = ForkJoin) || ctrlA.isStreamControl

      indent {
        dbgs(s"CtrlA: $ctrlA [$distA]")
        dbgs(s"CtrlB: $ctrlB [$distB]")
        dbgs(s"IsStream: $isParA")
      }

      // if LCA(a, p) == LCA(a, b), then we can directly compare the dataflow distances as yielded.
      if (ctrlA == ctrlB) {
        if (isParA) {
          // If the LCA is a parallel or streamed controller, then there's no ordering between them.
          return false
        }

        val ctrlAB = LCA(a, b)
        indent { dbgs(s"ctrlAB: $ctrlAB") }
        if (distA > 0 && distB > 0) {
          distA < distB && a.mustOccurWithin(ctrlAB, considerSelfEns)
        } // b a p
        else if (distA > 0 && distB < 0) {
          ctrlA.willRunMultiple && a.mustOccurWithin(ctrlAB, considerSelfEns)
        } // a p b
        else if (distA < 0 && distB < 0) {
          distA < distB && ctrlA.willRunMultiple && a.mustOccurWithin(ctrlAB, considerSelfEns)
        } // p b a
        else false
      } else {
        // LCA(a, p) != LCA(a, b)
        // This means that one of the accesses is "closer" in the hierarchy to p.
        // A mustFollow B w.r.t. P if A is at a closer level of the hierarchy, A happens before P
        // and A must occur within LCA(a, p)
        val aIsCloser = ctrlA.hasAncestor(ctrlB)
        if (!aIsCloser) {
          // If LCA-B is closer, then it's not possible UNLESS there's a stream/parallel since those are unordered
          false
        } else {
          // If A is closer in the hierarchy to p, then A will happen before P if:
          //   -- A happens before P
          //   -- A is unconditional
          //   -- ctrlA is NOT a parallel or stream controller

          !isParA && ((distA > 0) && a.mustOccurWithin(ctrlA, considerSelfEns))
        }
      }
    }


    def accessWidth: Int = a match {
      case Op(_:RegFileShiftIn[_,_])        => 1
      case Op(op:RegFileShiftInVector[_,_]) => op.data.width
      case Op(ua: UnrolledAccessor[_,_])    => ua.width
      case Op(MergeBufferBound(_,_,_,_)) => 1
      case Op(MergeBufferInit(_,_,_)) => 1
      case Op(RegWrite(_,_,_)) => 1
      case Op(RegRead(_))      => 1
      case Op(FIFORegDeq(_,_))      => 1
      case Op(FIFORegEnq(_,_,_))      => 1
      case _ => -1
    }

    def shiftAxis: Option[Int] = a match {
      case Op(op: RegFileShiftIn[_,_])       => Some(op.axis)
      case Op(op: RegFileBankedShiftIn[_,_]) => Some(op.axis)
      case Op(op: RegFileShiftInVector[_,_]) => Some(op.axis)
      case _ => None
    }

    def banks: Seq[Seq[Idx]] = a match {
      case Op(op@RegFileVectorWrite(_,_,addr,_)) => addr
      case Op(op@RegFileVectorRead(_,addr,_)) => addr
      case Op(op@RegFileShiftIn(_,_,addr,_,_)) => Seq(addr)
      case BankedReader(_,banks,_,_)   => banks
      case BankedWriter(_,_,banks,_,_) => banks
      case _ => Seq(Seq())
    }

    def isDirectlyBanked: Boolean = {
      if (a.banks.toList.flatten.isEmpty) false
      else if (a.banks.head.forall(_.asInstanceOf[Sym[_]].trace.isConst)) true
      else false
    }
  }

  @stateful def getDephasedUID(iters: Iterable[Idx], fullUID: Seq[Int], position: Int): Seq[Int] = {
    val pomMask = iters.map{i => i.parent.s.get.willUnrollAsPOM}.toSeq
    pomMask.zipWithIndex.collect{case (take,i) if i < position || take => fullUID(i)}
  }

  /** Generate replacement rules for access a relative to access b */
  @stateful def divergedIters(a: AccessMatrix, b: AccessMatrix, mem: Sym[_]): Map[Idx,Option[Int]] = {
    if (accessIterators(a.access, mem) != accessIterators(b.access, mem)) Map()
    else dephasingIters(a, b.unroll, mem).map{case ((iter,uid),ofs) => (iter -> ofs)}.toMap
  }

  /** Checks the iters in access a for those which can dephase due to controllers not being synchronized with base uid.  
    * Returns a mapping between iter, uid to the ofs to include for that iter.  If the offset is None, assume total dephasing 
    *
    * We can use this info to create replacement rules for each iter in each access that may conflict due to lockstep dephasing
    */
  @stateful def dephasingIters(a: AccessMatrix, baseUID: Seq[Int], mem: Sym[_]): Map[(Idx,Seq[Int]),Option[Int]] = {
    val aIters: Seq[Idx] = accessIterators(a.access, mem)
    if (aIters.nonEmpty) {
      val leaf = a.access.parent.s.get
      dbgs(s"Dephasing Iters: $a -> $leaf")
      // TODO: iterSynchronizationInfo should be standalone helper, not a func defined on a Sym!
      val ofsRules = if (aIters.nonEmpty) leaf.iterSynchronizationInfo(leaf, aIters, baseUID, a.unroll) else Map[Idx,Int]()
      aIters.map{i => ((i,a.unroll) -> ofsRules.get(i))}.toMap
    } else Map() 
  }


  /** Returns iterators between controller containing access (inclusive) and controller
    * containing mem (exclusive). Iterators are ordered outermost to innermost.
    */
  def accessIterators(access: Sym[_], mem: Sym[_]): Seq[Idx] = {
    // Use the parent "master" controller when checking for access iterators if the access and
    // memory are in different sub-controllers.
    // This is to account for memories defined, e.g. in the map (block 0) of a MemReduce
    // with the access defined in the second block.
    //
    // CASE 1: Direct hierarchy
    // Foreach(-1,-1)
    //   Foreach(0,0)
    //     *Alloc
    //     Reduce(-1,-1)
    //       Reduce(0,0)
    //         *Access
    // Want: Reduce(-1,-1) Reduce(0,0)
    // access.scopes(stop = mem.scope) = Reduce(-1,-1) Reduce(0,0)
    // access.scopes(stop = mem.scope.master) = Foreach(0,0) Reduce(-1,-1) Reduce(0,0)
    //
    // CASE 2: Access across subcontrollers
    // Foreach(-1,-1)
    //   Foreach(0,0)
    //     MemReduce(-1,-1)
    //       MemReduce(0,0)  -- MemReduce(0)
    //         *Alloc
    //       MemReduce(1,0)  -- MemReduce(1)
    //         *Access
    // Want: MemReduce(1,0) [STOP]
    // access.scopes(stop = mem.scope) = Accel Foreach(-1,-1) Foreach(0,0) MemReduce(-1) MemReduce(1)
    // access.scopes(stop = mem.scope.master) = MemReduce(1)
    val memoryIters = mem.scopes.filterNot(_.stage == -1).flatMap(_.iters).filter(!_.counter.ctr.isForever)
    val accessIters = access.scopes.filterNot(_.stage == -1).flatMap(_.iters).filter(!_.counter.ctr.isForever)

    accessIters diff memoryIters
  }

  /** Returns two sets of writers which may be visible to the given reader.
    * The first set contains all writers which always occur before the reader.
    * The second set contains writers which may occur after the reader (but be seen, e.g. in the
    * second iteration of a loop).
    *
    * A write MAY be seen by a reader if it may precede the reader and address spaces intersect.
    */
  @stateful def precedingWrites(read: AccessMatrix, writes: Set[AccessMatrix])(implicit isl: ISL): (Set[AccessMatrix], Set[AccessMatrix]) = {
    val preceding = writes.filter{write =>
      val intersects = read intersects write
      val mayPrecede = write.access.mayPrecede(read.access)

      intersects && mayPrecede
    }
    dbgs(s"  Preceding writes for ${read.short}: ")
    preceding.foreach{write => dbgs(s"    ${write.short}")}
    preceding.partition{write => !write.access.mayFollow(read.access) }
  }

  /** Returns true if the given write is entirely overwritten by a subsequent write PRIOR to the given read
    * (Occurs when another write w MUST follow this write and w contains ALL of the addresses in given write
    */
  @stateful def isKilled(write: AccessMatrix, others: Set[AccessMatrix], read: AccessMatrix)(implicit isl: ISL): Boolean = {
    others.exists{w => w.access.mustFollow(write.access, read.access) && w.isSuperset(write) }
  }

  @stateful def reachingWrites(reads: Set[AccessMatrix], writes: Set[AccessMatrix], isGlobal: Boolean)(implicit isl: ISL): Set[AccessMatrix] = {
    // TODO[5]: Hack, return all writers for global/unread memories for now
    if (isGlobal || reads.isEmpty) return writes

    var remainingWrites: Set[AccessMatrix] = writes
    var reachingWrites: Set[AccessMatrix] = Set.empty

    reads.foreach{read =>
      val (before, after) = precedingWrites(read, remainingWrites)

      val reachingBefore = before.filterNot{wr => isKilled(wr, before - wr, read) }
      val reachingAfter  = after.filterNot{wr => isKilled(wr, (after - wr) ++ before, read) }
      val reaching = reachingBefore ++ reachingAfter
      remainingWrites --= reaching
      reachingWrites ++= reaching
    }
    reachingWrites
  }

  @stateful def reachingWritesToReg(read: Sym[_], writes: Set[Sym[_]], writesAlwaysKill: Boolean = false): Set[Sym[_]] = {
    // TODO: Handle the difference between .buffer and .nonbuffer
    // To compute this -- a write to a register is a Reaching Write IFF it's not killed by another.
    val relevant = writes.filter(_.mayPrecede(read))
    dbgs(s"Relevant: $relevant")
    val alive = relevant.filterNot { write => (writes - write).exists {
      killer =>
        val result = killer.mustFollow(write, read, !writesAlwaysKill)
        indent { dbgs(s"$write killed by $killer: [$result]") }
        result
    }}
    alive
  }

  @rig def flatIndex(indices: Seq[I32], dims: Seq[I32]): I32 = {
    val strides = List.tabulate(dims.length){d => dims.drop(d+1).prodTree }
    indices.zip(strides).map{case (a,b) => a.to[I32] * b }.sumTree
  }

}
