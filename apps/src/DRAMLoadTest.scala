import spatial.dsl._

@spatial object DRAMLoadTest extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val len = 32;
    val memLen = 16;
    val inData = Array.tabulate[Int](len) { i =>
      i.to[Int]
    }
    val inDRAM: DRAM1[Int] = DRAM[Int](len)
    val result = ArgOut[Int]
    setMem(inDRAM, inData)

    Accel {
      val mem = SRAM[Int](memLen)
      result := Sequential
        .Reduce(Reg[Int])(len by memLen) { i =>
          mem load inDRAM(i :: i + memLen par 2)
          Reduce(Reg[Int])(memLen by 1) { j =>
            mem(j)
          } {
            _ + _
          }.value

        } {
          _ + _
        }
        .value

    }

    val hostResult = getArg(result)
    println(hostResult, "result = ")
  }
}
