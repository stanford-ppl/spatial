import spatial.dsl._

@spatial object LUTFloatTestFloatStore extends SpatialApp {
  def main(args: Array[String]): Unit = {
    type F = Float
    type T = Int
    val l: scala.Int = 64
    val inData: Array[I32] = Array.tabulate(l)(i => i.to[I32])
    val inDRAM: DRAM1[I32] = DRAM[I32](l)
    setMem(inDRAM, inData)

    val outDRAM: DRAM1[T] = DRAM[T](l)

    Accel {
      val inMem: SRAM1[I32] = SRAM[I32](l)
      val outMem: SRAM1[Float] = SRAM[Float](l)
      val outMem1: SRAM1[T] = SRAM[T](l)
      inMem load inDRAM
      val lut = LUT[F](l)(Seq.tabulate[F](l)(i => (i + 0.031).to[F]): _*)
      Foreach(l by 1) { idx =>
        outMem(idx) = lut(inMem(idx))
      }

      Foreach(l by 1) { idx =>
        outMem1(idx) = outMem(idx).to[T]
      }

      outDRAM store outMem1
    }

    printArray(getMem(outDRAM))
  }
}
