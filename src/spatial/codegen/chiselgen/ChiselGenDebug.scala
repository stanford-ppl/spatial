package spatial.codegen.chiselgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.internal.{spatialConfig => cfg}
import spatial.data._
import spatial.util._



trait ChiselGenDebug extends ChiselGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
	case FixToText(_)  => 
    case TextConcat(_) => 
    case PrintIf(_,_) => 
    case BitToText(_) => 

    case ExitIf(en) => 
    	val ens = if (en.isEmpty) "true.B" else en.mkString("&")
	    emitt(s"breakpoints(${earlyExits.length}) := ${ens} & ${swap(quote(lhs.parent.s.get), DatapathEn)}")
	    earlyExits = earlyExits :+ lhs

    case AssertIf(en,cond,_) => 
    	if (scope == "accel") {
	    	val ens = if (en.isEmpty) "true.B" else en.mkString("&")
	        emitt(s"breakpoints(${earlyExits.length}) := ${ens} & ${swap(quote(lhs.parent.s.get), DatapathEn)} & ${quote(cond)}")
	        earlyExits = earlyExits :+ lhs
	    }

    case BreakpointIf(en) => 
    	val ens = if (en.isEmpty) "true.B" else en.mkString("&")
        emitt(s"breakpoints(${earlyExits.length}) := ${ens} & ${swap(quote(lhs.parent.s.get), DatapathEn)}")
        earlyExits = earlyExits :+ lhs

	case _ => super.gen(lhs, rhs)
  }
}