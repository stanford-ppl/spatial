package spatial.metadata

import argon._
import argon.node._
import forge.tags._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.retiming._
import poly.{ISL,ConstraintMatrix}

package object access {

  implicit class OpAccessOps(op: Op[_]) {
    // TODO[3]: Should this just be any write?
    def isParEnq: Boolean = op match {
      case _:FIFOBankedEnq[_] => true
      case _:LIFOBankedPush[_] => true
      case _:SRAMBankedWrite[_,_] => true
      case _:FIFOEnq[_] => true
      case _:LIFOPush[_] => true
      case _:SRAMWrite[_,_] => true
      //case _:ParLineBufferEnq[_] => true
      case _ => false
    }

    def isStreamStageEnabler: Boolean = op match {
      case _:FIFODeq[_] => true
      case _:FIFOBankedDeq[_] => true
      case _:LIFOPop[_] => true
      case _:LIFOBankedPop[_] => true
      case _:StreamInRead[_] => true
      case _:StreamInBankedRead[_] => true
      case _ => false
    }

    def isStreamStageHolder: Boolean = op match {
      case _:FIFOEnq[_] => true
      case _:FIFOBankedEnq[_] => true
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

    def isParEnq: Boolean = a.op.exists(_.isParEnq)

    def isStreamStageEnabler: Boolean = a.op.exists(_.isStreamStageEnabler)
    def isStreamStageHolder: Boolean = a.op.exists(_.isStreamStageHolder)

    def isStatusReader: Boolean = StatusReader.unapply(a).isDefined
    def isReader: Boolean = Reader.unapply(a).isDefined || isUnrolledReader
    def isWriter: Boolean = Writer.unapply(a).isDefined || isUnrolledWriter

    def isUnrolledReader: Boolean = UnrolledReader.unapply(a).isDefined
    def isUnrolledWriter: Boolean = UnrolledWriter.unapply(a).isDefined

    /** Returns the sequence of enables associated with this symbol. */
    @stateful def enables: Set[Bit] = a match {
      case Op(d: Enabled[_]) => d.ens
      case _ => Set.empty
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
    @stateful def mustOccurWithin(ctrl: Ctrl): Boolean = {
      val parents = a.ancestors(ctrl)
      val enables = (a +: parents.flatMap(_.s)).flatMap(_.enables)
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
    @stateful def mustFollow(b: Sym[_], p: Sym[_]): Boolean = {
      val (ctrlA,distA) = LCAWithDataflowDistance(a, p) // Positive for a * p, negative otherwise
      val (ctrlB,distB) = LCAWithDataflowDistance(b, p) // Positive for b * p, negative otherwise
      val ctrlAB = LCA(a,b)
      if      (distA > 0 && distB > 0) { distA < distB && a.mustOccurWithin(ctrlAB) }   // b a p
      else if (distA > 0 && distB < 0) { ctrlA.willRunMultiple && a.mustOccurWithin(ctrlAB) } // a p b
      else if (distA < 0 && distB < 0) { distA < distB && ctrlA.willRunMultiple && a.mustOccurWithin(ctrlAB) } // p b a
      else false
    }


    def accessWidth: Int = a match {
      case Op(_:RegFileShiftIn[_,_])        => 1
      case Op(op:RegFileShiftInVector[_,_]) => op.data.width
      case Op(ua: UnrolledAccessor[_,_])    => ua.width
      case Op(RegWrite(_,_,_)) => 1
      case Op(RegRead(_))      => 1
      case _ => -1
    }

    def shiftAxis: Option[Int] = a match {
      case Op(op: RegFileShiftIn[_,_])       => Some(op.axis)
      case Op(op: RegFileBankedShiftIn[_,_]) => Some(op.axis)
      case Op(op: RegFileShiftInVector[_,_]) => Some(op.axis)
      case _ => None
    }

    def banks: Seq[Seq[Idx]] = a match {
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
    val memoryIters = mem.scopes.filterNot(_.stage == -1).flatMap(_.iters)
    val accessIters = access.scopes.filterNot(_.stage == -1).flatMap(_.iters)

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

  @rig def flatIndex(indices: Seq[I32], dims: Seq[I32]): I32 = {
    val strides = List.tabulate(dims.length){d => dims.drop(d+1).prodTree }
    indices.zip(strides).map{case (a,b) => a.to[I32] * b }.sumTree
  }

}
