package spatial.tests.feature.transfers


import spatial.dsl._


@spatial class MemCopySRAM1D extends SpatialTest {
  override def runtimeArgs: Args = "100"

  val N = 192


  def simpleLoadStore[T:Num](srcHost: Array[T], value: T): Array[T] = {
    val loadPar  = 1 (1 -> 1)
    val storePar = 1 (1 -> 1)
    val tileSize = 16 (16 -> 16)

    val srcFPGA = DRAM[T](N)
    val dstFPGA = DRAM[T](N)
    setMem(srcFPGA, srcHost)

    Accel {
      Sequential.Foreach(N by tileSize par 2) { i =>
        val b1 = SRAM[T](tileSize)

        b1 load srcFPGA(i::i+tileSize par 1)
        dstFPGA(i::i+tileSize par 1) store b1
      }
    }
    getMem(dstFPGA)
  }


  def main(args: Array[String]): Unit = {
    val arraySize = N
    val value = args(0).to[Int]

    val src = Array.tabulate[Int](arraySize) { i => i % 256 }
    val dst = simpleLoadStore(src, value)

    printArray(src, "Source")
    printArray(dst, "Dest")

    assert(dst == src)
  }
}
