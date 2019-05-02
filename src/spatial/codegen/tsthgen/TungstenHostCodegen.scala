package spatial.codegen.tsthgen

import argon._
import argon.codegen.FileDependencies
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal
import spatial.targets._
import spatial.codegen.cppgen._
import spatial.metadata.CLIArgs
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

    open("void printHelp() {")
      val argsList = CLIArgs.listNames
      val examples: Iterator[Seq[String]] = if (argsList.nonEmpty) IR.runtimeArgs.grouped(argsList.size) else Iterator(Seq(""))
      emit(s"""fprintf(stderr, "Help for app: ${config.name}\\n");""")
      emit(s"""fprintf(stderr, "  -- Args:    ${argsList.mkString(" ")}\\n");""")
      while(examples.hasNext) {
        emit(s"""fprintf(stderr, "    -- Example: ./tungsten ${examples.next.mkString(" ")}\\n");""")  
      }
  	  emit(s"""exit(1);""")
    close("}")

    open("int main(int argc, char **argv) {");
      emit("vector<string> *args = new vector<string>(argc-1);")
      open("for (int i=1; i<argc; i++) {")
        emit("(*args)[i-1] = std::string(argv[i]);")
        emit(src"""if (std::string(argv[i]) == "--help" | std::string(argv[i]) == "-h") {printHelp();}""")
      close("}")
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
