package spatial.tests.feature.control

import spatial.dsl._

@test class ControlEnable extends SpatialTest {
  override def runtimeArgs: Args = "9"


  def main(args: Array[String]): Unit = {
    val vectorA = Array.fill[Int](128) {
      4 // Please don't change this to random
    }
    val vectorB = Array.fill[Int](128) {
      8 // Please don't change this to random
    }
    val vectorC = Array.fill[Int](128) {
      14 // Please don't change this to random
    }
    val vecA = DRAM[Int](128)
    val vecB = DRAM[Int](128)
    val vecC = DRAM[Int](128)
    val result = DRAM[Int](128)
    val x = ArgIn[Int]
    setArg(x, args(0).to[Int])
    setMem(vecA, vectorA)
    setMem(vecB, vectorB)
    setMem(vecC, vectorC)
    Accel {

      val mem = SRAM[Int](128)

      if (x <= 4.to[Int]) {
        mem load vecA
      } else if (x <= 8.to[Int]) {
        mem load vecB
      } else {
        mem load vecC
      }

      result store mem
    }
    val res = getMem(result)
    val gold = Array.fill(128){ if (args(0).to[Int] <= 4) 4.to[Int] else if (args(0).to[Int] <= 8) 8.to[Int] else 14.to[Int] }
    println("Expected array of : " + gold(0) + ", got array of : " + res(0))
    assert(res == gold)
  }
}
