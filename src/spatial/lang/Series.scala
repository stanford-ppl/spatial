package spatial.lang

import core._
import emul.FixedPoint
import forge.tags._
import utils.implicits.collections._

case class Series[A:Num](start: A, end: A, step: A, par: I32, isUnit: Boolean = false)(implicit ev: A <:< Num[A]) {
  def tp: Num[A] = Num[A]

  def ::(start2: A): Series[A] = Series[A](start2, end, start, par, isUnit=false)

  def par(p: I32): Series[A] = Series[A](start, end, step, p, isUnit=false)

  @api def length: A = (end - start + step - Num[A].from(1))/step

  @api def meta: Range = (start,end,step,par) match {
    case (Literal(s:Int),Literal(e:Int),Literal(stride:Int),_) => Range(s,e,stride)
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
  /*implicit class SeriesOps(series: Series[I32]) {
    @api def apply(func: I32 => Void): Void = Foreach(series)(func)
  }*/
}
