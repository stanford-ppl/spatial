package pcc.data.static

import forge._
import pcc.core._
import pcc.data._
import pcc.node._
import pcc.lang._

trait HelpersMemory { this: HelpersControl =>

  def rankOf[C[_]](mem: Mem[_,C]): Int = mem match {
    case Op(m: MemAlloc[_]) => m.rank
    case _ => throw new Exception(s"Could not statically determine the rank of $mem")
  }
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
  }

}
