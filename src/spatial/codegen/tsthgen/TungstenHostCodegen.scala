package spatial.codegen.tsthgen

import argon._
import argon.codegen.FileDependencies
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal
import spatial.targets._
import spatial.codegen.cppgen._
import utils.io.files._

trait TungstenHostCodegen extends FileDependencies with CppCodegen {
  override val lang: String = "tungsten"
  override val ext: String = "cc"
  override def entryFile: String = s"main.$ext"
  override def out = buildPath(super.out, "src")

  override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
    visitBlock(b)
  }

  override protected def emitEntry(block: Block[_]): Unit = {
    emit("""
#include <iostream>
#include <fstream>
#include <istream>
#include "repl.h"
#include "Top.h"
#include "cppgenutil.h"

using namespace std;
""");

    open("int main(int argc, char **argv) {");
      gen(block)
    close(s"""}""")
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    emit(s"// TODO: Unmatched node $lhs = $rhs")
    rhs.blocks.foreach(ret)
  }

  override def copyDependencies(out: String): Unit = {

    super[FileDependencies].copyDependencies(out)
  }

}
