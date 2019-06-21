package spatial.tests.feature.banking


import spatial.dsl._


@spatial class ParFIFOLoad2 extends SpatialTest {
  override def dseModelArgs: Args = "384"
  override def finalModelArgs: Args = "384"
  override def runtimeArgs: Args = "384"

  val tileSize = 64


  def parFifoLoad[T:Num](src1: Array[T], src2: Array[T], src3: Array[T], in: Int): T = {

    val P1 = 1 (16 -> 16)

    val N = ArgIn[Int]
    setArg(N, in)

    val src1FPGA = DRAM[T](N)
    val src2FPGA = DRAM[T](N)
    val src3FPGA = DRAM[T](N)
    val out = ArgOut[T]
    setMem(src1FPGA, src1)
    setMem(src2FPGA, src2)
    setMem(src3FPGA, src3)

    Accel {
      val f1 = FIFO[T](tileSize)
      val f2 = FIFO[T](tileSize)
      val f3 = FIFO[T](tileSize)
      Foreach(N by tileSize) { i =>
        // Parallel {
        f1 load src1FPGA(i::i+tileSize par P1)
        f2 load src2FPGA(i::i+tileSize par P1)
        f3 load src3FPGA(i::i+tileSize par P1)
        // }
        val accum = Reduce(Reg[T](0.to[T]))(tileSize by 1){i =>
          f1.deq() * f2.deq() * f3.deq()
        }{_+_}
        Pipe { out := accum }
      }
      ()
    }
    getArg(out)
  }


  def main(args: Array[String]): Unit = {
    val arraySize = args(0).to[Int]

    val src1 = Array.tabulate(arraySize) { i => i % 4 }
    val src2 = Array.tabulate(arraySize) { i => i % 4 + 16}
    val src3 = Array.tabulate(arraySize) { i => i % 4 + 2*16}
    val out = parFifoLoad(src1, src2, src3, arraySize)

    val sub1_for_check = Array.tabulate(arraySize-tileSize) {i => i % 4}
    val sub2_for_check = Array.tabulate(arraySize-tileSize) {i => i % 4 + 16}
    val sub3_for_check = Array.tabulate(arraySize-tileSize) {i => i % 4 + 2*16}

    val gold = src1.zip(src2){_*_}.zip(src3){_*_}.reduce{_+_} - sub1_for_check.zip(sub2_for_check){_*_}.zip(sub3_for_check){_*_}.reduce(_+_)
    println("gold: " + gold)
    println("out: " + out)
    assert(out == gold)
  }
}

