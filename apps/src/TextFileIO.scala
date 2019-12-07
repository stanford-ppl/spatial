import spatial.dsl._

// PASSED
@spatial object TextFileIO extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val dataFname = "../../data/source.txt"

    val dramSrc = DRAM[Char](128)
    loadDRAMWithASCIIText[Char](dataFname, dramSrc)
    val dramOut = DRAM[Char](128.to[I32]) // Only load 32 chars for now and see what they look like...
    setMem(dramOut, Array.tabulate[Char](128)(i => i.to[Char]))

    Accel {
      val mem = SRAM[Char](128.to[I32])
      mem load dramSrc(0::128)
      dramOut(0::128) store mem
    }

    printArray(getMem(dramOut))
  }
}
