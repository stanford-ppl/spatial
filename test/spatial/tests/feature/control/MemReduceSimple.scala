package spatial.tests.feature.control


import spatial.dsl._


@test class MemReduceSimple extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  lazy val N = 16.to[Int]


  def main(args: Array[String]): Unit = {

    val out = DRAM[Int](16)
    val out2 = DRAM[Int](16)

    Accel {
      val a = SRAM[Int](16)
      MemReduce(a)(-5 until 0 by 1){i =>
        val tmp = SRAM[Int](16)
        Foreach(16 by 1) { j => tmp(j) = 1}
        tmp
      }{_+_}
      val b = SRAM[Int](16)
      Foreach(15 until -1 by -1){i => b(i) = 2}
      out store a
      out2 store b
    }
    val result = getMem(out)
    val result2 = getMem(out2)

    val gold = Array.tabulate(16){i => 5.to[Int]}
    val gold2 = Array.tabulate(16){i => 2.to[Int]}
    printArray(gold, "expected: ")
    printArray(result, "result:   ")
    printArray(gold2, "expected: ")
    printArray(result2, "result:   ")
    assert(gold == result, "gold did not match result")
    assert(gold2 == result2, "gold2 did not match result2")
  }
}
