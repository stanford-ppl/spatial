package nova.utils

import forge.tags._
import nova.core._
import nova.data._
import nova.node._

trait UtilsHierarchy {
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

  def isLoop(sym: Sym[_]): Boolean = sym.op.exists{case _:Loop => true; case _ => false}
  def isLoop(ctrl: Ctrl): Boolean = isLoop(ctrl.sym)

  def isSwitch(sym: Sym[_]): Boolean = sym.op.exists(isSwitch)
  def isSwitch(op: Op[_]): Boolean = false  // TODO

  @api def isInnerControl(sym: Sym[_]): Boolean = isControl(sym) && !isOuter(sym)
  @api def isInnerControl(ctrl: Ctrl): Boolean = isInnerControl(ctrl.sym)
  @api def isOuterControl(sym: Sym[_]): Boolean = isControl(sym) && isOuter(sym)
  @api def isOuterControl(ctrl: Ctrl): Boolean = isOuterControl(ctrl.sym)
}
