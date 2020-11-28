package spatial.tests.feature.control

import spatial.dsl._

// Not a great test or a great fix for this problem, but logging it in the suite anyway
@spatial class RussianNestingStreams extends SpatialTest {
  def main(args: Array[String]): Void = {

    val x = ArgOut[Int]
    Accel {
      val fifo = FIFO[Int](16)

      Stream {
        Stream.Foreach(4 by 1) { i =>
          Stream.Foreach(4 by 1) { j => fifo.enq(j)}
        }

        Foreach(16 by 1){ j => x := fifo.deq()}
      }
    }

    // Report the answer
    println(r"Result is ${getArg(x)}")
    assert(x == 3)
  }
}
