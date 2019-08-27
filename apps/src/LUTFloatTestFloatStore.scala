import spatial.dsl._

@spatial object LUTFloatTestFloatStore extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val varOffset: scala.Double = 0.031
    type F = Float
    type T = Int
    val l: scala.Int = 64
    val inData: Array[I32] = Array.tabulate(l)(i => i.to[I32])
    val inDRAM: DRAM1[I32] = DRAM[I32](l)
    setMem(inDRAM, inData)

    val outDRAM: DRAM1[F] = DRAM[F](l)

    Accel {
      val inMem: SRAM1[I32] = SRAM[I32](l)
      val outMem: SRAM1[Float] = SRAM[Float](l)
      val outMem1: SRAM1[F] = SRAM[F](l)
      inMem load inDRAM
      val lut = LUT[F](l)(Seq.tabulate[F](l)(i => (i + varOffset).to[F]): _*)
      Foreach(l by 1) { idx =>
        outMem(idx) = lut(inMem(idx))
      }

      Foreach(l by 1) { idx =>
        outMem1(idx) = outMem(idx)
      }

      outDRAM store outMem1
    }

    val accelArray: Array[Float] = getMem(outDRAM)
    val goldArray: Array[Float] = inData.map(i => i.to[Float] + varOffset.to[Float])
    val cksum: Boolean = Array
      .tabulate[Float](goldArray.length)(i => goldArray(i) - accelArray(i))
      .reduce { _ + _ } == 0.to[Float]
    println("LUTFloatTestFloatStore: cksum = " + cksum)
  }
}
