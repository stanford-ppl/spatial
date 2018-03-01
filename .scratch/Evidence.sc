trait A[B] {
  val ev: B <:< A[B]
  private implicit val evv = ev
}

case class C() extends A[C] {
  val ev: (C <:< A[C]) = implicitly[C <:< A[C]]
}

import emul.{FloatPoint,FixedPoint,FixFormat}

val Byte = FixFormat(true,8,0)
val Int = FixFormat(true,32,0)

val x = FloatPoint.fromFloat(128.5f)

val y = x.toFixedPoint(Byte)

val m = x.bits
println(x.fancyBitString())

val z = FixedPoint.fromBits(m,Int)