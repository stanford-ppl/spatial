package spatial.tests.feature.unit

import spatial.dsl._

@spatial class SumRows extends SpatialTest {

  def main(args: Array[String]): Unit = {

    Accel {
      val M = 32
      val N = 32
      val sram = SRAM[Int](M, N)
      val out = SRAM[Int](M)

      Foreach(M by 1){i =>
        val sum = Reduce(0)(N by 1){j =>
          sram(i,j)
        }{_+_}
        out(i) = sum
      }

    }

  }

}
