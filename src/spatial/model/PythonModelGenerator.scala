package spatial.model

import argon._
import argon.codegen.Codegen
import spatial.lang._

case class PythonApplicationModel(
  runtimes: Map[Sym[_], Block[I32]]
)

case class PythonModelGenerator(IR: State) extends Codegen {

  def run()

  override def gen(lhs: Sym[_], rhs: Sym[_]): Unit = rhs match {


    case _ => super.gen(lhs, rhs)
  }

}
