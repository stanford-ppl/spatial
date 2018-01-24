package pcc.test

import pcc.lang._

object NestedLoopTest extends Test {
  def main(): Void = Accel {
    val x = SRAM[I32](64)
    Foreach(64 by 32){i =>
      Foreach(32 by 1){j =>
        x(i + j) = i + j
      }
      println("Hello!")
    }
  }
}

class UnitTests extends Tests {
  "NestedLoopTest" should "compile" in { test(NestedLoopTest) }

}
