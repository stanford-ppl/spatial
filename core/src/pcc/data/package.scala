package pcc

import forge._
import pcc.core._
import pcc.lang._
import pcc.node._

import scala.language.existentials

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

  implicit class SymUtils(x: Sym[_]) {
    def isNum:  Boolean = x.isInstanceOf[Num[_]]
    def isBits: Boolean = x.isInstanceOf[Bits[_]]
    def isVoid: Boolean = x.isInstanceOf[Void]
    def isLocalMem: Boolean = x.isInstanceOf[LocalMem[_,C forSome {type C[_]}]] // ... yep
    def isRemoteMem: Boolean = x.isInstanceOf[RemoteMem[_,C forSome {type C[_]}]]

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

}
