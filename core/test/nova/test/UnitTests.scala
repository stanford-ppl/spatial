package nova.test

import nova.lang._
import utest._

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

object UnitTests extends Testbench { val tests = Tests {
  'NestedLoopTest - test(NestedLoopTest)
}}
