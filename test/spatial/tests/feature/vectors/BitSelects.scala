package spatial.tests.feature.vectors

import spatial.dsl._

@spatial class BitSelects extends SpatialTest {

  def main(args: Array[String]): Unit = {
    Accel {
      val x = 9.to[Int]
      val v = x.asBits
      println(v)
      println("bit 0: " + x.bit(0))
      println("bit 1: " + x.bit(1))
      println("bit 2: " + x.bit(2))
      println("bit 3: " + x.bit(3))

      assert(x.bit(0), "First bit should be 1")
      assert(!x.bit(1), "Second bit should be 0")
      assert(!x.bit(2), "Third bit should be 0")
      assert(x.bit(3), "Fourth bit should be 1")
    }
  }
}
