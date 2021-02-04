package fringe.utils

import chisel3.{Data, UInt, Vec}

trait UIntLike {
  def asUInt: UInt
  def raw = asUInt
  def r = asUInt
  def :=(other: UIntLike): Unit
  def :=(other: UInt): Unit = {
    val that = other
    this := new UIntLike {
      override def asUInt: UInt = that

      // Don't need this I think
      override def :=(other: UIntLike): Unit = Unit
    }
  }
}

class VecUInt[T <: Data](vec: Vec[T]) extends UIntLike {
  override def asUInt: UInt = vec.asUInt()

  override def :=(other: UIntLike): Unit = {
    println(s"Assigning to VecUInt: ${vec}")
    val otherUInt: UInt = other.asUInt
    vec := otherUInt.asTypeOf(vec)
  }
}
