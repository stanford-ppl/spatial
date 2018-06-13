package spatial.tests.feature.transfers

import spatial.dsl._

@test class PageBoundaryStraddle extends SpatialTest { // Regression (Unit) // Args: none
  override def runtimeArgs: Args = "864"


  def main(args: Array[String]): Void = {
    type T = FixPt[TRUE,_32,_0]
    // val size = args(0).to[Int]
    val tileSize = 48
    val N = ArgIn[Int]
    val size = args(0).to[Int]
    setArg(N, size)
    val offsetter = DRAM[T](480)
    val init = Array.tabulate(N){i => -1.to[T]}
    val ones = Array.tabulate(N){i => 1.to[T]}
    val dram0 = DRAM[T](N)
    val dram1 = DRAM[T](N)
    val dram2 = DRAM[T](N)
    val sum = ArgOut[T]
    // val dram3 = DRAM[T](N)
    // val dram4 = DRAM[T](N)

    setMem(dram0, ones)
    setMem(dram1, init)
    setMem(dram2, init)
    // setMem(dram3, init)
    // setMem(dram4, init)

    Accel{
      val sram0 = SRAM[T](tileSize)
      val sram1 = SRAM[T](tileSize)
      val sram2 = SRAM[T](tileSize)
      // val sram3 = SRAM[T](tileSize)
      // val sram4 = SRAM[T](tileSize)
      sum := Reduce(Reg[T](0))(N by tileSize){j =>
        sram0 load dram0(j::j+tileSize par 16)
        val minisum = Reduce(Reg[T](0))(tileSize by 1){i =>
          sram1(i) = 1
          sram2(i) = 2
          // sram3(i) = 3
          // sram4(i) = 4
          sram0(i)
        }{_+_}
        dram1(j::j+tileSize par 16) store sram1
        dram2(j::j+tileSize par 16) store sram2
        // dram3(j::j+tileSize par 16) store sram3
        // dram4(j::j+tileSize par 16) store sram4
        minisum
      }{_+_}
    }

    val res1 = getMem(dram1)
    val res2 = getMem(dram2)
    val gotsum = getArg(sum)
    // val res3 = getMem(dram3)
    // val res4 = getMem(dram4)

    val cksum0 = gotsum == size.to[T]
    val cksum1 = res1.map{_ == 1.to[T]}.reduce{_&&_}
    val cksum2 = res2.map{_ == 2.to[T]}.reduce{_&&_}
    // val cksum3 = res3.map{_ == 3}.reduce{_&&_}
    // val cksum4 = res4.map{_ == 4}.reduce{_&&_}

    printArray(res1, "res1")
    printArray(res2, "res2")
    println("Load test: " + gotsum + " =?= " + N)
    // printArray(res3, "res3")
    // printArray(res4, "res4")

    println("cksum0: " + cksum0)
    println("cksum1: " + cksum1)
    // println("cksum2: " + cksum2)
    // println("cksum3: " + cksum3)
    // println("cksum4: " + cksum4)


    val cksum = cksum0 && cksum1 && cksum2
    assert(cksum)
    println("PASS: " + cksum + " (PageBoundaryTest)")

    //    (0 until tileSize) foreach { i => assert(dst(i) == gold(i)) }
  }
}
