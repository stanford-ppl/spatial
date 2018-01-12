package pcc.data

import forge._
import pcc.core._

sealed abstract class ControlLevel
case object Outer extends ControlLevel
case object Inner extends ControlLevel

case class CtrlLevel(level: ControlLevel) extends SimpleData[CtrlLevel]
@data object levelOf {
  def get(x: Sym[_]): Option[ControlLevel] = metadata[CtrlLevel](x).map(_.level)
  def apply(x: Sym[_]): ControlLevel = levelOf.get(x).getOrElse{throw new Exception(s"Undefined control level for $x") }
  def update(x: Sym[_], level: ControlLevel): Unit = metadata.add(x, CtrlLevel(level))
}
@data object isOuter {
  def apply(x: Sym[_]): Boolean = levelOf(x) == Outer
  def update(x: Sym[_], isOut: Boolean): Unit = if (isOut) levelOf(x) = Outer else levelOf(x) = Inner
}


