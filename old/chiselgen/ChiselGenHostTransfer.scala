package spatial.codegen.chiselgen

import argon.core._
import spatial.aliases._
import spatial.nodes._

trait ChiselGenHostTransfer extends ChiselCodegen  {

  // Does not belong in chisel
  // override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
  //   case _ => super.gen(lhs, rhs)
  // }

}
