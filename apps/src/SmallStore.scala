import spatial.dsl._

@spatial object SmallStore {

  def main(args: Array[String]): Unit = {
    val data = Array.tabulate(32){i => i }
    val dram = DRAM[Int](32)
    setMem(dram, data)

    Accel {
      val sram = SRAM[Int](32)
      dram store sram
    }

    val out = getMem(dram)
    printArray(out, "out")
  }

}
