package spatial.codegen.roguegen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.util.spatialConfig

trait RogueGenInterface extends RogueGenCommon {

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
    case DRAMHostNew(dims, _) => throw new Exception(s"DRAM nodes not currently supported in Rogue!")
    case FrameHostNew(dim, _, stream) =>
      frames += lhs
      // TODO: Need to check stream.isInstanceOf[StreamOut[_]] to make directioned container??
      inGen(out, "ConnectStreams.py") {
        if (stream.isInstanceOf[StreamIn[_]]) {
          emit(src"base.${lhs}_frame = FrameMaster()")
          emit(src"pyrogue.streamConnect(base.${lhs}_frame, base.${stream}_port)")
        } else {
          emit(src"base.${lhs}_frame = FrameSlave()")
          emit(src"pyrogue.streamConnect(base.${stream}_port, base.${lhs}_frame)")
        }

      }
    case SetReg(reg, v) =>
      emit(src"accel.${argHandle(reg)}_arg.set($v)")
      emit(src"""print("Wrote %d to $reg!" % $v)""")
      emit(src"time.sleep(0.001)")
    case _: CounterNew[_] =>
    case _: CounterChainNew =>
    case GetReg(reg)    =>
      emit(src"$lhs = accel.${argHandle(reg)}_arg.get()")
      emit(src"time.sleep(0.0001)")
    case StreamInNew(bus) =>
      inGen(out, "ConnectStreams.py") {
        bus match {
          case AxiStream64Bus(tid, tdest) => emit(src"base.${lhs}_port = rogue.interfaces.stream.TcpClient('localhost', 8000 + ($tdest+1)*2 + $tid * 512)")
          case AxiStream256Bus(tid, tdest) => emit(src"base.${lhs}_port = rogue.interfaces.stream.TcpClient('localhost', 8000 + ($tdest+1)*2 + $tid * 512)")
          case AxiStream512Bus(tid, tdest) => emit(src"base.${lhs}_port = rogue.interfaces.stream.TcpClient('localhost', 8000 + ($tdest+1)*2 + $tid * 512)")
          case _ =>
        }
      }
    case StreamOutNew(bus) =>
      inGen(out, "ConnectStreams.py") {
        bus match {
          case AxiStream64Bus(tid, tdest) => emit(src"base.${lhs}_port = rogue.interfaces.stream.TcpClient('localhost', 8000 + ($tdest+1)*2 + $tid * 512)")
          case AxiStream256Bus(tid, tdest) => emit(src"base.${lhs}_port = rogue.interfaces.stream.TcpClient('localhost', 8000 + ($tdest+1)*2 + $tid * 512)")
          case AxiStream512Bus(tid, tdest) => emit(src"base.${lhs}_port = rogue.interfaces.stream.TcpClient('localhost', 8000 + ($tdest+1)*2 + $tid * 512)")
          case _ =>
        }
      }
    case SetMem(dram, data) =>
    case GetMem(dram, data) =>
    case SetFrame(frame, data) =>
      emit(src"base.${frame}_frame.sendFrame($data.astype(dtype='uint64'))")

    case GetFrame(frame, data) =>
      emit(src"""$lhs = base.${frame}_frame.getFrame()""")
      emit(src"""$data = np.frombuffer($lhs, dtype='uint8').astype(dtype='${data.tp.typeArgs.head}')""")

    case _ => super.gen(lhs, rhs)
  }



  override def emitFooter(): Unit = {
    inGen(out,"_AccelUnit.py") {
      emit("#!/usr/bin/env python")
      emit("import pyrogue as pr")
      emit("import rogue.protocols")
      emit("import numpy as np")
      emit("")

      emit("class AccelUnit(pr.Device,rogue.interfaces.stream.Slave):")
      emit("    def __init__(   self,")
      emit("            name        = 'AccelUnit',")
      emit("            description = 'Spatial Top Module SW',")
      emit("            **kwargs):")
      emit("        rogue.interfaces.stream.Slave.__init__(self)")
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
      frames.zipWithIndex.foreach {case (d, id) =>
        emit(src"#        self.add(pr.RemoteVariable(name = '${argHandle(d)}_ptr', description = 'dram ptr', offset = ${(argIns.length+id)*4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RW',))")
      }
      emit("\n##### ArgIOs")
      argIOs.zipWithIndex.foreach{case (a, id) =>
        emit(src"        self.add(pr.RemoteVariable(name = '${argHandle(a)}_arg', description = 'argIn', offset = ${(frames.length+argIns.length+id)*4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RW',))")
      }
      emit("\n##### ArgOuts")
      argOuts.zipWithIndex.foreach { case (a, id) =>
        emit(src"        self.add(pr.RemoteVariable(name = '${argHandle(a)}_arg', description = 'argIn', offset = ${(argIOs.length+frames.length+argIns.length+id)*4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RO',))")
      }
      emit("\n##### Instrumentation Counters")
      if (spatialConfig.enableInstrumentation) {
        instrumentCounters.foreach { case (s, _) =>
          val base = instrumentCounterIndex(s)
          emit(src"        self.add(pr.RemoteVariable(name = '${quote(s).toUpperCase}_cycles_arg', description = 'cycs', offset = ${((1 max (argIns.length + argIOs.length + argOuts.length)) + base) * 4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RO',))")
          emit(src"        self.add(pr.RemoteVariable(name = '${quote(s).toUpperCase}_iters_arg', description = 'numiters', offset = ${((1 max (argIns.length + argIOs.length + argOuts.length)) + base + 1) * 4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RO',))")
          if (hasBackPressure(s.toCtrl) || hasForwardPressure(s.toCtrl)) {
            emit(src"        self.add(pr.RemoteVariable(name = '${quote(s).toUpperCase}_stalled_arg', description = 'stalled', offset = ${((1 max (argIns.length + argIOs.length + argOuts.length)) + base + 2) * 4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RO',))")
            emit(src"        self.add(pr.RemoteVariable(name = '${quote(s).toUpperCase}_idle_arg', description = 'idle', offset = ${((1 max (argIns.length + argIOs.length + argOuts.length)) + base + 3) * 4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RO',))")
          }
        }
      }
      emit("\n##### Early Exits")
      earlyExits.foreach{x =>
        emit(src"        self.add(pr.RemoteVariable(name = '${quote(x).toUpperCase}_exit_arg', description = 'early exit', offset = ${(argOuts.toList.length + argIOs.toList.length + instrumentCounterArgs())*4 + 8}, bitSize = 32, bitOffset = 0, mode = 'RO',))")
      }
      emit("    # Unused code for testing how to receive a frame in SW")
      emit("    def _acceptFrame(self,frame):")
      emit("        p = bytearray(frame.getPayload())")
      emit("        frame.read(p,0)")
      emit("        print(len(p))")
      emit("        my_mask = np.arange(36)")
      emit("        if(len(p)>100):")
      emit("              my_mask = np.append(my_mask,np.arange(int(len(p)/2),int(len(p)/2)+36))")
      emit("              my_mask = np.append(my_mask,np.arange(len(p)-36,len(p)))")
      emit("")
      emit("        to_print = np.array(p)[-1:]")
      emit("        #print(np.array(p)[:96],to_print) #comment out for long term test")
      emit("        print(np.array(p)[my_mask])")
      emit("        print('--------------------------')")


    }
    super.emitFooter()
  }

}
