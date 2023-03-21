package spatial.tests.compiler.streaming

import spatial.dsl._
@spatial class ForeachReduce extends SpatialTest {

  override def compileArgs = "--nostreamify"

  type T = Fix[TRUE, _32, _0]
  val numReductions = 8
  val reductionSize = 8
  val reductionPar = 4
  val accumSize = 4
  val reduceOp = (a: T, b: T) => a + b
  override def main(args: Array[String]): Unit = {
    val outputDRAM = DRAM[T](numReductions)
    Accel {
      val inputData = FIFO[T](2*reductionPar)
      val outputFIFO = FIFO[T](numReductions)


      Stream {
        Foreach(0 until numReductions, 0 until reductionSize, 0 until reductionPar) { (i, j, k) =>
          inputData.enq(i + j + k)
        }

        val accum = RegFile[T](accumSize, Seq.fill(accumSize){I32(0)})
        Pipe.II(1).Foreach(0 until numReductions, 0 until reductionSize / accumSize, 0 until accumSize) { (i, j, parity) =>
          // Reads from accum(i), writes to accum(i+1)
          val thisCycle = inputData.deqVec(reductionPar)
          val reduced = thisCycle.reduce(reduceOp)
          val oldValue = mux(j === 0, Bits[T](0), accum(parity))
          val result = reduceOp(reduced, oldValue)
          accum(parity) = result

          val isLastIter = (j === (reductionSize / accumSize - 1)) & (parity === (accumSize - 1))
          // If we're on the last iter:
          val outputVal = (Seq.tabulate(accumSize - 1) {r => accum(r)} ++ Seq(result)) reduceTree reduceOp
          outputFIFO.enq(outputVal, isLastIter)
        }
      }
      outputDRAM store outputFIFO
    }
    printArray(getMem(outputDRAM))
    assert(Bit(true))
  }
}
