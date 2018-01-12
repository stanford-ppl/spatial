package pcc

import pcc.core._
import pcc.node._

package object data {
  def isControl(sym: Sym[_]): Boolean = sym.op.exists(isControl)
  def isControl(op: Op[_]): Boolean = op.isInstanceOf[Control]
}
