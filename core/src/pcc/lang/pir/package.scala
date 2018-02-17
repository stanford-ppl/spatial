package pcc.lang

import forge._
import pcc.core._
import pcc.data._
import pcc.node.pir._

package object pir {
  @api def pcu: PCUPtr = curPtr.get match {
    case Some(ptr: PCUPtr) => ptr
    case _ =>
      error(ctx, "No PCU is being defined in this scope.")
      error(ctx)
      throw CompilerErrors("Staging",state.errors)
  }
  @api def pmu: PMUPtr = curPtr.get match {
    case Some(ptr: PMUPtr) => ptr
    case _ =>
      error(ctx, "No PMU is being defined in this scope.")
      error(ctx)
      throw CompilerErrors("Staging",state.errors)
  }

  @api implicit def readInput[A:Bits](x: In[A]): A = stage(ReadIn(x))

  implicit class OutBitOps(out: Out[Bit]) {
    @api def ~~>(in: In[Bit]): Void = { stage(ControlBus(out,in)); void }
  }

}
