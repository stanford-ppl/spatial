package spatial.codegen.pirgen

import argon._
import argon.node._

trait PIRGenSeries extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    //case SeriesForeach(start,end,step,func) =>
      //open(src"val $lhs = for (${func.input} <- $start until $end by $step) {")
        //gen(func)
      //close("}")

    case _ => super.genAccel(lhs,rhs)
  }

}
