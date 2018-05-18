package spatial.codegen

import argon._
import argon.codegen.Codegen
import spatial.lang._

import scala.collection.mutable

trait StructCodegen extends Codegen {
  val encounteredStructs = mutable.HashMap[Struct[_], String]()
  var structNumber: Int = 0

  override protected def remap(tp: Type[_]): String = tp match {
    case t: Struct[_] =>
      encounteredStructs.getOrElseUpdate(t, { structNumber += 1; structName(t, structNumber) })

    case _ => super.remap(tp)
  }

  protected def structName(tp: Struct[_], idx: Int): String
  protected def emitDataStructures(): Unit

  override protected def postprocess[R](block: Block[R]): Block[R] = {
    emitDataStructures()
    super.postprocess(block)
  }


}
