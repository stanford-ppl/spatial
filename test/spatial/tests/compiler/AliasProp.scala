package spatial.tests.compiler

import spatial.dsl._

@spatial class AliasProp extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32)
    Accel { }

    val golden = Array.tabulate(32){i => 0.to[Int] }
    val result = getMem(dram)
    val result2 = Array.tabulate(32){i => result(i) }

    printArray(result)
    printArray(result2)
    assert(result2 == golden)
  }

}
