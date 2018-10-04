package spatial.tests.feature.retiming

import argon._
import spatial.dsl._
import spatial.node.DelayLine

@spatial class SimpleRetimePipe extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val a = ArgIn[Int]
    val b = ArgIn[Int]
    val c = ArgIn[Int]
    val d = ArgOut[Int]
    Accel {
      d := a * b + c
    }
    println("d: " + getArg(d))
    assert(getArg(d) == 0.to[Int])
  }

  override def checkIR(block: Block[_]): Result = {
    val delays = block.nestedStms.count{case Op(_:DelayLine[_]) => true; case _ => false }
    delays shouldBe 3
    super.checkIR(block)
  }
}
