package spatial.codegen.scalagen

import argon.core._
import argon.nodes._
import spatial.aliases._
import spatial.nodes._

trait ScalaGenMath extends ScalaGenBits { this: ScalaGenFltPt with ScalaGenFixPt =>



  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {




    case _ => super.gen(lhs, rhs)
  }

}