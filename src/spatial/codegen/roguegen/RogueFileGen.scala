package spatial.codegen.roguegen

import argon._

import spatial.metadata.CLIArgs

trait RogueFileGen extends RogueCodegen {

  backend = "python"

  override def emitHeader(): Unit = {
    inGen(out, entryFile) {
      emit("#!/usr/bin/env python3")
      emit("import setupLibPaths")
      emit("")
      emit("import sys")
      emit("import argparse")
      emit("import rogue")
      emit("import rogue.hardware.axi")
      emit("import rogue.interfaces.stream")
      emit("import rogue.interfaces.memory")
      emit("")
      emit("import pyrogue as pr")
      emit("import pyrogue.gui")
      emit("import pyrogue.utilities.prbs")
      emit("import pyrogue.interfaces.simulation")
      emit("")
      emit("import axipcie  as pcie")
      emit("import rogue.axi as axi")
      emit("")
      emit("import time")
      emit("import math")
      emit("import random")
      emit("import struct")
      emit("import numpy as np")
      emit("")
      open("def execute(base, cliargs):")
        emit("accel = base.Fpga.SpatialBox")
        emit("accel.Reset.set(1)")
        emit("accel.Enable.set(0)")
        emit("time.sleep(0.01)")
        emit("accel.Reset.set(0)")
        emit("print(\"Starting TopHost.py...\")")
    }
  }

  override protected def emitEntry(block: Block[_]): Unit = {
    gen(block)
  }

  override def emitFooter(): Unit = {
    inGen(out, entryFile) {
      close("")
    }
    super.emitFooter()
  }

}
