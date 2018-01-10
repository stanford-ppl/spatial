package pcc
package ir

import forge._
import pcc.ir.control.Foreach

case class Series(start: I32, end: I32, step: Option[I32], par: Option[I32], isUnit: Boolean) {
  def ::(start2: I32): Series = Series(start2, end, Some(start), par, isUnit = false)

  def par(p: I32): Series = Series(start, end, step, Some(p), isUnit = false)

  @api def apply(func: I32 => Void): Void = Foreach(this)(func)
}

object Series {
  @api def alloc(start: Option[I32], end: I32, step: Option[I32], par: Option[I32], isUnit: Boolean = false): Series = {
    Series(start.getOrElse(I32.c(0)),end,step,par,isUnit)
  }
}
