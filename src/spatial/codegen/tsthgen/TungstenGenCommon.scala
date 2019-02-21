package spatial.codegen.tsthgen

import argon._
import spatial.lang._
import spatial.util.spatialConfig
import spatial.codegen.cppgen._

trait TungstenHostGenCommon extends TungstenHostCodegen with CppGenCommon {
  override protected def remap(tp: Type[_]): String = tp match {
    case FixPtType(s,d,f) if f != 0 && d+f <= 32 => "float"
    case FixPtType(s,d,f) if f != 0 && d+f <= 64 => "double"
    case tp => super.remap(tp)
  }
}
