package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class LoadOnlyTest extends SpatialTest {
  lazy val N = 16.to[Int]

  def main(args: Array[String]): Unit = {

    val in = DRAM[Int](32)
    val out = List.tabulate(32){_ => ArgOut[Int]}

    setMem(in, Array.tabulate(32){i => i})

    Accel {
      val a = SRAM[Int](32)
      a load in
      out.zipWithIndex.foreach{case (o,i) => o := a(i)}
    }
    out.zipWithIndex.foreach{case (o,i) =>
      println(r"got ${getArg(o)}, wanted ${i-1}")
      assert(getArg(o) == i-1)
    }
  }
}


@spatial class StoreOnlyTest extends SpatialTest {
  lazy val N = 32.to[Int]

  def main(args: Array[String]): Unit = {

    val out = DRAM[Int](32)

    Accel {
      val a = SRAM[Int](32)
      Foreach(32 by 1){i => a(i) = i}
      out store a
    }
    val gold = Array.tabulate(32){i => i}
    printArray(getMem(out), "Got")
    assert(gold == getMem(out))
  }
}
