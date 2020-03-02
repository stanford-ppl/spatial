import spatial.dsl._

@spatial object DRAMLoadStoreTestArbPar extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val len = 64;
    val memLen = 16;
    val inData = Array.tabulate[Int](len){ i => i.to[Int] }
    val inDRAM: DRAM1[Int] = DRAM[Int](len)
    val outDRAM = DRAM[Int](len)
    setMem(inDRAM, inData)

    Accel {
      Foreach(len by memLen par 2) { i =>
        val mem0 = SRAM[Int](memLen)
        val mem1 = SRAM[Int](memLen)

        mem0 load inDRAM(i :: i + memLen par 2)
        Foreach(memLen by 1 par 2) { j =>
          mem1(j) = mem0(j) + 13.to[Int]
        }

        outDRAM(i :: i + memLen par 2) store mem1
      }
    }

    val outData = getMem(outDRAM)
    printArray(outData)
  }
}
