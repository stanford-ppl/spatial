package spatial.tests.feature.memories.sram

import spatial.dsl._

@spatial class SRAMTiny extends SpatialTest {
  override def backends = super.backends.filterNot{be => (be == VCS_noretime)}

  def main(args: Array[String]): Unit = {
  	val x = ArgOut[Int]
    Accel {
      val sram = SRAM[Int](1, 16)
      sram(0, 0) = 10
      val r = sram(0, 0)
      assert(r == 10)
      x := r
    }
    println(r"Arg ${getArg(x)} =?= 10")
    assert(getArg(x) == 10)
  }
}
