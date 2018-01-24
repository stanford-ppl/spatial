package pcc

import forge._
import pcc.core._
import pcc.lang._
import pcc.node._

import scala.language.existentials
import scala.collection.mutable.ListBuffer

package object data {
  def isControl(sym: Sym[_]): Boolean = sym.op.exists(isControl)
  def isControl(op: Op[_]): Boolean = op.isInstanceOf[Control]

  def isPrimitive(sym: Sym[_]): Boolean = sym.op.exists(isPrimitive)
  def isPrimitive(op: Op[_]): Boolean = op.isInstanceOf[Primitive[_]]

  def isStateless(sym: Sym[_]): Boolean = sym.op.exists(isStateless)
  def isStateless(op: Op[_]): Boolean = op match {
    case p: Primitive[_] => p.isStateless
    case _ => false
  }

  def isAccel(sym: Sym[_]): Boolean = sym.op.exists(isAccel)
  def isAccel(op: Op[_]): Boolean = op.isInstanceOf[AccelScope]

  @api def isInnerControl(sym: Sym[_]): Boolean = isControl(sym) && !isOuter(sym)
  @api def isOuterControl(sym: Sym[_]): Boolean = isControl(sym) && isOuter(sym)

  def getCChains(block: Block[_]): Seq[CounterChain] = getCChains(block.stms)
  def getCChains(stms: Seq[Sym[_]]): Seq[CounterChain] = stms.collect{case s: CounterChain => s}

  implicit class SymUtils(x: Sym[_]) {
    def isNum:  Boolean = x.isInstanceOf[Num[_]]
    def isBits: Boolean = x.isInstanceOf[Bits[_]]
    def isVoid: Boolean = x.isInstanceOf[Void]
    def isLocalMem: Boolean = x.isInstanceOf[LocalMem[_,C forSome {type C[_]}]] // ... yep
    def isRemoteMem: Boolean = x.isInstanceOf[RemoteMem[_,C forSome {type C[_]}]]

    def isReg: Boolean = x.isInstanceOf[Reg[_]]
    def isArgIn: Boolean = x.op.exists(_.isInstanceOf[ArgInNew[_]])
    def isArgOut: Boolean = x.op.exists(_.isInstanceOf[ArgOutNew[_]])

    def isSRAM: Boolean = x.isInstanceOf[SRAM[_]]
    def isFIFO: Boolean = x.isInstanceOf[FIFO[_]]
    def isLIFO: Boolean = x.isInstanceOf[LIFO[_]]

    def isReader: Boolean = Reader.unapply(x).isDefined
    def isWriter: Boolean = Writer.unapply(x).isDefined

    def nestedDataInputs: Set[Sym[_]] = {
      x.dataInputs.toSet ++ x.op.map{o =>
        val outs = o.blocks.flatMap(_.nestedStms)
        val used = outs.flatMap{s => s.nestedDataInputs }
        used diff outs
      }.getOrElse(Set.empty)
    }
  }

  implicit class BlockUtils(x: Block[_]) {
    def nestedStms: Set[Sym[_]] = x.stms.flatMap{s =>
      s.op.map{o => o.blocks.flatMap(_.nestedStms) }.getOrElse(Nil)
    }.toSet
  }


  /**
    * Returns all ancestors of the given node x (inclusive) to optional stop (inclusive)
    * Ancestors are ordered outermost to innermost
    */
  @stateful def allParents[T](x: T, stop: Option[T] = None)(parent: T => Option[T]): Seq[T] = {
    val parents = ListBuffer.empty[T]
    var current: Option[T] = Some(x)
    while (current.isDefined && current != stop) {
      parents.prepend(current.get)
      current = parent(current.get)
    }
    parents
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
    allParents(sym, stop){p => parentOf.get(p).map(_.sym) }
  }

  /**
    * Returns all ancestor controllers from this controller (inclusive) to optional stop (inclusive)
    * Ancestors are ordered outermost to innermost
    */
  @stateful def ctrlParents(ctrl: Ctrl): Seq[Ctrl] = allParents(ctrl,None){ctrlParent}
  @stateful def ctrlParents(ctrl: Ctrl, stop: Option[Ctrl]): Seq[Ctrl] = allParents(ctrl,stop){ctrlParent}
  @stateful def ctrlParents(sym: Sym[_], stop: Option[Ctrl] = None): Seq[Ctrl] = {
    parentOf.get(sym).map(p => ctrlParents(p,stop)).getOrElse(Nil)
  }


}
