package spatial.util

import forge.tags._
import argon._
import spatial.data._
import spatial.node._

trait UtilsHierarchy {
  def isControl(sym: Sym[_]): Boolean = sym.op.exists(isControl)
  def isControl(op: Op[_]): Boolean = op.isInstanceOf[Control[_]]

  def isPrimitive(sym: Sym[_]): Boolean = sym.op.exists(isPrimitive)
  def isPrimitive(op: Op[_]): Boolean = op.isInstanceOf[Primitive[_]]

  def isEphemeral(sym: Sym[_]): Boolean = sym.op.exists(isEphemeral)
  def isEphemeral(op: Op[_]): Boolean = op match {
    case p: Primitive[_] => p.isEphemeral
    case _ => false
  }

  def isAccel(ctrl: Ctrl): Boolean = ctrl.s.exists(isAccel)
  def isAccel(sym: Sym[_]): Boolean = sym.op.exists(isAccel)
  def isAccel(op: Op[_]): Boolean = op.isInstanceOf[AccelScope]

  def isLoop(ctrl: Ctrl): Boolean = ctrl.s.exists(isLoop)
  def isLoop(sym: Sym[_]): Boolean = sym.op.exists{ _.isInstanceOf[Loop[_]] }

  def isSwitch(ctrl: Ctrl): Boolean = ctrl.s.exists(isSwitch)
  def isSwitch(sym: Sym[_]): Boolean = sym.op.exists(isSwitch)
  def isSwitch(op: Op[_]): Boolean = op.isInstanceOf[Switch[_]]

  def isBranch(ctrl: Ctrl): Boolean = ctrl.s.exists(isBranch)
  def isBranch(sym: Sym[_]): Boolean = sym.op.exists(isBranch)
  def isBranch(op: Op[_]): Boolean = op match {
    case _:Switch[_] | _:SwitchCase[_] | _:IfThenElse[_] => true
    case _ => false
  }

  def isParallel(ctrl: Ctrl): Boolean = ctrl.s.exists(isParallel)
  def isParallel(sym: Sym[_]): Boolean = sym.op.exists(isParallel)
  def isParallel(op: Op[_]): Boolean = op.isInstanceOf[ParallelPipe]


  def isInnerControl(ctrl: Ctrl): Boolean = ctrl.s.exists(isInnerControl) || !isOuterBlock(ctrl)
  def isInnerControl(sym: Sym[_]): Boolean = isControl(sym) && !isOuter(sym)
  def isOuterControl(sym: Sym[_]): Boolean = isControl(sym) && isOuter(sym)
  def isOuterControl(ctrl: Ctrl): Boolean  = isOuterBlock(ctrl)

  def isOuterBlock(blk: Ctrl): Boolean = blk match {
    case Controller(sym,id) => sym match {
      case Op(ctrl: Control[_]) => isOuterControl(sym) && (ctrl.mayBeOuterBlock(blk.id) || blk.id == -1)
      case _ => isOuterControl(sym)
    }
    case Host => true
  }
}
