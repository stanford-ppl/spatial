package spatial.executor.scala.resolvers

import argon._
import argon.lang.{Bits, Struct}
import emul.FixedPoint
import spatial.executor.scala._
import spatial.executor.scala.memories.{ScalaQueue, ScalaReg, ScalaStruct, ScalaStructType, ScalaTensor}
import spatial.lang.Reg
import spatial.node._

import spatial.metadata.memory._

import scala.reflect.ClassTag

trait MemoryResolver extends OpResolverBase {

  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = {
    implicit val st: argon.State = execState.IR
    sym match {
      case Op(ao:RegAlloc[_, Reg]) if !ao.A.isInstanceOf[Struct[_]]=>
        val initVal = execState(ao.init)
        new ScalaReg(initVal, initVal)

      case Op(rw@RegWrite(mem, data, ens)) =>
        val tmp = execState(mem) match { case sr:ScalaReg[_] => sr }
        type ET = tmp.ET
        val sReg = tmp.asInstanceOf[ScalaReg[ET]]
        val eData = execState(data) match {
          case ev: ET => ev
        }
        val enabled = rw.isEnabled(execState)
        sReg.write(eData, enabled)
        EmulUnit(sym)

      case Op(RegRead(mem)) =>
        execState(mem) match {
          case sr: ScalaReg[_] => sr.curVal
        }

      case Op(GetReg(mem)) =>
        execState(mem) match {
          case sr: ScalaReg[_] => sr.curVal
        }

      case Op(SetReg(mem, data)) =>
        val tmp = execState(mem) match {
          case sr: ScalaReg[_] => sr
        }
        type ET = tmp.ET
        val reg = tmp.asInstanceOf[ScalaReg[ET]]

        val realData = execState(data).asInstanceOf[ET]
        reg.write(realData, true)
        EmulUnit(sym)

      case Op(malloc: MemAlloc[_, _]) if sym.isSRAM || sym.isDRAM && !malloc.A.isInstanceOf[Struct[_]] =>
        val elType: ExpType[_, _] = malloc.A.tp
        type ET = elType.L
        val elSize = malloc.A.nbits / 8
        val newTensor = new ScalaTensor[SomeEmul](malloc.dims.map(execState.getValue[FixedPoint](_)).map(_.toInt), Some(elSize))
        if (sym.isDRAM) {
          execState.hostMem.register(newTensor)
        }
        newTensor

      case Op(sm@SetMem(dram, data)) =>
        val target = execState.getTensor[SomeEmul](dram)
        val wrData = execState.getTensor[SomeEmul](data)
        wrData.values.copyToArray(target.values)
        EmulUnit(sym)

      case Op(gm@GetMem(dram, data)) =>
        val wrData = execState.getTensor[SomeEmul](dram)
        val target = execState.getTensor[SomeEmul](data)
        dbgs(s"Transferring <${wrData.values.mkString(", ")}> -> <${target.values.mkString(", ")}>")
        wrData.values.copyToArray(target.values)
        EmulUnit(sym)

      case Op(streamOut: StreamOutNew[_]) if streamOut.A.isInstanceOf[Struct[_]] =>
        new ScalaQueue[ScalaStruct]()

      case Op(streamIn@StreamInNew(bus)) if streamIn.A.isInstanceOf[Struct[_]] =>
        new ScalaQueue[ScalaStruct]()

      case Op(streamIn@StreamInNew(bus)) =>
        new ScalaQueue[SomeEmul]()

      case Op(sir@StreamInRead(mem, ens)) if !mem.A.isInstanceOf[Struct[_]] =>
        val enabled = sir.isEnabled(execState)
        val queue = execState(mem) match { case sq: ScalaQueue[_] => sq }
        if (enabled) {
          queue.deq()
        } else {
          SimpleEmulVal(mem.A.zero.asInstanceOf[Bits[_]].c.get, false)
        }

      case Op(sow@StreamOutWrite(mem, data, ens)) if data.isInstanceOf[Struct[_]] =>
        execState(mem) match {
          case sq: ScalaQueue[ScalaStruct] =>
            if (sow.isEnabled(execState)) {
              val dataVal = execState.getValue[ScalaStruct](data)
              sq.enq(dataVal)
            }
        }
        EmulUnit(sym)

      case Op(DRAMAddress(dram)) =>
        // Get the location of the dram
        val tensor = execState.getTensor(dram)
        SimpleEmulVal(FixedPoint.fromInt(execState.hostMem.getEntry(tensor).start))

      case Op(DRAMIsAlloc(mem)) =>
        SimpleEmulVal(emul.Bool(true))

      case Op(srw@SRAMWrite(mem, data, addr, ens)) if !srw.A.isInstanceOf[Struct[_]] =>
        if (srw.isEnabled(execState)) {
          val realData = execState.getValue[srw.A.L](data)
          val tensor = execState.getTensor[EmulVal[srw.A.L]](mem)
          val address = addr.map(execState.getValue[FixedPoint](_).toInt)
          tensor.write(SimpleEmulVal(realData), address, true)
        }
        EmulUnit(sym)

      case Op(srw@SRAMRead(mem, addr, ens)) if !srw.A.isInstanceOf[Struct[_]] =>
        if (srw.isEnabled(execState)) {
          val tensor = execState.getTensor[EmulVal[srw.A.L]](mem)
          val address = addr.map(execState.getValue[FixedPoint](_).toInt)
          tensor.read(address, srw.isEnabled(execState)).get
        } else {
          SimpleEmulVal[srw.A.L](null, false)
        }

      case _ => super.run(sym, execState)
    }
  }
}
