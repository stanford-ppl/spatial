import spatial.dsl._

@spatial object DRAMStoreTest extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val len = 32;
    val memLen = 16;
    val outDRAM = DRAM[Int](len)

    Accel {
      Foreach(len by memLen) { i =>
        val mem = SRAM[Int](memLen)
        Foreach (memLen by 1) { j =>
          mem(j) = i + j
        }

        outDRAM(i :: i + memLen) store mem
      }
    }

    val outData = getMem(outDRAM)
    printArray(outData)
  }
}
