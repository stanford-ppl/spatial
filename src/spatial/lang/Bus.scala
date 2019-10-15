package spatial.lang

import argon.Mirrorable
import argon.tags.struct
import forge.tags._
import argon.lang.types._
import argon._

case class Pin(name: String) {
  override def toString: String = name
}

abstract class Bus extends Mirrorable[Bus] {
  @rig def nbits: Int
  def mirror(f:Tx) = this
}

object Bus {
  def apply(valid: Pin, data: Pin*) = PinBus(valid, data)
  def apply(valid: String, data: String*) = PinBus(Pin(valid), data.map(Pin.apply))
}

case class PinBus(valid: Pin, data: Seq[Pin]) extends Bus {
  override def toString: String = "Bus(" + valid.toString + ": " + data.mkString(", ") + ")"
  @rig def nbits: Int = data.length
}

/* Type for AxiStream interface for a 256-bit datapath. TODO: tid tdest and tuser widths are made up */
@struct case class AxiStream256(tdata: U256, tstrb: U32, tkeep: U32, tlast: Bit, tid: U8, tdest: U8, tuser: U32)
object AxiStream256Data {
  /* Helper for those who don't care about the other fields of the axi stream */
  @stateful def apply(tdata: U256): AxiStream256 = AxiStream256(tdata, Bits[U32].from(0), Bits[U32].from(0), Bit(false), Bits[U8].from(0), Bits[U8].from(0), Bits[U32].from(0))
}

/* Type for AxiStream interface for a 256-bit datapath. TODO: tid tdest and tuser widths are made up */
@struct case class AxiStream512(tdata: U512, tstrb: U64, tkeep: U64, tlast: Bit, tid: U8, tdest: U8, tuser: U64)
object AxiStream512Data {
  /* Helper for those who don't care about the other fields of the axi stream */
  @stateful def apply(tdata: U512): AxiStream512 = AxiStream512(tdata, Bits[U64].from(0), Bits[U64].from(0), Bit(false), Bits[U8].from(0), Bits[U8].from(0), Bits[U64].from(0))
}

@struct case class BurstCmd(offset: I64, size: I32, isLoad: Bit)
@struct case class IssuedCmd(size: I32, start: I32, end: I32)

case object AxiStream256Bus extends Bus {
  @rig def nbits: Int = 256
}

case object AxiStream512Bus extends Bus {
  @rig def nbits: Int = 512
}

abstract class DRAMBus[A:Bits] extends Bus { @rig def nbits: Int = Bits[A].nbits }

/*
 * Bus type on StreamIn and StreamOut. Each element in Stream In Out will be a new line in csv.
 * If A is struct, different fields will be in columns of the CSV.
 * */
case class FileBus[A:Bits](fileName:String) extends Bus { @rig def nbits: Int = Bits[A].nbits }
/*
 * Same as FileBus. Except A must be struct type with last field in Bit type. The last field
 * will be interpreted as last bit of the stream to terminate simulation.
 * */
case class FileEOFBus[A:Bits](fileName:String)(implicit state:State) extends Bus { 
  Type[A] match {
    case a:Struct[_] if a.fields.last._2 == Type[Bit] => 
    case a => 
      error(s"EFOBus must have struct type with last field in Bit. Got type ${a}")
      state.logError()
  }
  @rig def nbits: Int = Bits[A].nbits
}

case object BurstCmdBus extends DRAMBus[BurstCmd]
case object BurstAckBus extends DRAMBus[Bit]
case class BurstDataBus[A:Bits]() extends DRAMBus[A]
case class BurstFullDataBus[A:Bits]() extends DRAMBus[Tup2[A, Bit]]

case object GatherAddrBus extends DRAMBus[I64]
case class GatherDataBus[A:Bits]() extends DRAMBus[A]

case class ScatterCmdBus[A:Bits]() extends DRAMBus[Tup2[A, I64]]
case object ScatterAckBus extends DRAMBus[Bit]