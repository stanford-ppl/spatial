package spatial.lang

import forge.tags._
import forge.implicits.collections._

import core._

case class Series(start: I32, end: I32, step: I32, par: I32, isUnit: Boolean) {
  def ::(start2: I32): Series = Series(start2, end, start, par, isUnit = false)

  def par(p: I32): Series = Series(start, end, step, p, isUnit = false)

  @api def apply(func: I32 => Void): Void = Foreach(this)(func)

  @api def meta: Range = (start,end,step,par) match {
    case (Lit(s),Lit(e),Lit(stride),_) => Range(s.toInt,e.toInt,stride.toInt)
    case _ =>
      val s = if (!start.isConst) "start" else ""
      val e = if (!end.isConst) "end" else ""
      val t = if (!step.isConst) "step" else ""
      val err = Seq(s,e,t).filter(_ != "")
      val xs  = err.mkString(" and ")
      val wrng = if (err.lengthMoreThan(1)) s"$xs are not constants" else s"$xs is not a constant"
      error(ctx, s"Cannot create metaprogrammed range: $wrng")
      error(ctx)
      Range(0, 0, 1)
  }
}

object Series {
  @api def alloc(start: I32 = I32.c(0), end: I32, step: I32 = I32.c(1), par: I32 = I32.c(1), isUnit: Boolean = false): Series = {
    Series(start,end,step,par,isUnit)
  }
}
