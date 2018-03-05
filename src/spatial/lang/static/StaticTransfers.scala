package spatial.lang
package static

import core._
import forge.tags._

import spatial.node._

trait StaticTransfers {

  /**
    * Transfer a scalar value from the host to the accelerator through the ArgIn `reg`.
    */
  @api def setArg[A](reg: ArgIn[A], value: A): Void = {
    implicit val bA: Bits[A] = reg.tA
    stage(SetArgIn(reg,value))
  }

  /**
    * Transfer a scalar value from the accelerator to the host through the ArgOut `reg`.
    */
  @api def getArg[A](reg: ArgOut[A]): A = {
    implicit val bA: Bits[A] = reg.tA
    stage(GetArgOut(reg))
  }

}
