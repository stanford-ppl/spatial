package spatial.lang

import forge.tags._
import argon._
import emul.FixedPointRange
import spatial.node._

/** Types */
@ref class Counter[A:Num] extends Top[Counter[A]] with Ref[FixedPointRange,Counter[A]] {
  override protected val __isPrimitive: Boolean = false
}
object Counter {
  @api def apply[A:Num](
    start: A,
    end:   A,
    step:  A = null,
    par:   I32 = I32(1)
  ): Counter[A] = {
    val stride: A = Option(step).getOrElse(Num[A].one)
    stage(CounterNew[A](start, end, stride, par))
  }

  @rig def from[A:Num](series: Series[A]): Counter[A] = {
    val Series(start,end,step,par,_) = series
    Counter(start,end,step,par)
  }
}
