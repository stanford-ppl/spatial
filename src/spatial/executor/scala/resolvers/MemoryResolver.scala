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

  private def isSimpleMem(sym: Sym[_]): Boolean = {
    return sym.isSRAM || sym.isDRAM || sym.isRegFile || sym.isLUT
  }

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

      case malloc: MemAlloc[_, _] if isSimpleMem(sym) =>
        if (sym.isWriteBuffer) {
          throw SimulationException("Explicitly buffered memories are not currently handled in Scalasim2")
        }
        val elType: ExpType[_, _] = malloc.A.tp
        val elSize = malloc.A.nbits / 8
        val shape = malloc.dims.map(execState.getValue[FixedPoint](_)).map(_.toInt)
        val inits: Option[Seq[Option[SomeEmul]]] = malloc match {
          case RegFileNew(_, init) =>
            dbgs(s"Processing RegFileNew: ${stm(sym)}")
            init match {
              case None =>
                val default = execState(malloc.A.zero.asInstanceOf[Bits[_]].asSym)
                Some(Seq.tabulate(shape.product) {_ => Some(default)})
              case Some(ivals) => Some(ivals.map {iv => Some(execState(iv))})
            }
          case LUTNew(_, init) =>
            dbgs(s"Processing LUTNew: ${stm(sym)}")
            Some(init.map { iv => Some(execState(iv)) })
          case _ => None
        }
        val newTensor = new ScalaTensor[SomeEmul](shape, Some(elSize), inits)
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

      case rfr@RegFileReset(mem, ens) if rfr.isEnabled(execState) =>
        val tens = execState.getTensor[SomeEmul](mem)
        tens.reset()
        EmulUnit(sym)

      case rfsi@RegFileShiftIn(mem, data, addrs, ens, axis) if rfsi.isEnabled(execState) =>
        val tens = execState.getTensor[SomeEmul](mem)
        // This is the base address, guaranteed to be zero on the relevant axis.
        val addr = addrs.map(execState.getValue[FixedPoint](_).toInt)
        val flatIndex = tens.flattenIndex(addr)
        val wData = execState(data)

        val stride = tens.strides(axis)
        // To shift the data in, go through that axis, and shift all the data down by 1.
        Range(0, tens.shape(axis)-1).reverse.foreach({
          i =>
            tens.values(flatIndex + stride * (i + 1)) = tens.values(flatIndex + stride * i)
        })
        tens.values(flatIndex) = Some(wData)
        EmulUnit(sym)

      case read: Reader[_, _] if read.isEnabled(execState) && isSimpleMem(read.mem) =>
        val tensor = execState.getTensor[SomeEmul](read.mem)
        val address = read.addr.map(execState.getValue[FixedPoint](_).toInt)
        val res = tensor.read(address, true)
        res.getOrElse(EmulPoison(sym))

      case write: Writer[_] if write.isEnabled(execState) && isSimpleMem(write.mem) =>
        val tensor = execState.getTensor[SomeEmul](write.mem)
        val address = write.addr.map(execState.getValue[FixedPoint](_).toInt)
        val res = tensor.write( execState(write.data), address, true)
        EmulUnit(sym)

      case lbn@LineBufferNew(rows, cols, stride) =>
        val logicalDims = Seq(rows, cols, stride).map(execState.getValue[FixedPoint](_).toInt)
        val realStride = execState.getValue[FixedPoint](stride).toInt
        new ScalaLB[SomeEmul](logicalDims, Some(lbn.A.nbits/8), None,
          realStride, emit = {x: Any => emit(x)})

      case lbr@LineBufferRead(mem, addrs, ens) if lbr.isEnabled(execState) =>
        val tens = execState.getTensor[SomeEmul](mem)
        val addr = addrs.map(execState.getValue[FixedPoint](_).toInt)
        val fullAddr = if (addr.size == 3) { addr } else {addr ++ Seq(0)}
        val result = tens.read(fullAddr, true).getOrElse(EmulPoison(sym))
//        emit(s"Reading from tensor $mem[${fullAddr}] = $result")
        result

      case lbenq@LineBufferEnq(mem, data, addrs, ens) if lbenq.isEnabled(execState) =>
        val tens = execState(mem) match {
          case slb: ScalaLB[SomeEmul] => slb
        }
        val wData = execState(data)
        val addr = addrs.map(execState.getValue[FixedPoint](_).toInt)
        val Seq(row, col) = addr.take(2)
//        emit(s"Writing to Linebuffer $mem[${addr.mkString(", ")}] <- $wData")
        tens.push(row, col, Some(wData))
        EmulUnit(sym)

      case _ => super.run(sym, op, execState)
    }
  }
}
