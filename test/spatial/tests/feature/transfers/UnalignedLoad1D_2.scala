package spatial.tests.feature.transfers


import spatial.dsl._



@test class UnalignedLoad1D_2 extends SpatialTest {
  override def runtimeArgs: Args = "100"

  val N = 19200

  val numCols = 8
  val paddedCols = 1920

  def unaligned_1d[T:Num](src: Array[T], ii: Int): T = {
    val iters = ArgIn[Int]
    val srcFPGA = DRAM[T](paddedCols)
    val acc = ArgOut[T]

    setArg(iters, ii)
    setMem(srcFPGA, src)

    Accel {
      val mem = SRAM[T](96)
      val accum = Reg[T](0.to[T])
      Reduce(accum)(iters by 1) { k =>
        mem load srcFPGA(k*numCols::k*numCols+numCols)
        Reduce(Reg[T](0.to[T]))(numCols by 1){i => mem(i) }{_+_}
      }{_+_}
      acc := accum
    }
    getArg(acc)
  }


  def main(args: Array[String]): Unit = {
    val ii = args(0).to[Int]
    val size = paddedCols
    val src = Array.tabulate(size) {i => i }

    val dst = unaligned_1d(src, ii)

    val gold = Array.tabulate(ii*numCols){ i => i }.reduce{_+_}

    println("src:" + gold)
    println("dst:" + dst)
    val cksum = gold == dst
    println("PASS: " + cksum + " (UnalignedLd)")
    assert(cksum)
  }
}