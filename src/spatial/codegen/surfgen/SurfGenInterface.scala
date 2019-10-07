package spatial.codegen.surfgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.util.spatialConfig

trait SurfGenInterface extends SurfGenCommon {

  // override protected def remap(tp: Type[_]): String = tp match {
  //   case tp: RegType[_] => src"${tp.typeArguments.head}"
  //   case _ => super.remap(tp)
  // }

  def isHostIO(x: Sym[_]) = "false"

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ArgInNew(init)  =>
      argIns += lhs
      emit(src"$lhs = $init;")
    case HostIONew(init)  =>
      argIOs += lhs
      emit(src"$lhs = $init;")
    case ArgOutNew(init) =>
      argOuts += lhs
    case RegRead(reg)    =>
      emit(src"$lhs = $reg;")
    case RegWrite(reg,v,en) =>
      emit(src"# $lhs $reg $v $en reg write")
    case DRAMHostNew(dims, _) =>
      drams += lhs
      emit(src"""${lhs} = self._reqFrame(${dims.map(quote).mkString("*")}, True)""")
      emit(src"set this ${argHandle(lhs)}_ptr")
      emit(src"c1->setArg(${argHandle(lhs)}_ptr, $lhs, false);")
      emit(src"""print("Allocate mem of size ${dims.map(quote).mkString("*")} at %p\n" % ${lhs}_ptr);""")

    case SetReg(reg, v) =>
      emit(src"accel.${argHandle(reg)}_arg.set($v)")
      emit(src"""print("Wrote %d to $reg!" % $v)""")
      emit(src"time.sleep(0.001)")
    case _: CounterNew[_] =>
    case _: CounterChainNew =>
    case GetReg(reg)    =>
      emit(src"$lhs = accel.${argHandle(reg)}_arg.get()")
      emit(src"time.sleep(0.001)")
    case StreamInNew(stream) =>
    case StreamOutNew(stream) =>

    case SetMem(dram, data) =>

    case GetMem(dram, data) =>

    case _ => super.gen(lhs, rhs)
  }



  override def emitFooter(): Unit = {
    inGen(out,"_AccelTop.py") {
      emit("#!/usr/bin/env python")
      emit("import pyrogue as pr")
      emit("")
      emit("class AccelTop(pr.Device):")
      emit("    def __init__(   self,")
      emit("            name        = 'AccelTop',")
      emit("            description = 'Spatial Top Module SW',")
      emit("            **kwargs):")
      emit("        super().__init__(name=name, description=description, **kwargs)")
      emit("        self.add(pr.RemoteVariable(")
      emit("            name         = 'Enable',")
      emit("            description  = 'Enable signal for App',")
      emit("            offset       =  0x000,")
      emit("            bitSize      =  1,")
      emit("            bitOffset    =  0,")
      emit("            mode         = 'RW',")
      emit("        ))")
      emit("        self.add(pr.RemoteVariable(")
      emit("            name         = 'Reset',")
      emit("            description  = 'Reset signal for App',")
      emit("            offset       =  0x000,")
      emit("            bitSize      =  1,")
      emit("            bitOffset    =  1,")
      emit("            mode         = 'RW',")
      emit("        ))")
      emit("")
      emit("        self.add(pr.RemoteVariable(")
      emit("            name         = 'Done',")
      emit("            description  = 'App Done',")
      emit("            offset       =  0x004,")
      emit("            bitSize      =  32,")
      emit("            bitOffset    =  0,")
      emit("            mode         = 'RO',")
      emit("        ))")
      emit("")


      emit("\n##### ArgIns")
      argIns.zipWithIndex.foreach{case (a, id) =>
        emit(src"        self.add(pr.RemoteVariable(name = '${argHandle(a)}_arg', description = 'argIn', offset = ${id*4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RW',))")
      }
      emit("\n##### DRAM Ptrs:")
      drams.zipWithIndex.foreach {case (d, id) =>
        emit(src"        self.add(pr.RemoteVariable(name = '${argHandle(d)}_ptr', description = 'dram ptr', offset = ${(argIns.length+id)*4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RW',))")
      }
      emit("\n##### ArgIOs")
      argIOs.zipWithIndex.foreach{case (a, id) =>
        emit(src"        self.add(pr.RemoteVariable(name = '${argHandle(a)}_arg', description = 'argIn', offset = ${(drams.length+argIns.length+id)*4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RW',))")
      }
      emit("\n##### ArgOuts")
      argOuts.zipWithIndex.foreach { case (a, id) =>
        emit(src"        self.add(pr.RemoteVariable(name = '${argHandle(a)}_arg', description = 'argIn', offset = ${(argIOs.length+drams.length+argIns.length+id)*4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RO',))")
      }
      emit("\n##### Instrumentation Counters")
      if (spatialConfig.enableInstrumentation) {
        instrumentCounters.foreach { case (s, _) =>
          val base = instrumentCounterIndex(s)
          emit(src"        self.add(pr.RemoteVariable(name = '${quote(s).toUpperCase}_cycles_arg', description = 'cycs', offset = ${(argIns.length + drams.length + argIOs.length + argOuts.length + base) * 4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RO',))")
          emit(src"        self.add(pr.RemoteVariable(name = '${quote(s).toUpperCase}_iters_arg', description = 'numiters', offset = ${(argIns.length + drams.length + argIOs.length + argOuts.length + base + 1) * 4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RO',))")
          if (hasBackPressure(s.toCtrl) || hasForwardPressure(s.toCtrl)) {
            emit(src"        self.add(pr.RemoteVariable(name = '${quote(s).toUpperCase}_stalled_arg', description = 'stalled', offset = ${(argIns.length + drams.length + argIOs.length + argOuts.length + base + 2) * 4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RO',))")
            emit(src"        self.add(pr.RemoteVariable(name = '${quote(s).toUpperCase}_idle_arg', description = 'idle', offset = ${(argIns.length + drams.length + argIOs.length + argOuts.length + base + 3) * 4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RO',))")
          }
        }
      }
      emit("\n##### Early Exits")
      earlyExits.foreach{x =>
        emit(src"        self.add(pr.RemoteVariable(name = '${quote(x).toUpperCase}_exit_arg', description = 'early exit', offset = ${(argOuts.toList.length + argIOs.toList.length + instrumentCounterArgs())*4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RO',))")
      }

    }
    super.emitFooter()
  }

}
