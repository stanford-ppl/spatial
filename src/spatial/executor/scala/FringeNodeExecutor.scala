package spatial.executor.scala

import argon.lang.Bits
import argon.{Op, Sym, dbgs, emit, indentGen, stm}
import emul.FixedPoint
import spatial.executor.scala.memories.{ScalaQueue, ScalaStruct}
import spatial.node._

import scala.collection.mutable

object FringeNodeExecutor {
  @forge.tags.stateful def apply(op: Sym[_], execState: ExecutionState): OpExecutorBase = op match {
    case Op(_: FringeDenseLoad[_, _]) => new FringeDenseLoadExecutor(op, execState)
    case Op(_: FringeDenseStore[_, _]) => new FringeDenseStoreExecutor(op, execState)
    case _ => throw new NotImplementedError(s"Haven't implemented ${stm(op)} yet!")
  }
}

trait FringeOpExecutor extends OpExecutorBase {
  val kLoadLatency: Int = 40
  // Estimated 200 bytes/tick
  val kLoadThroughput: Double = 200
  val maxInFlightRequests = 32

  val memEntry: MemEntry
  val cmdStream: ScalaQueue[ScalaStruct]

  val bytesPerElement: Int

  case class DRAMAccessData(base: Int, nElements: Int)
  @forge.tags.stateful def decodeCmd(cmd: ScalaStruct): DRAMAccessData = {
    val offset = cmd.fieldValues("offset") match {
      case ev: EmulVal[FixedPoint] => ev.value.toInt
    }
    val sizeInBytes = cmd.fieldValues("size") match {
      case ev: EmulVal[FixedPoint] => ev.value.toInt
    }



    // Convert these to address ranges
    val nElements = sizeInBytes / bytesPerElement

    // val addr_bytes = (dramAddr() * bytesPerWord).to[I64] + dram.address

    // dramAddr() = (addr_bytes - dram.address) / bytesPerWord
    val dramIndex = (offset - memEntry.start) / bytesPerElement
    emit(s"Decoding $cmd:")
    indentGen {
      emit(s"Offset = $offset")
      emit(s"Size (B) = $sizeInBytes")
      emit(s"MemEntryStart = ${memEntry.start}")
      emit(s"DramIndex = $dramIndex")
      emit(s"BytesPerElement = $bytesPerElement")
    }
    DRAMAccessData(dramIndex, nElements)
  }

  protected def computeLatency(nElements: Int): Int = {
//    return 1
    // In Bytes
    val transferSize = bytesPerElement * nElements
    scala.math.ceil(transferSize / kLoadThroughput).toInt + kLoadLatency
  }
}

class FringeDenseLoadExecutor(op: Sym[_], override val execState: ExecutionState)(implicit state: argon.State) extends FringeOpExecutor {
  val Op(fdl@FringeDenseLoad(dramSym, cmdStreamSym, dataStreamSym)) = op


  type ET = fdl.A.L
  val dram = execState.getTensor[EmulVal[ET]](dramSym)
  override val bytesPerElement: Int = dram.elementSize.get

  override val memEntry = execState.hostMem.getEntry(dram)
  override val cmdStream = execState(cmdStreamSym) match { case sq:ScalaQueue[ScalaStruct] => sq }
  val dataStream = execState(dataStreamSym) match { case sq:ScalaQueue[EmulVal[ET]] => sq }

  class DRAMLoad(var latencyRemaining: Int, val accessData: DRAMAccessData) {
    override def toString: String = s"DRAMLoad($latencyRemaining, $accessData)"
  }

  val requests = mutable.Queue[DRAMLoad]()

  override def tick(): Unit = {

    // decrement all of the current latencies
    requests.foreach { load => load.latencyRemaining -= 1 }

    emit(s"$this Status Report:")
    indentGen {
      emit(s"Requests: ${requests.map(_.toString).mkString(", ")}")
      emit(s"Pending Cmd: ${cmdStream.headOption}")
    }

    if (!cmdStream.isEmpty && requests.size < maxInFlightRequests) {
      val cmd = cmdStream.deq()
      val accessData = decodeCmd(cmd)

      requests.enqueue(new DRAMLoad(computeLatency(accessData.nElements), accessData))
    }

    requests.headOption match {
      case Some(load) if load.latencyRemaining <= 0 =>
        (0 until load.accessData.nElements) foreach {
          shift =>
            val readVal = dram.values(load.accessData.base + shift).get
            dataStream.enq(readVal)
        }
        requests.dequeue()
      case _ =>
    }
  }

  override def status: Status = if (requests.nonEmpty || !cmdStream.isEmpty) {
    Running
  } else Indeterminate
}

class FringeDenseStoreExecutor(op: Sym[_], override val execState: ExecutionState)(implicit state: argon.State) extends FringeOpExecutor {
  val Op(fdr@FringeDenseStore(dramSym, cmdStreamSym, dataStreamSym, ackStreamSym)) = op

  type ET = fdr.A.L
  val dram = execState.getTensor[EmulVal[ET]](dramSym)
  override val bytesPerElement: Int = dram.elementSize.get

  override val memEntry = execState.hostMem.getEntry(dram)
  override val cmdStream = execState(cmdStreamSym) match { case sq:ScalaQueue[ScalaStruct] => sq }
  val dataStream = execState(dataStreamSym) match { case sq:ScalaQueue[EmulVal[ET]] => sq }
  val ackStream = execState(ackStreamSym) match {case sq: ScalaQueue[EmulVal[emul.Bool]] => sq }

  class DRAMStore(var latencyRemaining: Int, val accessData: DRAMAccessData, val values: Seq[Option[EmulVal[_]]]) {
    override def toString: String = s"DRAMStore($latencyRemaining, $accessData, $values)"
  }
  val requests = mutable.Queue[DRAMStore]()

  override def tick(): Unit = {

    emit(s"$this Status Report:")
    indentGen {
      emit(s"Requests: ${requests.map(_.toString).mkString(", ")}")
      emit(s"Pending Cmd: ${cmdStream.headOption}")
      emit(s"Current Data: ${dataStream.size}")
    }


    // decrement all of the current latencies
    requests.foreach { req => req.latencyRemaining -= 1 }

    if (!cmdStream.isEmpty && requests.size < maxInFlightRequests) {

      val dramAccess = decodeCmd(cmdStream.head match {case sc: ScalaStruct => sc})

      dbgs(s"Processing request: $cmdStream.head")
      dbgs(s"$dramAccess")

      if (dataStream.size >= dramAccess.nElements) {
        val data = Seq.tabulate(dramAccess.nElements) {
          _ =>
            val entry = dataStream.deq() match {case sc: ScalaStruct => sc}
            val en = entry.fieldValues("_2") match {
              case ev: EmulVal[emul.Bool] => ev.value.value
            }
            if (en) {
              val payload = entry.fieldValues("_1")
              Some(payload)
            } else None

        }

        requests.enqueue(new DRAMStore(computeLatency(dramAccess.nElements), dramAccess, data))
        cmdStream.deq()
      }
    }

    if (requests.nonEmpty && requests.head.latencyRemaining <= 0) {
      // Pop the first request off, and handle it
      val load = requests.dequeue()
      load.values.zipWithIndex foreach {
        case (value@Some(_), offset) =>
          dram.values(offset + load.accessData.base) = value
        case _ =>
      }
      ackStream.enq(SimpleEmulVal(emul.Bool(true)))
    }
  }

  override def status: Status = {
    if (requests.nonEmpty || !cmdStream.isEmpty || !dataStream.isEmpty) {
      Running
    } else Indeterminate
  }
}


