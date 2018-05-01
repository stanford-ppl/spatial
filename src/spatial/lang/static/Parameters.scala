package spatial.lang
package static

import argon._
import forge.tags._
import spatial.data._

trait Parameters {
  @api def param[A](c: Lift[A]): A = c.B.from(c,errorOnLoss = true, isParam = true)

  @rig def createParam(default: Int, start: Int, stride: Int, end: Int): I32 = {
    val p = I32.p(default)
    p.paramDomain = (start, stride, end)
    p
  }
}
