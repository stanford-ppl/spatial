package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._

trait PIRGenStream extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@StreamInNew(bus)  =>
      stateMem(lhs, "FIFO()")
      val streams = mapStruct(lhs.asMem.A) { s => Lhs(lhs, s.map { _._1 })}
      emit(src"""streamIn($streams, $bus)""")

    case op@StreamOutNew(bus) =>
      val streams = mapStruct(lhs.asMem.A) { s => Lhs(lhs, s.map { _._1 })}
      stateMem(lhs, "FIFO()")
      emit(src"""streamOut($streams, $bus)""")

    case op@StreamInBankedRead(strm, ens) =>
      stateRead(lhs, strm, None, None, ens)
      val Def(StreamInNew(bus)) = strm
      bus match {
        case BurstAckBus | ScatterAckBus =>
          val count = s"countAck_$lhs"
          emit(src"""val $count = CountAck().input($lhs).tp(Bool)""")
          emit(src"""MemWrite().setMem(argOut().name("$count").tp(Bool)).data($count)""")
        case bus =>
      }

    case StreamOutBankedWrite(strm, data, ens) =>
      stateWrite(lhs, strm, None, None, data, ens)

    case _ => super.genAccel(lhs, rhs)
  }

  override protected def quoteOrRemap(arg: Any): String = arg match {
    case x:DRAMBus[_] => "DRAMBus"
    case x => super.quoteOrRemap(arg)
  }

}
