package spatial.codegen.scalagen

import argon._
import argon.node._

trait ScalaGenDebugging extends ScalaCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case PrintIf(ens,msg)             => emit(src"val $lhs = if (${and(ens)}) System.out.print($msg)")
    case AssertIf(ens,cond,Some(msg)) => emit(src"val $lhs = if (${and(ens)}) { if (!$cond.toValidBoolean) { System.out.println($msg) }; assert($cond.toValidBoolean, $msg) }")
    case AssertIf(ens,cond,None)      => emit(src"val $lhs = if (${and(ens)}) assert($cond.toValidBoolean)")

    case BreakpointIf(ens) =>
      emit(src"""val $lhs = if (${and(ens)}) { System.out.println("${lhs.ctx}: Breakpoint"); Console.readLine() }""")

    case ExitIf(ens) => emit(src"""val $lhs = if (${and(ens)}) { System.out.println("${lhs.ctx}: Exit"); sys.exit() }""")

    case _ => super.gen(lhs, rhs)
  }

}
