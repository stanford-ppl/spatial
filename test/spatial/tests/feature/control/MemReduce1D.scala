package spatial.tests.feature.control


import spatial.dsl._


@test class MemReduce1D extends SpatialTest {
  override def runtimeArgs: Args = "1920"
  val tileSize = 64
  val p = 1


  def blockreduce_1d[T:Num](src: Array[T], size: Int): Array[T] = {
    val sizeIn = ArgIn[Int]
    setArg(sizeIn, size)

    val srcFPGA = DRAM[T](sizeIn)
    val dstFPGA = DRAM[T](tileSize)

    setMem(srcFPGA, src)

    Accel {
      val accum = SRAM[T](tileSize)
      MemReduce(accum)(sizeIn by tileSize par p){ i  =>
        val tile = SRAM[T](tileSize)
        tile load srcFPGA(i::i+tileSize par 16)
        tile
      }{_+_}
      dstFPGA(0::tileSize par 16) store accum
    }
    getMem(dstFPGA)
  }


  def main(args: Array[String]): Unit = {
    val size = args(0).to[Int]
    val src = Array.tabulate(size){i => i % 256}

    val dst = blockreduce_1d(src, size)

    val tsArr = Array.tabulate(tileSize){i => i % 256}
    val perArr = Array.tabulate(size/tileSize){i => i}
    val gold = tsArr.map{ i => perArr.map{j => src(i+j*tileSize)}.reduce{_+_}}

    printArray(gold, "src:")
    printArray(dst, "dst:")
    assert(gold == dst)
  }
}
