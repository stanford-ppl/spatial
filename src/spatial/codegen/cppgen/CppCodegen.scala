package spatial.codegen.cppgen

import argon._
import argon.codegen.{Codegen, FileDependencies}

trait CppCodegen extends Codegen with FileDependencies {
  override val lang: String = "cpp"
  override val ext: String = "cpp"
  override def entryFile: String = s"TopHost.$ext"

  override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
    visitBlock(b)
    if (withReturn) emit(src"${b.result}")
  }


}
