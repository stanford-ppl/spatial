package spatial.tests.feature.control


import spatial.dsl._


@test class MemReduceTiny extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    Accel {
      val accum = SRAM[Int](32, 32)
      MemReduce(accum)(0 until 32) { i =>
        val inner = SRAM[Int](32, 32)
        Foreach(0 until 32, 0 until 32) { (j, k) => inner(j, k) = j + k }
        inner
      } { (a, b) => a + b }

      println(accum(0, 0))
    }
  }
}