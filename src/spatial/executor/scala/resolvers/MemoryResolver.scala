package spatial.executor.scala.resolvers

import argon._
import argon.lang.{Bits, Struct}
import emul.FixedPoint
import spatial.executor.scala._
import spatial.executor.scala.memories._
import spatial.lang._
import spatial.node._

import spatial.metadata.memory._

import scala.reflect.ClassTag

trait MemoryResolver extends OpResolverBase {

  override def run[U, V](sym: Exp[U, V], op: Op[V], execState: ExecutionState): EmulResult = {
    implicit val st: argon.State = execState.IR
    op match {
      case ao:RegAlloc[_, Reg] if !ao.A.isInstanceOf[Struct[_]]=>
        val initVal = execState(ao.init)
        new ScalaReg(initVal, initVal)

      case streamIn@StreamInNew(FileBus(filename)) =>
        new FileReadScalaQueue(filename, streamIn.A)

      case StreamOutNew(FileBus(filename)) =>
        new FileWriteScalaQueue(filename)

      case streamOut: StreamOutNew[_] if streamOut.A.isInstanceOf[Struct[_]] =>
        new ScalaQueue[ScalaStruct]()

      case streamIn@StreamInNew(bus) if streamIn.A.isInstanceOf[Struct[_]] =>
        new ScalaQueue[ScalaStruct]()

      case streamIn@StreamInNew(bus) =>
        new ScalaQueue[SomeEmul]()

      case malloc: MemAlloc[_, _] if sym.isSRAM || sym.isDRAM =>
        val elType: ExpType[_, _] = malloc.A.tp
        val elSize = malloc.A.nbits / 8
        val newTensor = new ScalaTensor[SomeEmul](malloc.dims.map(execState.getValue[FixedPoint](_)).map(_.toInt), Some(elSize))
        if (sym.isDRAM) {
          execState.hostMem.register(newTensor)
        }
        newTensor

      case rw@RegWrite(mem, data, ens) =>
        val tmp = execState(mem) match { case sr:ScalaReg[_] => sr }
        type ET = tmp.ET
        val sReg = tmp.asInstanceOf[ScalaReg[ET]]
        val eData = execState(data) match {
          case ev: ET => ev
        }
        val enabled = rw.isEnabled(execState)
        sReg.write(eData, enabled)
        EmulUnit(sym)

      case RegRead(mem) =>
        execState(mem) match {
          case sr: ScalaReg[_] => sr.curVal
        }

      case GetReg(mem) =>
        execState(mem) match {
          case sr: ScalaReg[_] => sr.curVal
        }

      case SetReg(mem, data) =>
        val tmp = execState(mem) match {
          case sr: ScalaReg[_] => sr
        }
        type ET = tmp.ET
        val reg = tmp.asInstanceOf[ScalaReg[ET]]

        val realData = execState(data).asInstanceOf[ET]
        reg.write(realData, true)
        EmulUnit(sym)

      case sm@SetMem(dram, data) =>
        val target = execState.getTensor[SomeEmul](dram)
        val wrData = execState.getTensor[SomeEmul](data)
        wrData.values.copyToArray(target.values)
        EmulUnit(sym)

      case gm@GetMem(dram, data) =>
        val wrData = execState.getTensor[SomeEmul](dram)
        val target = execState.getTensor[SomeEmul](data)
        dbgs(s"Transferring <${wrData.values.mkString(", ")}> -> <${target.values.mkString(", ")}>")
        wrData.values.copyToArray(target.values)
        EmulUnit(sym)

      case sir@StreamInRead(mem, ens) if !mem.A.isInstanceOf[Struct[_]] =>
        val enabled = sir.isEnabled(execState)
        val queue = execState(mem) match { case sq: ScalaQueue[_] => sq }
        if (enabled) {
          queue.deq()
        } else {
          SimpleEmulVal(mem.A.zero.asInstanceOf[Bits[_]].c.get, false)
        }

      case sow@StreamOutWrite(mem, data, ens) if data.isInstanceOf[Struct[_]] =>
        execState(mem) match {
          case sq: ScalaQueue[ScalaStruct] =>
            if (sow.isEnabled(execState)) {
              val dataVal = execState.getValue[ScalaStruct](data)
              sq.enq(dataVal)
            }
        }
        EmulUnit(sym)

      case DRAMAddress(dram) =>
        // Get the location of the dram
        val tensor = execState.getTensor(dram)
        SimpleEmulVal(FixedPoint.fromInt(execState.hostMem.getEntry(tensor).start))

      case DRAMIsAlloc(mem) =>
        SimpleEmulVal(emul.Bool(true))

      case srw@SRAMWrite(mem, data, addr, ens) if !srw.A.isInstanceOf[Struct[_]] =>
        if (srw.isEnabled(execState)) {
          val realData = execState.getValue[srw.A.L](data)
          val tensor = execState.getTensor[EmulVal[srw.A.L]](mem)
          val address = addr.map(execState.getValue[FixedPoint](_).toInt)
          tensor.write(SimpleEmulVal(realData), address, true)
        }
        EmulUnit(sym)

      case srw@SRAMRead(mem, addr, ens) if !srw.A.isInstanceOf[Struct[_]] =>
        if (srw.isEnabled(execState)) {
          val tensor = execState.getTensor[EmulVal[srw.A.L]](mem)
          val address = addr.map(execState.getValue[FixedPoint](_).toInt)
          val read = tensor.read(address, true)
          if (read.isEmpty) {
            throw SimulationException(s"Attempting to read $mem[${address.mkString(", ")}], which was uninitialized.")
          }
          read.get
        } else {
          SimpleEmulVal[srw.A.L](null, false)
        }

      case _ => super.run(sym, op, execState)
    }
  }
}
