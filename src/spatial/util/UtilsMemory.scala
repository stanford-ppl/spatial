package spatial.util

import core._
import forge.tags._
import utils.implicits.collections._
import poly.ISL
import spatial.data._
import spatial.lang._
import spatial.node._

trait UtilsMemory { this: UtilsControl with UtilsHierarchy =>
  implicit class SymMemories(x: Sym[_]) {
    // Weird scalac issue is preventing x.isInstanceOf[C[_,_]]?

    def isLocalMem: Boolean = x match {
      case _: LocalMem[_,_] => true
      case _ => false
    }
    def isRemoteMem: Boolean = x match {
      case _: RemoteMem[_,_] => true
      case _ => false
    }

    def isReg: Boolean = x.isInstanceOf[Reg[_]]
    def isArgIn: Boolean = x.isInstanceOf[ArgIn[_]]
    def isArgOut: Boolean = x.isInstanceOf[ArgOut[_]]

    def isDRAM: Boolean = x match {
      case _:DRAM[_,_] => true
      case _ => false
    }

    def isSRAM: Boolean = x match {
      case _: SRAM[_,_] => true
      case _ => false
    }
    def isRegFile: Boolean = x match {
      case _: RegFile[_,_] => true
      case _ => false
    }
    def isFIFO: Boolean = x.isInstanceOf[FIFO[_]]
    def isLIFO: Boolean = x.isInstanceOf[LIFO[_]]

    def isReader: Boolean = Reader.unapply(x).isDefined
    def isWriter: Boolean = Writer.unapply(x).isDefined

    /**
      * Returns the sequence of enables associated with this symbol
      */
    @stateful def enables: Set[Bit] = x match {
      case Op(d:Enabled[_]) => d.ens
      case _ => Set.empty
    }

    /**
      * Returns true if an execution of access a may occur before one of access b.
      */
    @stateful def mayPrecede(b: Sym[_]): Boolean = {
      val (ctrl,dist) = LCAWithDistance(parentOf(b), parentOf(x))
      dist <= 0 || (dist > 0 && isInLoop(ctrl))
    }

    /**
      * Returns true if an execution of access a may occur after one of access b
      */
    @stateful def mayFollow(b: Sym[_]): Boolean = {
      val (ctrl,dist) = LCAWithDistance(parentOf(b), parentOf(x))
      dist >= 0 || (dist < 0) && isInLoop(ctrl)
    }

    /**
      * Returns true if access b must happen every time the body of controller ctrl is run
      * This case holds when all of the enables of ctrl are true
      * and when b is not contained in a switch within ctrl
      *
      * NOTE: Usable only before unrolling (so enables will not yet include boundary conditions)
      */
    @stateful def mustOccurWithin(ctrl: Ctrl): Boolean = {
      val parents = ctrlParents(parentOf(x))
      val innerParents: Seq[Ctrl] = parents.take(parents.indexOf(ctrl))
      val switches = innerParents.filter{p => isSwitch(p.sym) }
      switches.isEmpty && x.enables.forall{case Const(c) => c.value; case _ => false }
    }

    /**
      * Returns true if access a must always come after access b, relative to some observer access p
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
      val (ctrlA,distA) = LCAWithDistance(x, p) // Positive if a * p, negative otherwise
      val (ctrlB,distB) = LCAWithDistance(b, p) // Positive if b * p, negative otherwise
      val ctrlAB = LCA(x,b).get
      if      (distA > 0 && distB > 0) { distA < distB && x.mustOccurWithin(ctrlAB) }   // b a p
      else if (distA > 0 && distB < 0) { isInLoop(ctrlA) && x.mustOccurWithin(ctrlAB) } // a p b
      else if (distA < 0 && distB < 0) { distA < distB && isInLoop(ctrlA) && x.mustOccurWithin(ctrlAB) } // p b a
      else false
    }
  }


  /** Returns the statically defined rank (number of dimensions) of the given memory. */
  def rankOf(mem: Sym[_]): Int = mem match {
    case Op(m: MemAlloc[_,_]) => m.rank
    case _ => throw new Exception(s"Could not statically determine the rank of $mem")
  }

  /** Returns the statically defined staged dimensions (symbols) of the given memory. */
  def dimsOf(mem: Sym[_]): Seq[I32] = mem match {
    case Op(m: MemAlloc[_,_]) => m.dims
    case _ => throw new Exception(s"Could not statically determine the dimensions of $mem")
  }

  /** Returns constant values of the dimensions of the given memory. */
  def constDimsOf(mem: Sym[_]): Seq[Int] = {
    val dims = dimsOf(mem)
    if (dims.forall{case Expect(c) => true; case _ => false}) {
      dims.collect{case Expect(c) => c.toInt }
    }
    else {
      throw new Exception(s"Could not get constant dimensions of $mem")
    }
  }

  /**
    * Returns iterators between controller containing access (inclusive) and controller containing mem (exclusive).
    * Iterators are ordered outermost to innermost.
    */
  @stateful def accessIterators(access: Sym[_], mem: Sym[_]): Seq[Idx] = {
    (ctrlBetween(parentOf.get(access), parentOf.get(mem)).flatMap(ctrlIters) ++ ctrlIters(Ctrl(access,-1))).distinct
  }

  /**
    * Returns two sets of writers which may be visible to the given reader.
    * The first set contains all writers which always occur before the reader.
    * The second set contains writers which may occur after the reader (but be seen, e.g. in the second iteration of a loop).
    *
    * A write MAY be seen by a reader if it may precede the reader and address spaces intersect.
    */
  @stateful def precedingWrites(read: AccessMatrix, writes: Set[AccessMatrix])(implicit isl: ISL): (Set[AccessMatrix], Set[AccessMatrix]) = {
    //dbgs("  Preceding writers for: ")
    //dbgss(read)

    val preceding = writes.filter{write =>
      val intersects = read intersects write
      val mayPrecede = write.access.mayPrecede(read.access)

      /*if (config.enDbg) {
        val notAfter = !write.access.mayFollow(read.access)
        val (ctrl, dist) = LCAWithDistance(read.parent, write.parent)
        val inLooop = isInLoop(ctrl)
        dbgss(write)
        dbgs(s"     LCA=$ctrl, dist=$dist, inLoop=$inLooop")
        dbgs(s"     notAfter=$notAfter, intersects=$intersects, mayPrecede=$mayPrecede")
      }*/

      intersects && mayPrecede
    }
    preceding.foreach{write => dbgs(s"    ${write.access} {${write.unroll.mkString(",")}}")}
    preceding.partition{write => !write.access.mayFollow(read.access) }
  }

  /**
    * Returns true if the given write is entirely overwritten by a subsequent write PRIOR to the given read
    * (Occurs when another write w MUST follow this write and w contains ALL of the addresses in given write
    */
  @stateful def isKilled(write: AccessMatrix, others: Set[AccessMatrix], read: AccessMatrix)(implicit isl: ISL): Boolean = {
    others.exists{w => w.access.mustFollow(write.access, read.access) && w.isSuperset(write) }
  }

  @stateful def reachingWrites(reads: Set[AccessMatrix], writes: Set[AccessMatrix], isGlobal: Boolean)(implicit isl: ISL): Set[AccessMatrix] = {
    // TODO[5]: Hack, return all writers for global memories for now
    if (isGlobal) return writes

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

  /**
    * Returns whether the associated memory should be considered an accumulator given the set
    * of reads and writes.
    * This looks like an accumulator to buffers if a read and write occurs in the same controller.
    */
  @stateful def accumType(reads: Set[AccessMatrix], writes: Set[AccessMatrix]): AccumType = {
    // TODO[4]: Is read/write in a single control only "accumulation" if the read occurs after the write?
    val isBuffAccum = writes.cross(reads).exists{case (wr,rd) => rd.parent == wr.parent }
    if (isBuffAccum) AccumType.Buff else AccumType.None
  }
}
