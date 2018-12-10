package spatial.node

import argon._
import argon.node._
import forge.tags._
import spatial.lang._

abstract class ShuffleOp[A:Bits] extends Primitive[Tup2[A, Bit]] {
  val A: Bits[A] = Bits[A]

  def in: Tup2[A, Bit]
}

abstract class ShuffleOpVec[A:Bits](implicit val vT: Type[Vec[Tup2[A, Bit]]]) extends Primitive[Vec[Tup2[A, Bit]]] {
  val A: Bits[A] = Bits[A]

  def in: Seq[Sym[Tup2[A, Bit]]]
}

@op case class ShuffleCompress[A:Bits](
  in: Tup2[A, Bit]
) extends ShuffleOp[A]

@op case class ShuffleCompressVec[A:Bits](
  in: Seq[Sym[Tup2[A, Bit]]],
)(implicit val vA: Type[Vec[Tup2[A, Bit]]]) extends ShuffleOpVec[A]

