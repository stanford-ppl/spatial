package spatial.codegen.surfgen

import argon._

import spatial.metadata.CLIArgs

trait SurfFileGen extends SurfCodegen {

  backend = "python"

  override def emitHeader(): Unit = {
    inGen(out, entryFile) {
      emit("import numpy as np")
      open("def execute():")
    }
  }

  override protected def emitEntry(block: Block[_]): Unit = {
    gen(block)
  }

  override def emitFooter(): Unit = {
    super.emitFooter()
  }

}
