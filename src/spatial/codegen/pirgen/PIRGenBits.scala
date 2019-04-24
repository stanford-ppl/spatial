package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._

trait PIRGenBits extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case Mux(sel, a, b) => genOp(lhs)
    case OneHotMux(selects,datas) =>
      val op = s"OneHotMux${selects.size}"
      genOp(lhs, op=Some(op))

    case e@DataAsBits(a) => alias(lhs)(a)
    case BitsAsData(v,a) => genOp(lhs, inputs=Some(List(v)))
    case _ => super.genAccel(lhs,rhs)
  }

}
