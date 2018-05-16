package spatial.tests.compiler

import spatial.dsl._

@test class AliasProp extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32)
    Accel { }

    val golden = Array.tabulate(32){i => i }
    val result = getMem(dram)
    val result2 = Array.tabulate(32){i => result(i) }

    printArray(result)
    printArray(result2)
    assert(result2 == golden)
  }

}
