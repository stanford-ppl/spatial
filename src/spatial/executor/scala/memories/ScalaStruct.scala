package spatial.executor.scala.memories

import spatial.executor.scala.{EmulResult, EmulVal}


import scala.reflect.ClassTag

case class ScalaStruct(fieldValues: Map[String, EmulVal[_]]) extends EmulVal[ScalaStruct] {

  override def value: ScalaStruct = this
}

case class ScalaStructType(fieldNames: Set[String])
