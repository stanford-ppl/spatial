package spatial.lang

import argon.Mirrorable
import spatial.tags._

case class Pin(name: String) {
  override def toString: String = name
}

abstract class Bus extends Mirrorable[Bus] {
  def length: Int
  def mirror(f:Tx) = this
}

object Bus {
  def apply(valid: Pin, data: Pin*) = PinBus(valid, data)
  def apply(valid: String, data: String*) = PinBus(Pin(valid), data.map(Pin.apply))
}

case class PinBus(valid: Pin, data: Seq[Pin]) extends Bus {
  override def toString: String = "Bus(" + valid.toString + ": " + data.mkString(", ") + ")"
  def length: Int = data.length
}

@struct case class BurstCmd(offset: I64, size: I32, isLoad: Bit)
@struct case class IssuedCmd(size: I32, start: I32, end: I32)

abstract class DRAMBus[A:Bits] extends Bus { def length: Int = Bits[A].nbits }

case object BurstCmdBus extends DRAMBus[BurstCmd]
case object BurstAckBus extends DRAMBus[Bit]
case class BurstDataBus[A:Bits]() extends DRAMBus[A]
case class BurstFullDataBus[A:Bits]() extends DRAMBus[Tup2[A, Bit]]

case object GatherAddrBus extends DRAMBus[I64]
case class GatherDataBus[A:Bits]() extends DRAMBus[A]

case class ScatterCmdBus[A:Bits]() extends DRAMBus[Tup2[A, I64]]
case object ScatterAckBus extends DRAMBus[Bit]