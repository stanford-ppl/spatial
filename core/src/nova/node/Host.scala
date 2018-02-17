package nova.node

import forge.tags._
import nova.core._
import nova.data._
import nova.lang._

@op case class PrintIf(en: Seq[Bit], x: Text) extends Primitive[Void] {
  override def effects = Effects.Simple
  override val debugOnly: Boolean = true
}

@op case class AssertIf(en: Seq[Bit], cond: Bit, x: Option[Text]) extends Primitive[Void] {
  override def effects = Effects.Global
  override val debugOnly: Boolean = true
}