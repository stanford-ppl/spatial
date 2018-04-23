package spatial.tests.feature.unit


import spatial.dsl._


@test class OOBTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val y = DRAM[Int](13)
    Accel {
      val x = SRAM[Int](13)
      x load y(0::13)
      Foreach(15 by 1){i => x(i) = i }
      y store x
    }
    val result = getMem(y)
    printArray(result, "y")
    val gold = Array.tabulate(13){i => i}
    assert(gold == result)
  }
}
