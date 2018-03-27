package spatial.lang
package static

import argon._
import forge.tags._

import spatial.node._

trait StaticTransfers {

  /**
    * Transfer a scalar value from the host to the accelerator through the ArgIn `reg`.
    */
  @api def setArg[A](reg: ArgIn[A], value: Lift[A]): Void = {
    implicit val bA: Bits[A] = reg.A
    stage(SetArgIn(reg,value.unbox))
  }
  @api def setArg[A](reg: ArgIn[A], value: Bits[A]): Void = {
    implicit val bA: Bits[A] = reg.A
    stage(SetArgIn(reg,value.unbox))
  }

  /**
    * Transfer a scalar value from the accelerator to the host through the ArgOut `reg`.
    */
  @api def getArg[A](reg: ArgOut[A]): A = {
    implicit val bA: Bits[A] = reg.A
    stage(GetArgOut(reg))
  }

}
