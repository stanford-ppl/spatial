package spatial.tests.compiler

import spatial.dsl._

@spatial class IllegalFIFOParallelization extends SpatialTest {
  override def backends = RequireErrors(1)

  def main(args: Array[String]): Unit = {

    val out = ArgOut[Int]

    Accel {
      val fifo = FIFO[Int](32)

      Foreach(16 par 2) {i =>
        Foreach(8 by 1){j => fifo.enq(j) }
        Foreach(8 by 1){j => out := fifo.deq() }
      }
    }

  }
}
