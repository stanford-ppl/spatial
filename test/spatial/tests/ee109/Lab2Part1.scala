package spatial.tests.ee109

import spatial.dsl._

@spatial object Lab2Part1SimpleMemReduce extends SpatialApp {

  val N = 16.to[Int]

  def main(args: Array[String]): Unit = {
    val out = DRAM[Int](16)
    Accel {
      val a = SRAM[Int](16)
      MemReduce(a)(-5 until 5 by 1){i =>
        val tmp = SRAM[Int](16)
        Foreach(16 by 1) { j => tmp(j) = 1}
        tmp
      }{_+_}
      out store a
    }

    val result = getMem(out)
    val gold = Array.tabulate(16){i => 10.to[Int]}
    printArray(gold, "expected: ")
    printArray(result, "result:   ")

    val cksum = gold.zip(result){_==_}.reduce{_&&_}
    println("PASS: " + cksum + " (Lab2Part1SimpleMemReduce)")
  }
}