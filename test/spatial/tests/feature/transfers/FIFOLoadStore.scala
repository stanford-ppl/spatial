package spatial.tests.feature.transfers


import spatial.dsl._


@spatial class FIFOLoadStore extends SpatialTest {
  override def dseModelArgs: Args = "192"
  override def finalModelArgs: Args = "192"
  override def runtimeArgs: Args = "192"


  def fifoLoad[T:Num](srcHost: Array[T], N: Int): Array[T] = {
    val tileSize = 16 (64 -> 64)

    val size = ArgIn[Int]
    setArg(size, N)

    val srcFPGA = DRAM[T](size)
    val dstFPGA = DRAM[T](size)
    setMem(srcFPGA, srcHost)

    Accel {
      val f1 = FIFO[T](tileSize)
      Sequential.Foreach(size by tileSize) { i =>
        f1 load srcFPGA(i::i + tileSize par 1)
        dstFPGA(i::i + tileSize par 1) store f1
      }
      ()
    }
    getMem(dstFPGA)
  }


  def main(args: Array[String]): Unit = {
    val arraySize = args(0).to[Int]

    val src = Array.tabulate(arraySize){i => i % 256}
    val dst = fifoLoad(src, arraySize)

    val gold = src

    println("Sent in: ")
    (0 until arraySize) foreach { i => print(gold(i) + " ") }
    println("\nGot out:")
    (0 until arraySize) foreach { i => print(dst(i) + " ") }
    println("")

    val cksum = dst.zip(gold){_ == _}.reduce{_&&_}
    println("PASS: " + cksum + " (FifoLoad)")
    assert(cksum)
  }
}
