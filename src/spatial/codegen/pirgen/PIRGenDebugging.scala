package spatial.codegen.pirgen

import argon._
import argon.node._

trait PIRGenDebugging extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case PrintIf(ens,msg)             => 
      state(lhs) { 
        src"""PrintIf().en($ens).msg($msg).tp(${lhs.sym.tp})"""
      }
      //emit(src"val $lhs = if (${and(ens)}) System.out.print($msg)")
    case AssertIf(ens,cond,Some(msg)) => 
      state(lhs) { 
        src"""AssertIf().en($ens).cond($cond).msg($msg).tp(${lhs.sym.tp})"""
      }
      //emit(src"val $lhs = if (${and(ens)}) { if (!$cond.toValidBoolean) { System.out.println($msg) }; assert($cond.toValidBoolean, $msg) }")
    case AssertIf(ens,cond,None)  => 
      state(lhs) { 
        src"""AssertIf().en($ens).cond($cond).tp(${lhs.sym.tp})"""
      }
      //emit(src"val $lhs = if (${and(ens)}) assert($cond.toValidBoolean)")

    //case BreakpointIf(ens) =>
      //emit(src"""val $lhs = if (${and(ens)}) { System.out.println("${lhs.ctx}: Breakpoint"); Console.readLine() }""")

    case ExitIf(ens) => 
      state(lhs) { 
        src"""ExitIf().en($ens)"""
      }
      emit(src"""val $lhs = if (${and(ens)}) { System.out.println("${lhs.ctx}: Exit"); sys.exit() }""")

    case _ => super.genAccel(lhs, rhs)
  }

}
