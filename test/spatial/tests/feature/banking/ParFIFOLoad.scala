package spatial.tests.feature.banking


import spatial.dsl._


@spatial class ParFIFOLoad extends SpatialTest {
  override def dseModelArgs: Args = "192"
  override def finalModelArgs: Args = "192"
  override def runtimeArgs: Args = "192"

  override def compileArgs: Args = "--scalaExec --scalaSimAccess=1"

  def parFifoLoad[T:Num](src1: Array[T], src2: Array[T], in: Int) = {

    val tileSize = 96 (96 -> 96)

    val N = ArgIn[Int]
    setArg(N, in)

    val src1FPGA = DRAM[T](N)
    val src2FPGA = DRAM[T](N)
    val out = ArgOut[T]
    setMem(src1FPGA, src1)
    setMem(src2FPGA, src2)

    Accel {
      val f1 = FIFO[T](tileSize)
      val f2 = FIFO[T](tileSize)
      Foreach(N by tileSize) { i =>
        Parallel {
          f1 load src1FPGA(i::i+tileSize)
          f2 load src2FPGA(i::i+tileSize)
        }
        val accum = Reduce(Reg[T](0.to[T]))(tileSize by 1){i =>
          f1.deq() * f2.deq()
        }{_+_}
        Pipe { out := accum }
      }
      ()
    }
    getArg(out)
  }


  def main(args: Array[String]): Unit = {
    val arraySize = args(0).to[Int]

    val src1 = Array.tabulate(arraySize) { i => i }
    val src2 = Array.tabulate(arraySize) { i => i*2 }
    val out = parFifoLoad(src1, src2, arraySize)

    val sub1_for_check = Array.tabulate(arraySize-96) {i => i}
    val sub2_for_check = Array.tabulate(arraySize-96) {i => i*2}
    val sub = if (arraySize <= 96) 0 else sub1_for_check.zip(sub2_for_check){_*_}.reduce(_+_)

    // val gold = src1.zip(src2){_*_}.zipWithIndex.filter( (a:Int, i:Int) => i > arraySize-96).reduce{_+_}
    val gold = src1.zip(src2){_*_}.reduce{_+_} - sub
    println("gold = " + gold)
    println("out = " + out)

    val cksum = out == gold
    assert(cksum)
  }
}
