package spatial.tests.feature.memories.fifo

import spatial.dsl._



@spatial class FIFOLoad extends SpatialTest {
  override def runtimeArgs: Args = "960"


  def fifoLoad[T:Num](srcHost: Array[T], N: Int): Array[T] = {
    val tileSize = 96 (96 -> 96)

    val size = ArgIn[Int]
    setArg(size, N)

    val srcFPGA = DRAM[T](size)
    val dstFPGA = DRAM[T](size)
    setMem(srcFPGA, srcHost)

    Accel {
      val f1 = FIFO[T](tileSize)
      Sequential.Foreach(size by tileSize) { i =>
        f1 load srcFPGA(i::i + tileSize)
        val b1 = SRAM[T](tileSize)
        Foreach(tileSize by 1) { i =>
          b1(i) = f1.deq()
        }
        dstFPGA(i::i + tileSize) store b1
      }
      ()
    }
    getMem(dstFPGA)
  }


  def main(args: Array[String]): Unit = {
    val arraySize = args(0).to[Int]

    val src = Array.tabulate(arraySize){i => i }
    val dst = fifoLoad(src, arraySize)

    val gold = src

    println("Sent in: ")
    (0 until arraySize) foreach { i => print(gold(i) + " ") }
    println("Got out:")
    (0 until arraySize) foreach { i => print(dst(i) + " ") }
    println("")

    val cksum = dst.zip(gold){_ == _}.reduce{_&&_}
    println("PASS: " + cksum + " (FifoLoadTest)")
    assert(cksum)
  }
}
