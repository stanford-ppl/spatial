package pcc

import pcc.ir._

package object data {
  def isControl(x: Sym[_]): Boolean = x.op.exists(isControl)
  def isControl(x: Op[_]): Boolean = x.isInstanceOf[Control]
}
