package spatial.codegen.scalagen

import argon._
import argon.codegen.{Codegen, FileDependencies}

trait ScalaCodegen extends Codegen with FileDependencies with ScalaFileGen {
  override val lang: String = "scala"
  override val ext: String = "scala"

  override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
    visitBlock(b)
    if (withReturn) emit(src"${b.result}")
  }


}
