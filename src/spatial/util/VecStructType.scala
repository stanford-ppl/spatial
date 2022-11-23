package spatial.util

import argon._
import argon.tags.struct
import forge.tags.api
import spatial.lang._

class VecStructMismatchException(msg: String) extends Exception(msg)

case class VecStructType[T](structFields: Seq[(T, _ <: Bits[_])], errorOnMismatch: Boolean = true)(implicit state: argon.State) {

  type DefaultType = PartialFunction[T, Bits[_]]

  // First, allocate each field to a subset of bits
  private val fieldLoc: Map[T, (Int, Int)] = {
    val allocations = structFields.map(_._2.nbits).scanLeft(0) {
      _ + _
    }.sliding(2).map(_.toSeq).toSeq
    structFields.map(_._1) zip allocations map {
      case (a, Seq(start, end)) =>
        a -> (start, end-1)
    }
  }.toMap

  lazy val structKeys: Seq[T] = structFields.map(_._1)

  val bitWidth: Int = structFields.map(_._2.nbits).sum

  lazy val isEmpty: Boolean = bitWidth == 0

  implicit def bitsEV: Bits[Vec[Bit]] = Vec.bits[Bit](bitWidth)

  def apply(entries: Map[T, Bits[_]], default: DefaultType = PartialFunction.empty)(implicit srcCtx: SrcCtx): VecStruct = VecStruct.fromMap(entries, default)

  override def toString: String = {
    s"$VecStructType($structFields)]<$fieldLoc>($bitWidth)"
  }

  // Capture the type for use inside the VecStruct
  private def vecStructType: VecStructType[T] = this

  @struct case class VecStruct(private val structData: Vec[Bit]) {
    val tp: VecStructType[T] = vecStructType

    assert(bitWidth == structData.nbits, s"Error creating a vector of size $bitWidth from $structData (${structData.nbits})")
    @forge.tags.api def unpack: Map[T, Bits[_]] = {
      val tmpData = this.structData

      (structFields map {
        case (name: T, bits: Bits[_]) =>
          val (start, stop) = fieldLoc(name)
          val sliced = tmpData(start until stop)

          assert(sliced.nbits == bits.nbits, s"Trying to unpack ${bits.nbits} from ${sliced.nbits}")
          name -> sliced
      }).toMap
    }
  }

  object VecStruct {
    @forge.tags.api def fromMap(entries: Map[T, Bits[_]], default: DefaultType): VecStruct = {
      val data = structFields.map {
        case (name: T, bits: Bits[_]) =>
          entries.get(name) match {
            case Some(v) =>
              dbgs(s"Name: $name ${v.nbits}, ${bits.nbits}")
              assert(v.nbits == bits.nbits, s"Mismatched bits: Got $v of size ${v.nbits} for a field $name of size ${bits.nbits} [${implicitly[argon.SrcCtx]}]")
              v.asBits
            case None =>
              val defaultResult = default.applyOrElse(name, {
                _: T => if (errorOnMismatch) {
                  throw new VecStructMismatchException(s"Could not find field $name of type ${name.getClass} in $entries")
                } else {
                  bits.zero.asInstanceOf[Bits[_]].asBits
                }
              })
              dbgs(s"Result: $defaultResult[${defaultResult.nbits}]")
              assert(defaultResult.nbits == bits.nbits, s"Mismatched Bits: Default ${defaultResult} of size ${defaultResult.nbits} for a field $name of size ${bits.nbits} [${implicitly[argon.SrcCtx]}]")
              defaultResult.asBits
          }
      }
      VecStruct(Vec.concat(data))
    }

    @forge.tags.api def zero: VecStruct = Bits[VecStruct].zero
  }

  type VFIFO = FIFO[VecStruct]

  def VFIFO(size: I32): VFIFO = {
    FIFO[VecStruct](size)
  }
}

