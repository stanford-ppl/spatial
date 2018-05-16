package spatial.codegen.cppgen

import argon._

import spatial.data._

trait CppFileGen extends CppCodegen {

  backend = "cpp"

  override  def emitHeader(): Unit = {

    inGen(out, "cpptypes.hpp") {
      emit("""#ifndef __CPPTYPES_H__
#define __CPPTYPES_H__
#endif""")
    }

    inGen(out, "functions.hpp") {
      emit("""#ifndef __CPPFUN_H__
#define __CPPFUN_H__
""")
    }

    inGen(out, "functions.cpp") {
      emit("""#include "functions.hpp" """)
    }

    inGen(out, "structs.hpp") {
      emit("#include <vector>")
      emit("""#include <stdint.h>""")
      emit("""#include <sys/time.h>""")
      emit("""#include <iostream>""")
      emit("""#include <fstream>""")
      emit("""#include <string> """)
      emit("""#include <sstream> """)
      emit("""#include <stdarg.h>""")
      emit("""#include <signal.h>""")
      emit("""#include <sys/wait.h>""")
      emit("""#include <pwd.h>""")
      emit("""#include <unistd.h>""")
      emit("""#include <stdlib.h>""")
      emit("""#include <stdio.h>""")
      emit("""#include <errno.h>""")
      emit("using std::vector;")
      emit("#ifndef STRUCTS_HPP")
      emit("#define STRUCTS_HPP")
      emit("#ifndef ZYNQ")
      emit("typedef __int128 int128_t;")
      emit("#endif")

    }

    inGen(out, "ArgAPI.hpp") {
      emit(s"// API for args in app ${config.name}")
    }

    inGen(out, entryFile) {
      emit("""#include "structs.hpp"""")
      emit("""#include <stdint.h>""")
      emit("""#include <sys/time.h>""")
      emit("""#include <iostream>""")
      emit("""#include <fstream>""")
      emit("""#include <string> """)
      emit("""#include <sstream> """)
      emit("""#include <stdarg.h>""")
      emit("""#include <signal.h>""")
      emit("""#include <sys/wait.h>""")
      emit("""#include <pwd.h>""")
      emit("""#include <unistd.h>""")
      emit("""#include <stdlib.h>""")
      emit("""#include <stdio.h>""")
      emit("""#include <errno.h>""")
      emit("""#include "DeliteCpp.h"""")
      emit("""#include "cppDeliteArraystring.h"""")
      emit("""#include "cppDeliteArrays.h"""")
      emit("""#include "cppDeliteArraydouble.h"""")
      emit("""#include "FringeContext.h"""")
      emit("""#include "functions.hpp"""")
      emit("""#include "ArgAPI.hpp"""")
      emit("""#include <vector>""")
      emit("""using std::vector;""")
      emit("""""")
      emit("""#ifndef ZYNQ""")
      emit("""typedef __int128 int128_t;""")
      emit("""#endif""")
      emit("")
      open(s"void Application(int numThreads, vector<string> * args) {")
      emit("// Create an execution context.")
      emit("""FringeContext *c1 = new FringeContext("./verilog/accel.bit.bin");""")
      emit("""c1->load();""")
    }
  }
  
  override protected def emitEntry(block: Block[_]): Unit = {
    gen(block)
  }

  override def emitFooter(): Unit = {
    inGen(out, "functions.hpp") {
      emit("""#endif""")
    }

    inGen(out, "structs.hpp") {
      emit("#endif // STRUCTS_HPP ///:~ ")
    }
    
    inGen(out, entryFile) {
      emit("delete c1;")
      close("}")
      emit("")
      open("void printHelp() {")
        val argsList = CLIArgs.listNames.mkString(" ")
        emit(s"""fprintf(stderr, "Help for app: ${config.name}\\n");""")
  	    emit(s"""fprintf(stderr, "  -- bash run.sh ${argsList}\\n\\n");""")
  	    emit(s"""exit(0);""")
      close("}")

      emit("")
      open(src"""int main(int argc, char *argv[]) {""")
        emit(src"""vector<string> *args = new vector<string>(argc-1);""")
        open(src"""for (int i=1; i<argc; i++) {""")
          emit(src"""(*args)[i-1] = std::string(argv[i]);""")
          emit(src"""if (std::string(argv[i]) == "--help" | std::string(argv[i]) == "-h") {printHelp();}""")
        close(src"""}""")
        emit(src"""int numThreads = 1;""")
        emit(src"""char *env_threads = getenv("DELITE_NUM_THREADS");""")
        emit(src"""if (env_threads != NULL) { numThreads = atoi(env_threads); } else {""")
        emit(src"""  fprintf(stderr, "[WARNING]: DELITE_NUM_THREADS undefined, defaulting to 1\n");""")
        emit(src"""}""")
        emit(src"""fprintf(stderr, "Executing with %d thread(s)\n", numThreads);""")
        emit(src"""Application(numThreads, args);""")
        emit(src"""return 0;""")
      close(src"""}""")
    }
    super.emitFooter()
  }

}
