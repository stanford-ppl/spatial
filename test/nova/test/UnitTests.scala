package nova.test

import spatial.dsl._
import utest._

@spatial object NestedLoopTest {
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

@spatial object IfThenElseTest {
  def main(): Void = {
    Accel {
      val c = random[Bit]
      val a = random[I32]
      val b = random[I32]

      val x0 = if (c) 1 else 0
      val x1 = if (c) a else 0
      val x2 = if (c) 0 else b
      val x3 = if (c) a else b
      println(r"x0: $x0, x1: $x1, x2: $x2, x3: $x3")

      val m0 = mux(c, 1, 0)
      val m1 = mux(c, a, 0)
      val m2 = mux(c, 0, b)
      val m3 = mux(c, a, b)
      println(r"m0: $m0, m1: $m1, m2: $m2, m3: $m3")
    }
  }
}

object UnitTests extends Testbench { val tests = Tests {
  'NestedLoopTest - test(NestedLoopTest)
  'IfThenElseTest - test(IfThenElseTest)
}}
