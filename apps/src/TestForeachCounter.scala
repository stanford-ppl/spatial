import spatial.dsl._

@spatial object TestForeachCounter extends SpatialApp {

  def main(args: Array[String]): Unit = {
    // Loop upper bound
    val N = 128

    // The DRAM
    val d = DRAM[Int](N)

    // DRAM content
    val data = Array.fill[Int](N)(0)
    setMem(d, data)

    Accel {
      val s = SRAM[Int](N)

      s load d(0::N)

      Foreach(N by 1) { i => s(i) = s(i) + i }

      d(0::N) store s
    }

    printArray(getMem(d), "Result: ")
  }
}
