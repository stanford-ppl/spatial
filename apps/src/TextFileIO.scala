import spatial.dsl._

// PASSED
@spatial object TextFileIO extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val testLen = 128
    val argOut = ArgOut[I32]
    val dataFname = "../../data/source.txt"

    val testTextArray = loadASCIITextFile(dataFname)
    val dramSrc = DRAM[Char](testTextArray.length)
    setMem(dramSrc, testTextArray)
    val dramOut = DRAM[Char](128.to[I32]) // Only load 32 chars for now and see what they look like...

    Accel {
      val mem = SRAM[Char](128.to[I32])
      mem load dramSrc(0::128)
      dramOut(0::128) store mem
    }


    println(argOut.value)
    printArray(getMem(dramOut))
  }
}
