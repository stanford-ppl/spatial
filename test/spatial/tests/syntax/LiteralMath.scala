package spatial.tests.syntax

import spatial.dsl._

@spatial class LiteralMath extends SpatialTest {
  override def runtimeArgs: Args = "32 16"

  def main(args: Array[String]): Unit = {
    Accel { /* No hardware lol */ }

    val in = args(0).to[Int]
    val h  = args(1).to[Int]
    val r = ArgIn[Int]
    setArg(r, in)
    val a1: Int = 1 + r; assert(a1 == 33)
    val b1: Int = 1 - r; assert(b1 == -31)
    val c1: Int = 1 * r; assert(c1 == 32)
    val d1: Int = 1 / r; assert(d1 == 0)
    val e1: Int = 1 % r; assert(e1 == 1)

    val a2: Int = 1.0 + r; assert(a2 == 33)
    val b2: Int = 1.0 - r; assert(b2 == -31)
    val c2: Int = 1.0 * r; assert(c2 == 32)
    val d2: Int = 1.0 / r; assert(d2 == 0)
    val e2: Int = 1.0 % r; assert(e2 == 1)

    val a3: Int = 1.0f + r; assert(a3 == 33)
    val b3: Int = 1.0f - r; assert(b3 == -31)
    val c3: Int = 1.0f * r; assert(c3 == 32)
    val d3: Int = 1.0f / r; assert(d3 == 0)
    val e3: Int = 1.0f % r; assert(e3 == 1)

    val a4: Int = 1 + h; assert(a4 == 17)
    val b4: Int = 1 - h; assert(b4 == -15)
    val c4: Int = 1 * h; assert(c4 == 16)
    val d4: Int = 1 / h; assert(d4 == 0)
    val e4: Int = 1 % h; assert(e4 == 1)

    val a5: Int = 1.0 + h; assert(a5 == 17)
    val b5: Int = 1.0 - h; assert(b5 == -15)
    val c5: Int = 1.0 * h; assert(c5 == 16)
    val d5: Int = 1.0 / h; assert(d5 == 0)
    val e5: Int = 1.0 % h; assert(e5 == 1)

    val a6: Int = 1.0f + h; assert(a6 == 17)
    val b6: Int = 1.0f - h; assert(b6 == -15)
    val c6: Int = 1.0f * h; assert(c6 == 16)
    val d6: Int = 1.0f / h; assert(d6 == 0)
    val e6: Int = 1.0f % h; assert(e6 == 1)
  }
}
