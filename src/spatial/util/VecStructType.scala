package spatial.util

import argon._
import spatial.lang._

class VecStructMismatchException(msg: String) extends Exception(msg)

case class VecStructType[T](fields: Seq[(T, Bits[_])], errorOnMismatch: Boolean = true)(implicit state: argon.State) {
  // First, allocate each field to a subset of bits
  val fieldLoc: Map[T, (Int, Int)] = {
    val allocations = fields.map(_._2.nbits).scanLeft(0) {
      _ + _
    }.sliding(2).map(_.toSeq).toSeq
    fields.map(_._1) zip allocations map {
      case (a, Seq(start, end)) =>
        a -> (start, end-1)
    }
  }.toMap

  lazy val bitWidth: Int = fields.map(_._2.nbits).sum

  lazy val isEmpty: Boolean = bitWidth == 0

  def bitsEV: Bits[Vec[Bit]] = Vec.bits[Bit](bitWidth)

  def packStruct(entries: Map[T, Bits[_]]): Vec[Bit] = {
    val data = fields.map {
      case (name, bits) =>
        entries.get(name) match {
          case Some(v) =>
            assert(v.nbits == bits.nbits, s"Mismatched bits: Got $v of size ${v.nbits} for a field $bits of size ${bits.nbits}")
            v.asBits
          case None if !errorOnMismatch =>
            bits.asBits
          case None =>
            throw new VecStructMismatchException(s"Could not find field $name of type ${name.getClass} in $entries")
        }
    }
    Vec.concat(data)
  }

  def unpackStruct(vec: Vec[Bit])(implicit srcCtx: SrcCtx): Map[T, Bits[_]] = {
    assert(bitWidth == vec.nbits, s"Error unpacking a vector of size $bitWidth from $vec (${vec.nbits}) [$srcCtx]")
    (fields map {
      case (name, bits) =>
        implicit def bEV: Bits[Bits[bits.R]] = bits.asInstanceOf[Bits[Bits[bits.R]]]
        val (start, stop) = fieldLoc(name)
        val sliced = vec(start until stop)
        assert(sliced.nbits == bits.nbits, s"Trying to unpack ${bits.nbits} from ${sliced.nbits}")
        name -> sliced.as[Bits[bits.R]]
    }).toMap
  }
  override def toString: String = {
    s"$VecStructType($fields)]<$fieldLoc>($bitWidth)"
  }
}
