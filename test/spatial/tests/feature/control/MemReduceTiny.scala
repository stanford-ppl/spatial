package spatial.tests.feature.control

import spatial.dsl._

@spatial class MemReduceTiny extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32,32)

    Accel {
      val accum = SRAM[Int](32, 32)
      MemReduce(accum)(0 until 32) { i =>
        val inner = SRAM[Int](32, 32)
        Foreach(0 until 32, 0 until 32) { (j, k) => inner(j, k) = j + k }
        inner
      } { (a, b) => a + b }

      dram store accum
    }

    val result = getMatrix(dram)
    val golden = Matrix.tabulate(32,32){(j,k) => (j + k)*32 }
    printMatrix(golden, "Wanted:")
    printMatrix(result, "Got:")
    val cksum = result == golden
    println("PASS: " + cksum + " (MemReduceTiny)")
    assert(cksum)
  }
}
