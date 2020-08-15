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
#include "DUT.h"
#include "cppgenutil.h"
#include "hostio.h"

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
    open("void genLink() {")
      emit(s"""Top* top = new Top;""")
      emit(s"""Module DUT({top}, "DUT");""")
      emit(s"""REPL repl(&DUT, std::cout);""")
      emit(s"""repl.Command("source script_link");""")
      emit(s"""delete top;""")
  	  emit(s"""exit(1);""")
    close("}")

    open("int main(int argc, char **argv) {");
      emit("vector<string> *args = new vector<string>(0);")
      emit("vector<string> *W_pre = new vector<string>(0);")
      emit("vector<string> *W_post = new vector<string>(0);")
      open("for (int i=1; i<argc; i++) {")
        emit(src"""if (std::string(argv[i]) == "--WPRE" || std::string(argv[i]) == "--WPOST")  {
          assert(i+1 != argc);
          if (std::string(argv[i]) == "--WPRE") {
            std::cout << "Add pre-execution command: " << argv[i+1] << std::endl;
            W_pre->push_back(argv[i+1]); 
          } else {
            std::cout << "Add post-execution command: " << argv[i+1] << std::endl;
            W_post->push_back(argv[i+1]); 
          }
          i++;
          continue;
            }
        """)
        emit(src"""if (std::string(argv[i]) == "--help" | std::string(argv[i]) == "-h") {printHelp();}""")
        emit(src"""if (std::string(argv[i]) == "--gen-link" | std::string(argv[i]) == "-l") {genLink();}""")
        emit("(*args).push_back(std::string(argv[i]));")
      close("}")
      gen(block)
      emit(s"""cout << "Complete Simulation" << endl;""")
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
