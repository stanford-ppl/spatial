package spatial.executor.scala.memories

import spatial.executor.scala.{EmulResult, EmulVal}

case class ScalaStruct(fieldValues: Map[String, EmulVal[_]], fieldOrder: Seq[String]) extends EmulVal[ScalaStruct] {

  override def value: ScalaStruct = this
}
