package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait PIRGenDebugging extends PIRCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case PrintIf(ens,msg)             => emit(src"val $lhs = if (${and(ens)}) System.out.print($msg)")
    case AssertIf(ens,cond,Some(msg)) => emit(src"val $lhs = if (${and(ens)}) assert($cond, $msg)")
    case AssertIf(ens,cond,None)      => emit(src"val $lhs = if (${and(ens)}) assert($cond)")

    case BreakpointIf(ens) =>
      emit(src"""val $lhs = if (${and(ens)}) { System.out.println("${lhs.ctx}: Breakpoint"); Console.readLine() }""")

    case ExitIf(ens) => emit(src"""val $lhs = if (${and(ens)}) { System.out.println("${lhs.ctx}: Exit"); sys.exit() }""")

    case _ => super.gen(lhs, rhs)
  }

}
