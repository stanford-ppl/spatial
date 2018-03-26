package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait ScalaGenDebugging extends ScalaCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case PrintIf(en,msg)             => emit(src"val $lhs = if ($en) System.out.print($msg)")
    case AssertIf(en,cond,Some(msg)) => emit(src"val $lhs = if ($en) assert($cond, $msg)")
    case AssertIf(en,cond,None)      => emit(src"val $lhs = if ($en) assert($cond)")
    case BreakpointIf(en)        =>
      val bp = '"' + "Breakpoint" + '"'
      emit(src"val $lhs = if ($en) { System.out.println($bp); Console.readLine() }")

    case ExitIf(en)        =>
      val exit = '"' + "Exit" + '"'      
      emit(src"val $lhs = if ($en) { System.out.println($exit); return }")      

    case _ => super.gen(lhs, rhs)
  }

}
