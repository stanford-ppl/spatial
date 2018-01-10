package pcc
package ir

import data._
import forge._

object Dbg {
  @api def printIf(en: Seq[Bit], x: Text): Void = stage(PrintIf(en,x))
  @api def assertIf(en: Seq[Bit], cond: Bit, x: Option[Text]): Void = stage(AssertIf(en,cond,x))
}

case class PrintIf(en: Seq[Bit], x: Text) extends Op[Void] {
  override def effects = Effects.Simple
  def mirror(f:Tx) = Dbg.printIf(f(en),f(x))
}

case class AssertIf(en: Seq[Bit], cond: Bit, x: Option[Text]) extends Op[Void] {
  override def effects = Effects.Global
  def mirror(f:Tx) = Dbg.assertIf(f(en),f(cond),f(x))
}