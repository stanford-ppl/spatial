package spatial.lang

import argon._
import emul.FixedPointRange
import forge.tags._
import spatial.node._

/** Types */
@ref class Counter[A:Num] extends Top[Counter[A]] with Ref[FixedPointRange,Counter[A]] {
  override protected val __neverMutable: Boolean = false

  @api def __makeCopy: Counter[A] = {
    if (this.op.isEmpty) {
      error(ctx, s"No op definition for $this")
      err_[Counter[A]](this.tp, "Invalid declaration in global namespace")
    } else {
      this.op.get match {
        case Op(CounterNew(start, end, stride, par)) =>
          stage(CounterNew[A](start.asInstanceOf[A], end.asInstanceOf[A], stride.asInstanceOf[A], par))
        case Op(ForeverNew()) =>
          // This will only happen if A = I32
          stage(ForeverNew()).asInstanceOf[Counter[A]]
      }
    }
  }
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
