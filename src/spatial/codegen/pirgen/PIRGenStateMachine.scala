package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait PIRGenStateMachine extends PIRCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case StateMachine(ens,start,notDone,action,nextState) =>
      val en = if (ens.isEmpty) "true" else ens.map(quote).mkString(" && ")
      val stat = notDone.input

      open(src"""val $lhs = if ($en) {""")
        emit(src"var $stat: ${stat.tp} = $start")
        open(src"def notDone() = {")
          ret(notDone)
        close("}")

        open(src"while( notDone() ){")
          gen(action)
          gen(nextState)
          emit(src"$stat = ${nextState.result}")
        close("}")

      close("}")

    case _ => super.gen(lhs,rhs)
  }
}
