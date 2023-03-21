package spatial.tests.lib

import spatial.dsl._
import spatial.lib.{StreamedReduction => sreduction}
@spatial class StreamedReduction extends SpatialTest {
  override def compileArgs = "--nostreamify"

  val accumSize = 4
  val reductionIters = 512
  val reductionPar = 8
  override def main(args: Array[String]): Unit = {
    val output = ArgOut[I32]
    Accel {
      Stream {
        val inputFIFO = FIFO[I32](16)
        val outputFIFO = FIFO[I32](1)

        Foreach(0 until reductionIters * reductionPar par 16) {
          i =>
            inputFIFO.enq(i)
        }

        sreduction(inputFIFO, outputFIFO, accumSize, (a: I32, b: I32) => a + b, reductionPar, reductionIters)

        Pipe {
          output := outputFIFO.deq()
        }
      }
    }

    println(r"Sum from 0 to 511 is ${output.value}")
    assert(Bit(true))
  }
}
