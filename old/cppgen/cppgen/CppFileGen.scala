package argon.codegen.cppgen

import argon.core._
import argon.codegen.FileGen

trait CppFileGen extends FileGen {

  override protected def emitMain[S:Type](b: Block[S]): Unit = emitBlock(b)

  override protected def process[S:Type](b: Block[S]): Block[S] = {
    // Forcefully create the following streams
    withStream(getStream("TopHost")) {
      if (config.emitDevel > 0) { Console.println(s"[ ${lang}gen ] Begin!")}
      preprocess(b)
      emitMain(b)
      postprocess(b)
      if (config.emitDevel > 0) { Console.println(s"[ ${lang}gen ] Complete!")}
      b
    }
  }


  override protected def emitFileHeader() {
    emit(s"""#include <stdint.h>
#include <sys/time.h>
#include <iostream>
#include <fstream>
#include <string> 
#include <sstream> 
#include <stdarg.h>
#include <signal.h>
#include <sys/wait.h>
#include <pwd.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include "DeliteCpp.h"
#include "cppDeliteArraystring.h"
#include "cppDeliteArrays.h"
#include "cppDeliteArraydouble.h"
#include "FringeContext.h"
#include "functions.h"
#include "ArgAPI.h"
#include "half.hpp"
#include <vector>
using std::vector;
using namespace half_float;

#ifndef ZYNQ
typedef __int128 int128_t;
#endif

""")

  open(s"void Application(int numThreads, vector<std::string> * args) {")
  emit("// Create an execution context.")
  emit("""FringeContext *c1 = new FringeContext("./verilog/accel.bit.bin");""")
  emit("""c1->load();""")


    withStream(getStream("cpptypes","h")) {
      emit("""#ifndef __CPPTYPES_H__
#define __CPPTYPES_H__
#endif""")
    }

    withStream(getStream("functions","h")) {
      emit("""#ifndef __CPPFUN_H__
#define __CPPFUN_H__
""")
    }

    withStream(getStream("functions","cpp")) {
      emit("""#include "functions.h" """)
    }

    withStream(getStream("ArgAPI", "h")) {
      emit(s"// API for args in app ${config.name}")
    }

    withStream(getStream("argmap","json")) {
      open("""{""")
        open(""""args": [""")
    }

    super.emitFileHeader()
  }

  override protected def emitFileFooter() {
    emit("delete c1;")
    close("}")
    val argInts = cliArgs.toSeq.map(_._1)
    val argsList = if (argInts.length > 0) {
      (0 to argInts.max).map{i => 
        if (cliArgs.contains(i)) s"<$i- ${cliArgs(i)}>"
        else s"<${i}- UNUSED>"
      }.mkString(" ")
    } else {"<No input args>"}
    emit(s"""
void printHelp() {
  fprintf(stderr, "Help for app: ${config.name}\\n");
  fprintf(stderr, "  -- bash run.sh ${argsList}\\n\\n");
  exit(0);
}
""")
    emit(s"""
int main(int argc, char *argv[]) {
  vector<std::string> *args = new vector<std::string>(argc-1);
  for (int i=1; i<argc; i++) {
    (*args)[i-1] = std::string(argv[i]);
    if (std::string(argv[i]) == "--help" | std::string(argv[i]) == "-h") {printHelp();}
  }
  int numThreads = 1;
  char *env_threads = getenv("DELITE_NUM_THREADS");
  if (env_threads != NULL) {
    numThreads = atoi(env_threads);
  } else {
    fprintf(stderr, "[WARNING]: DELITE_NUM_THREADS undefined, defaulting to 1\\n");
  }
  fprintf(stderr, "Executing with %d thread(s)\\n", numThreads);
  Application(numThreads, args);
  return 0;
}
""")

    withStream(getStream("functions","h")) {
      emit("""#endif""")
    }

    withStream(getStream("argmap","json")) {
        close("""]""")
      close("}")
    }

    super.emitFileFooter()
  }

}
