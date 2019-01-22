package spatial.codegen.chiselgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.retiming._

trait ChiselGenDebug extends ChiselGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
	case FixToText(_)  => emit(src"""val $lhs = "" """)
    case TextConcat(_) =>  emit(src"""val $lhs = "" """)
    case PrintIf(_,_) =>  emit(src"""val $lhs = "" """)
    case BitToText(_) =>  emit(src"""val $lhs = "" """)
    case GenericToText(_) => emit(src"""val $lhs = "" """)
    case VarNew(_) =>  emit(src"""val $lhs = "" """)
    case VarRead(_) =>  emit(src"""val $lhs = "" """)
    case VarAssign(_,_) =>  emit(src"""val $lhs = "" """)

    case ExitIf(en) => 
    	val ens = and(en)
	    emit(s"Ledger.tieBreakpoint(breakpoints,${earlyExits.length}, ${ens} & (datapathEn).D(${lhs.fullDelay}))")
	    earlyExits = earlyExits :+ lhs

    case AssertIf(en,cond,_) => 
    	if (inHw) {
	    	val ens = and(en)
	        emit(s"Ledger.tieBreakpoint(breakpoints,${earlyExits.length}, ${ens} & (datapathEn).D(${lhs.fullDelay}) & ~${quote(cond)})")
	        earlyExits = earlyExits :+ lhs
	    }

    case BreakpointIf(en) => 
        val ens = and(en)
        emit(s"Ledger.tieBreakpoint(breakpoints,${earlyExits.length}, ${ens} & (datapathEn).D(${lhs.fullDelay}))")
        earlyExits = earlyExits :+ lhs

	case _ => super.gen(lhs, rhs)
  }
}