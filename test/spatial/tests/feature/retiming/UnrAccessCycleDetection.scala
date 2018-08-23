package spatial.tests.feature.retiming

import spatial.dsl._

@spatial class UnrAccessCycleDetection extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val x = ArgIn[Int]
    setArg(x, 1)
    val w = DRAM[Int](32)
    Accel {
      // Inspired by SHA1 the last instance of this loop (not nested in an if statement) has incorrect II
      val W = SRAM[Int](32)
      val data = SRAM[Int](16)
      Foreach(16 by 1) { i => data(i) = i+1 }

      if (x.value == 1) {
        'CORRECT.Foreach(32 by 1) { i =>
          W(i) = if (i < 16) {data(i)} else {W(i-3) + W(i-8) + W(i-14) + W(i-16)}
        }
      }

      Foreach(16 by 1) { i => data(i) = i }

      'WRONG.Foreach(32 by 1) { i =>
        W(i) = if (i < 16) {data(i)} else {W(i-3) + W(i-8) + W(i-14) + W(i-16)}
      }

      w store W
    }

    printArray(getMem(w), "W: ")
    val gold = Array[Int](0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,23,27,31,42,49,56,70,80,97,117,133,163,192,217,270,314)
    printArray(gold, "G: ")
    assert(getMem(w) == gold)
  }
}
