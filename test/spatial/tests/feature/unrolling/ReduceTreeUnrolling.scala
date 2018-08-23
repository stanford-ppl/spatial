package spatial.tests.feature.unrolling

import spatial.dsl._

@spatial class ReduceTreeUnrolling extends SpatialTest{

  def main(args: Array[String]): Unit = {
    val out1 = ArgOut[Int]
    val dram = DRAM[Int](16)
    val data = Array.fill(16){ random[Int] }
    setMem(dram, data)

    Accel {
      val sram = SRAM[Int](16)
      sram load dram
      val sum = Reduce(0)(16 par 4){i => sram(i) }{_+_}
      out1 := sum
    }

    val gold = data.reduce{_+_}
    assert(getArg(out1) == gold, r"$out1 != $gold")
  }
}
