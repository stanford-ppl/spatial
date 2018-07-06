package spatial.tests.feature.memories.sram

import spatial.dsl._

@spatial class SRAMMaskedWrite extends SpatialTest {
  override def runtimeArgs: Args = "2"
  type T = Int

  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val N = 128
    val a = args(0).to[T]
    val y = DRAM[T](N)
    val s = ArgIn[T]

    setArg(s, a)

    Accel {
      val yy = SRAM[T](N)
      Foreach(2*N by 1) { i =>
        if (i < s.value) { yy(i) = 1.to[T]}
      }
      Foreach(2*N by 1) { i =>
        if ((i >= s.value) && (i < N)) {yy(i) = 2.to[T] }
      }
      y(0 :: N par 1) store yy
    }


    // Extract results from accelerator
    val result = getMem(y)

    // Create validation checks and debug code
    val gold = Array.tabulate(N){i => if (i < a) {1} else {2}}
    printArray(gold, "expected: ")
    printArray(result, "got: ")

    val cksum = gold.zip(result){_ == _}.reduce{_&&_}
    println("PASS: " + cksum + " (MaskedWrite)")
    assert(cksum)
  }
}
