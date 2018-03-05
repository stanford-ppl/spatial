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

  def test[T:Bits](x: T, y: T): Void = {
    val c = random[Bit]
    val a1 = mux(c, 0, x)
    val a2 = mux(c, x, 0)
    val a3 = mux(c, x, y)
    println(r"a1: $a1, a2: $a2, a3: $a3")

    val b1 = if (c) 0 else x
    val b2 = if (c) x else 0
    val b3 = if (c) x else y
    println(r"b1: $b1, b2: $b2, b3: $b3")
  }

  def main(): Void = {
    Accel {
      val c = random[Bit]
      val i16 = random[I16]
      val i32 = random[I32]
      val i64 = random[I64]

      val x0 = if (c) 1 else 0
      val x1 = if (c) i32 else 0
      val x2 = if (c) 0 else i32
      val x3 = if (c) i32 else i32
      println(r"x0: $x0, x1: $x1, x2: $x2, x3: $x3")

      //val y0 = if (c) 1 else 0.2
      val y1 = if (c) i16 else 0
      val y2 = if (c) 0 else i16
      val y3 = if (c) i16 else i16
      println(r"y0: N/A, y1: $y1, y2: $y2, y3: $y3")

      val m0 = mux(c, 1, 0)
      val m1 = mux(c, i32, 0)
      val m2 = mux(c, 0, i32)
      val m3 = mux(c, i32, i32)
      println(r"m0: $m0, m1: $m1, m2: $m2, m3: $m3")

      //val n0 = mux(c, 1, 0.2)
      val n1 = mux(c, i16, 0)
      val n2 = mux(c, 0, i16)
      val n3 = mux(c, i16, i16)
      println(r"n0: N/A, n1: $n1, n2: $n2, n3: $n3")

      test(i32, i32)
    }
  }
}

object UnitTests extends Testbench { val tests = Tests {
  'NestedLoopTest - test(NestedLoopTest)
  'IfThenElseTest - test(IfThenElseTest)
}}
