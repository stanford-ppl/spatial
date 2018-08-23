package spatial.node

import argon._
import argon.node._
import forge.tags._
import spatial.lang._
import spatial.metadata.control.Ctrl

sealed abstract class Accum

case object AccumAdd extends Accum
case object AccumMul extends Accum
case object AccumMin extends Accum
case object AccumMax extends Accum
case object AccumFMA extends Accum
case object AccumUnk extends Accum

sealed abstract class AccumMarker {
  var control: Option[Ctrl] = None
  def first: Bit
}
object AccumMarker {
  object Reg {
    case class Op(reg: Reg[_], data: Bits[_], written: Bits[_], first: Bit, ens: Set[Bit], op: Accum, invert: Boolean) extends AccumMarker
    case class FMA(reg: Reg[_], m0: Bits[_], m1: Bits[_], written: Bits[_], first: Bit, ens: Set[Bit], invert: Boolean) extends AccumMarker
  }
  object Unknown extends AccumMarker { def first: Bit = null }
}

abstract class RegAccum[A:Bits] extends Accumulator[A] {
  def bank = Nil
  def ofs = Nil
}


@op case class RegAccumFMA[A:Bits](
    mem:  Reg[A],
    m0:   Bits[A],
    m1:   Bits[A],
    en:   Set[Bit],
    first: Bit)
  extends RegAccum[A]

@op case class RegAccumOp[A:Bits](
    mem: Reg[A],
    in:  Bits[A],
    en:  Set[Bit],
    op:  Accum,
    first: Bit)
  extends RegAccum[A]

@op case class RegAccumLambda[A:Bits](
    mem: Reg[A],
    en:  Set[Bit],
    func: Lambda1[A,A],
    first: Bit)
  extends RegAccum[A]