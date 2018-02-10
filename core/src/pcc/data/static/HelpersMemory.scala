package pcc.data.static

import forge._
import pcc.core._
import pcc.data._
import pcc.node._
import pcc.lang._
import pcc.poly.ISL

trait HelpersMemory { this: HelpersControl =>
  implicit class SymMemories(x: Sym[_]) {
    def isLocalMem: Boolean = x.isInstanceOf[LocalMem[_,C forSome {type C[_]}]] // ... yep
    def isRemoteMem: Boolean = x.isInstanceOf[RemoteMem[_,C forSome {type C[_]}]]

    def isReg: Boolean = x.isInstanceOf[Reg[_]]
    def isArgIn: Boolean = x.op.exists{op => op.isInstanceOf[ArgInNew[_]]}
    def isArgOut: Boolean = x.op.exists{op => op.isInstanceOf[ArgOutNew[_]]}

    def isSRAM: Boolean = x.isInstanceOf[SRAM[_]]
    def isFIFO: Boolean = x.isInstanceOf[FIFO[_]]
    def isLIFO: Boolean = x.isInstanceOf[LIFO[_]]

    def isReader: Boolean = Reader.unapply(x).isDefined
    def isWriter: Boolean = Writer.unapply(x).isDefined

    /**
      * Returns the sequence of enables associated with this symbol
      */
    @stateful def enables: Seq[Bit] = x match {
      case Op(d:EnabledControl) => d.ens
      case Op(d:EnPrimitive[_]) => d.ens
      case _ => Nil
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
      switches.isEmpty && x.enables.forall{case Const(c: Boolean) => c; case _ => false }
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


  /**
    * Returns the statically defined rank (number of dimensions) of the given memory.
    */
  def rankOf[C[_]](mem: Mem[_,C]): Int = mem match {
    case Op(m: MemAlloc[_]) => m.rank
    case _ => throw new Exception(s"Could not statically determine the rank of $mem")
  }

  /**
    * Returns the statically defined staged dimensions (symbols) of the given memory.
    */
  def dimsOf[C[_]](mem: Mem[_,C]): Seq[I32] = mem match {
    case Op(m: MemAlloc[_]) => m.dims
    case _ => throw new Exception(s"Could not statically determine the dimensions of $mem")
  }

  /**
    * Returns iterators between controller containing access (inclusive) and controller containing mem (exclusive).
    * Iterators are ordered outermost to innermost.
    */
  @stateful def accessIterators(access: Sym[_], mem: Sym[_]): Seq[I32] = {
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
    dbgs("  Preceding writers for: ")
    dbgss(read)

    val preceding = writes.filter{write =>
      val intersects = read intersects write
      val mayPrecede = write.access.mayPrecede(read.access)

      if (config.enDbg) {
        val notAfter = !write.access.mayFollow(read.access)
        val (ctrl, dist) = LCAWithDistance(read.parent, write.parent)
        val inLooop = isInLoop(ctrl)
        dbgss(write)
        dbgs(s"     LCA=$ctrl, dist=$dist, inLoop=$inLooop")
        dbgs(s"     notAfter=$notAfter, intersects=$intersects, mayPrecede=$mayPrecede")
      }

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
    // TODO: Hack, return all writers for global memories for now
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
    * TODO: Is this only true if the read occurs after the write?
    */
  @stateful def accumType(reads: Set[AccessMatrix], writes: Set[AccessMatrix]): AccumType = {
    val isBuffAccum = writes.cross(reads).exists{case (wr,rd) => rd.parent == wr.parent }
    if (isBuffAccum) AccumType.Buff else AccumType.None
  }

}
