package spatial.lang
package static

import argon._
import forge.tags._
import spatial.data.rangeOf

trait Parameters {
  @api def param[A](c: Lift[A]): A = c.B.from(c,checked = true, isParam = true)

  @rig def createParam(default: Int, start: Int, stride: Int, end: Int): I32 = {
    val p = I32.p(default)
    rangeOf(p) = (start, stride, end)
    p
  }
}
