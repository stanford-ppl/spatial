package spatial.tests.compiler

import spatial.dsl._

@spatial class MultiControlUse extends SpatialTest {
  // TODO: Complete this test
  override def backends = DISABLED

  def main(args: Array[String]): Unit = {
    val N = 32

    Accel {
      val sram = SRAM[Int](33)

      'PARENT.Foreach(N by 1){i =>
        val max = i + 1
        'CHILD1.Foreach(max by 1) {j => sram(j) = j }
        'CHILD2.Foreach(max by 1) {j => println(sram(j)) }
      }
    }



  }
}
