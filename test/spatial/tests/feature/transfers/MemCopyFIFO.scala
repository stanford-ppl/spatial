package spatial.tests.feature.transfers


import spatial.dsl._


@test class MemCopyFIFO extends SpatialTest {
  def runtimeArgs: Args = NoArgs

  val N = 32

  def fifoLoadStore[T:Bits](srcHost: Array[T]): Array[T] = {
    val tileSize = N

    val srcFPGA = DRAM[T](N)
    val dstFPGA = DRAM[T](N)
    setMem(srcFPGA, srcHost)

    Accel {
      val f1 = FIFO[T](tileSize)
      f1 load srcFPGA(0::tileSize par 16)
      dstFPGA(0::tileSize par 16) store f1
    }
    getMem(dstFPGA)
  }


  def main(args: Array[String]): Unit = {
    val arraySize = N

    val src = Array.tabulate(arraySize) { i => i % 256 }
    val dst = fifoLoadStore(src)

    val gold = src

    println("gold:")
    (0 until arraySize) foreach { i => print(gold(i) + " ") }
    println("")
    println("dst:")
    (0 until arraySize) foreach { i => print(dst(i) + " ") }
    println("")

    val cksum = dst.zip(gold){_ == _}.reduce{_&&_}
    println("PASS: " + cksum + " (FifoLoadStore)")
    assert(cksum)
  }
}
