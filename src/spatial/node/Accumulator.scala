package spatial.node

import argon._
import forge.tags._
import spatial.lang._

sealed abstract class Accum
object Accum {
  case object Add extends Accum
  case object Mul extends Accum
  case object Min extends Accum
  case object Max extends Accum
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