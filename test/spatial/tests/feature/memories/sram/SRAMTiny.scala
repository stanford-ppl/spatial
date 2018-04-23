package spatial.tests.feature.memories.sram


import spatial.dsl._


@test class SRAMTiny extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    Accel {
      val sram = SRAM[Int](1, 16)
      sram(0, 0) = 10
      println(sram(0, 0))
    }
  }
}