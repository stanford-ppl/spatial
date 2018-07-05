package spatial.tests.feature.transfers


import spatial.dsl._


@spatial class UnalignedLoad1D extends SpatialTest {
  override def runtimeArgs: Args = "100 9"
  val N = 19200

  val paddedCols = 1920


  def unaligned_1d[T:Num](src: Array[T], ii: Int, numCols: Int): T = {
    val iters = ArgIn[Int]
    val srcFPGA = DRAM[T](paddedCols)
    val ldSize = ArgIn[Int]
    val acc = ArgOut[T]

    setArg(iters, ii)
    setArg(ldSize, numCols)
    setMem(srcFPGA, src)

    Accel {
      val ldSizeReg = Reg[Int](0.to[Int])
      ldSizeReg := ldSize.value
      val accum = Reduce(Reg[T](0.to[T]))(iters by 1 par 1) { k =>
        val mem = SRAM[T](16)
        mem load srcFPGA(k*ldSizeReg.value::(k+1)*ldSizeReg.value)
        Reduce(Reg[T](0.to[T]))(ldSizeReg.value by 1){i => mem(i) }{_+_}
      }{_+_}
      acc := accum
    }
    getArg(acc)
  }


  def main(args: Array[String]): Unit = {
    type T = FixPt[TRUE,_32,_32]
    val ii = args(0).to[Int]
    val cols = args(1).to[Int]
    val size = paddedCols
    val src = Array.tabulate[T](size) {i => (i % 256).to[T] }

    val dst = unaligned_1d(src, ii, cols)

    val goldArray = Array.tabulate[T](ii*cols){ i => (i % 256).to[T] }
    val gold = goldArray.reduce{_+_}

    printArray(src, "src")
    printArray(goldArray, "gold")

    println("src:" + gold)
    println("dst:" + dst)
    val cksum = gold == dst
    assert(cksum)
  }
}