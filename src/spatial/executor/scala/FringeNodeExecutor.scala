package spatial.executor.scala

import argon.lang.Bits
import argon.{Op, Sym, dbgs, emit, indentGen, stm}
import emul.FixedPoint
import spatial.executor.scala.memories.{ScalaQueue, ScalaStruct}
import spatial.node._

import scala.collection.mutable

object FringeNodeExecutor {
  @forge.tags.stateful def apply(op: Sym[_], execState: ExecutionState): ControlExecutor = op match {
    case Op(_: FringeDenseLoad[_, _]) => new FringeDenseLoadExecutor(op, execState)
    case Op(_: FringeDenseStore[_, _]) => new FringeDenseStoreExecutor(op, execState)
    case _ => throw new NotImplementedError(s"Haven't implemented ${stm(op)} yet!")
  }
}

trait FringeOpExecutor extends ControlExecutor {

  val memEntry: MemEntry
  val cmdStream: ScalaQueue[ScalaStruct]

  val bytesPerElement: Int

  case class DRAMAccessData(base: Int, nElements: Int, size: Int)
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
    DRAMAccessData(dramIndex, nElements, sizeInBytes)
  }

  override def isDeadlocked: Boolean = false
}

class FringeDenseLoadExecutor(op: Sym[_], override val execState: ExecutionState) extends FringeOpExecutor {
  val Op(fdl@FringeDenseLoad(dramSym, cmdStreamSym, dataStreamSym)) = op

  override val ctrl: Sym[_] = op

  type ET = fdl.A.L
  val dram = execState.getTensor[EmulVal[ET]](dramSym)
  override val bytesPerElement: Int = dram.elementSize.get

  override val memEntry = execState.hostMem.getEntry(dram)
  override val cmdStream = execState(cmdStreamSym) match { case sq:ScalaQueue[ScalaStruct] => sq }
  val dataStream = execState(dataStreamSym) match { case sq:ScalaQueue[EmulVal[ET]] => sq }

  case class DRAMLoad(request: Request, accessData: DRAMAccessData)

  val requests = mutable.Queue[DRAMLoad]()

  override def tick(): Unit = {
    if (!cmdStream.isEmpty) {
      val cmd = cmdStream.deq()
      val accessData = decodeCmd(cmd)
      val request = execState.memoryController.makeRequest(accessData.size)
      requests.enqueue(DRAMLoad(request, accessData))
    }

    requests.headOption match {
      case Some(DRAMLoad(request, accessData)) if request.status == RequestFinished =>
        (0 until accessData.nElements) foreach {
          shift =>
            val readVal = dram.values(accessData.base + shift).get
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

  override val ctrl: Sym[_] = op

  type ET = fdr.A.L
  val dram = execState.getTensor[EmulVal[ET]](dramSym)
  override val bytesPerElement: Int = dram.elementSize.get

  override val memEntry = execState.hostMem.getEntry(dram)
  override val cmdStream = execState(cmdStreamSym) match { case sq:ScalaQueue[ScalaStruct] => sq }
  val dataStream = execState(dataStreamSym) match { case sq:ScalaQueue[EmulVal[ET]] => sq }
  val ackStream = execState(ackStreamSym) match {case sq: ScalaQueue[EmulVal[emul.Bool]] => sq }

  case class DRAMStore(request: Request, accessData: DRAMAccessData, values: Seq[Option[EmulVal[_]]])

  val requests = mutable.Queue[DRAMStore]()

  override def tick(): Unit = {

    if (!cmdStream.isEmpty) {

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

        requests.enqueue(new DRAMStore(execState.memoryController.makeRequest(dramAccess.size), dramAccess, data))
        cmdStream.deq()
      }
    }

    requests.headOption match {
      case Some(DRAMStore(request, accessData, values)) if request.status == RequestFinished =>
        requests.dequeue()
        values.zipWithIndex foreach {
          case (value@Some(_), offset) =>
            dram.values(offset + accessData.base) = value
          case _ =>
        }
        ackStream.enq(SimpleEmulVal(emul.Bool(true)))
      case _ =>
    }
  }

  override def status: Status = {
    if (requests.nonEmpty || !cmdStream.isEmpty || !dataStream.isEmpty) {
      Running
    } else Indeterminate
  }
}


