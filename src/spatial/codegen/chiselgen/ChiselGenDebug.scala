package spatial.codegen.chiselgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.internal.{spatialConfig => cfg}


trait ChiselGenDebug extends ChiselGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
	case FixToText(_)  => 
    case TextConcat(_) => 
    case PrintIf(_,_) => 
    case BitToText(_) => 
	case _ => super.gen(lhs, rhs)
  }
}