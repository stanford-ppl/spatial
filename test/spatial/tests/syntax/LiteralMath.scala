package spatial.tests.syntax

import spatial.dsl._

@test class LiteralMath extends SpatialTest {
  def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    val in = ArgIn[Int]
    val io = HostIO[Int]
    val out = ArgOut[Int]

    Accel {
      val r = Reg[Int]
      val a1: Int = 1 + r
      val b1: Int = 1 - r
      val c1: Int = 1 * r
      val d1: Int = 1 / r
      val e1: Int = 1 % r

      val a2: Int = 1.0 + r
      val b2: Int = 1.0 - r
      val c2: Int = 1.0 * r
      val d2: Int = 1.0 / r
      val e2: Int = 1.0 % r

      val a3: Int = 1.0f + r
      val b3: Int = 1.0f - r
      val c3: Int = 1.0f * r
      val d3: Int = 1.0f / r
      val e3: Int = 1.0f % r

      val h: Int = random[Int]
      val a4: Int = 1 + h
      val b4: Int = 1 - h
      val c4: Int = 1 * h
      val d4: Int = 1 / h
      val e4: Int = 1 % h

      val a5: Int = 1.0 + h
      val b5: Int = 1.0 - h
      val c5: Int = 1.0 * h
      val d5: Int = 1.0 / h
      val e5: Int = 1.0 % h

      val a6: Int = 1.0f + h
      val b6: Int = 1.0f - h
      val c6: Int = 1.0f * h
      val d6: Int = 1.0f / h
      val e6: Int = 1.0f % h
    }
  }
}
