package fringe.test

import chisel3._
import fringe.templates.math._

class FixedPointTester(val s: Boolean, val d: Int, val f: Int) extends Module {
  def this(tuple: (Boolean, Int, Int)) = this(tuple._1, tuple._2, tuple._3)
  val io = IO( new Bundle {
    val num1 = Input(UInt((d+f).W))
    val num2 = Input(UInt((d+f).W))

    val add_result = Output(UInt((d+f).W))
    val prod_result = Output(UInt((d+f).W))
    val sub_result = Output(UInt((d+f).W))
    val quotient_result = Output(UInt((d+f).W))
  })

  val fix1 = Wire(new FixedPoint(s,d,f))
  io.num1 := fix1.r
  val fix2 = Wire(new FixedPoint(s,d,f))
  io.num2 := fix2.r
  val sum = fix1 + fix2
  sum.r := io.add_result
  val prod = fix1 * fix2
  prod.r := io.prod_result
  val sub = fix1 - fix2
  sub.r := io.sub_result
  val quotient = fix1 / fix2
  quotient.r := io.quotient_result
}
