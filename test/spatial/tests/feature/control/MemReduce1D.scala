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

@test class PartialMemReduce1D extends SpatialTest { // Regression (Unit) // Args: 1920
  override def runtimeArgs: Args = "1920"

  val tileSize = 64
  val p = 1

  def blockreduce_1d[T:Num](src: Array[T], size: Int) = {
    val sizeIn = ArgIn[Int]
    setArg(sizeIn, size)

    val srcFPGA = DRAM[T](sizeIn)
    val dstFPGA = DRAM[T](tileSize)
    val dstFPGA_partial = DRAM[T](tileSize*2)

    setMem(srcFPGA, src)

    Accel {
      val accum = SRAM[T](tileSize)
      MemReduce(accum)(sizeIn by tileSize par p){ i  =>
        val tile = SRAM[T](tileSize)
        tile load srcFPGA(i::i+tileSize par 16)
        tile
      }{_+_}
      dstFPGA(0::tileSize par 16) store accum

      val accum_partial = SRAM[T](tileSize*2)
      MemReduce(accum_partial(tileSize::tileSize*2))(4 by 1){i =>
        val tile_partial = SRAM[T](tileSize*2)
        Foreach(tileSize by 1){j => tile_partial(tileSize + j) = i.to[T]}
        tile_partial(tileSize::tileSize*2)
      }{_+_}
      dstFPGA_partial store accum_partial
    }
    (getMem(dstFPGA), getMem(dstFPGA_partial))
  }

  def main(args: Array[String]): Void = {
    val size = args(0).to[Int]
    val src = Array.tabulate(size){i => i % 256}

    val (dst, partial) = blockreduce_1d(src, size)

    val tsArr = Array.tabulate(tileSize){i => i % 256}
    val perArr = Array.tabulate(size/tileSize){i => i}
    val gold = tsArr.map{ i => perArr.map{j => src(i+j*tileSize)}.reduce{_+_}}

    val partial_gold = Array.tabulate(tileSize*2){i => if (i < tileSize) 0.to[Int] else 6.to[Int]}
    val partial_result = partial 

    val partial_gold_check = Array.fill(tileSize)(6.to[Int])
    val partial_result_check = Array.tabulate(tileSize){i => partial(tileSize + i)}


    printArray(gold, "gold:")
    printArray(dst, "result:")
    printArray(partial_gold, "partial gold:")
    printArray(partial_result, "partial result:")
    val cksum = dst.zip(gold){_ == _}.reduce{_&&_} && partial_gold_check.zip(partial_result_check){_==_}.reduce{_&&_}
    assert(cksum)

    println("PASS: " + cksum + " (BlockReduce1D)")

    //    (0 until tileSize) foreach { i => assert(dst(i) == gold(i)) }
  }
}

