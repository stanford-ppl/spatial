package spatial.lang

import argon.Mirrorable
import argon.tags.struct
import forge.tags._
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

@struct case class BurstCmd(offset: I64, size: I32, isLoad: Bit)
@struct case class IssuedCmd(size: I32, start: I32, end: I32)

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
case class BlackBoxBus[A:Bits](name:String) extends Bus {
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

case class CoalesceSetupBus[A:Bits]() extends DRAMBus[Tup2[I64, I32]]
case class CoalesceCmdBus[A:Bits]() extends DRAMBus[Tup2[A, Bit]]
case object CoalesceAckBus extends DRAMBus[Bit]

case class DynStoreSetupBus[A:Bits]() extends DRAMBus[I64]
case class DynStoreCmdBus[A:Bits]() extends DRAMBus[Tup2[A, Bit]]
case object DynStoreAckBus extends DRAMBus[Bit]

/** Abstract class for any bus which is specific to a particular target and 
  * is created directly in the host part of the spatial app
  */
abstract class TargetBus[A:Bits] extends Bus { @rig def nbits: Int = Bits[A].nbits }

case object CXPPixelBus extends TargetBus[U256]
