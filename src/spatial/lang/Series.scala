package spatial.lang

import core._
import emul.FixedPoint
import forge.tags._
import forge.implicits.collections._

case class Series[A:Num](start: A, end: A, step: A, par: I32, isUnit: Boolean = false)(implicit ev: A <:< Num[A]) {
  def tp: Num[A] = Num[A]

  def ::(start2: A): Series[A] = Series[A](start2, end, start, par, isUnit=false)

  def par(p: I32): Series[A] = Series[A](start, end, step, p, isUnit=false)

  @api def meta: Range = (start,end,step,par) match {
    case (Const(s:FixedPoint),Const(e:FixedPoint),Const(stride:FixedPoint),_) =>
      Range(s.toInt,e.toInt,stride.toInt)
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
  implicit class SeriesOps(series: Series[I32]) {
    @api def apply(func: I32 => Void): Void = Foreach(series)(func)
  }
}
