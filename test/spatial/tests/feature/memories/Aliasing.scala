package spatial.tests.feature.memories

import spatial.dsl._

@spatial class NestedAlias extends SpatialTest {

  def main(args: Array[String]): Void = {
    Accel {
      val x = SRAM[I32](32)
      Foreach(0 until 32){i => x(i) = i }
      val y = x(2::2::16) // 8 elements
      val z = y(4::2::8)  // This is massively confusing but technically allowed
                          // Translation:
                          // Starting from element 2 (inclusive) and taking every other element,
                          // take the 4th element of those and every other element following until the 8th element
                          // 0 1 2 3 4 5 6 7 8 9 A B C D E F... = x
                          //     ^   ^   ^   ^   ^   ^   ^
                          //     0   1   2   3   4   5   6      = y
                          //                     ^       ^
                          //                     0       1      = z
      println(z(0))       // Should be 10
      assert(z(0) == 10)
    }
  }
}
