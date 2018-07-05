package spatial.tests.feature.transfers


import spatial.dsl._


@spatial class UnalignedMemCopyFIFO extends SpatialTest {
  override def runtimeArgs: Args = "400"

  val tileSize = 20


  def singleFifoLoad[T:Num](src1: Array[T], in: Int): T = {
    val P1 = 1 (16 -> 16)

    val N = ArgIn[Int]
    setArg(N, in)

    val src1FPGA = DRAM[T](N)
    val out = ArgOut[T]
    setMem(src1FPGA, src1)

    Accel {
      val f1 = FIFO[T](3*tileSize)
      Foreach(N by tileSize) { i =>
        f1 load src1FPGA(i::i+tileSize par P1)
        val accum = Reg[T](0.to[T])
        accum.reset
        Reduce(accum)(tileSize by 1 par 1){i =>
          f1.deq()
        }{_+_}
        Pipe { out := accum }
      }
    }
    getArg(out)
  }


  def main(args: Array[String]): Unit = {
    val arraySize = args(0).to[Int]

    val src1 = Array.tabulate(arraySize) { i => i % 256}
    val out = singleFifoLoad(src1, arraySize)

    val sub1_for_check = Array.tabulate(arraySize-tileSize) {i => i % 256}

    val gold = src1.reduce{_+_} - sub1_for_check.reduce(_+_)
    println("gold: " + gold)
    println("out: " + out)
    assert(out == gold)
  }
}