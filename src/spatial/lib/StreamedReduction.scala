package spatial.lib

import spatial.lang._
import spatial.node._
import argon._

import spatial.metadata.control._
import spatial.metadata.memory._
object StreamedReduction {
  /**
    * Computes a streaming reduction
    * @param inputFIFO FIFO containing the inputs. Should be sized at LEAST reductionPar
    * @param outputFIFO FIFO containing outputs.
    * @param accumSize Size of the accumulator -- This should be set to be longer than the latency of the reduction tree.
    * @param reducer The reduction function
    * @param reductionPar Number of values to read from inputFIFO at a time
    * @param reductionIterations Number of iterations to perform -- total number of elements reduced is iters * par
    * @tparam T type of input elements
    * @return Streaming Foreach loop performing the reduction
    */
  @forge.tags.stateful def apply[T: Bits](inputFIFO: FIFO[T], outputFIFO: FIFO[T], accumSize: Int, reducer: (T, T) => T, reductionPar: Int, reductionIterations: I32): Void = {
    val initializers = Seq.fill(accumSize) {Bits[T].zero.asInstanceOf[Bits[T]]}
    val accum = RegFile[T](I32(accumSize), initializers)
    accum.explicitName = s"ExplicitAccumulator_${accum}"
    'StreamingReduction.Pipe.II(1).Foreach(0 until reductionIterations by accumSize, 0 until accumSize) { (j, parity) =>
      // Reads from accum(i), writes to accum(i+1)
      val thisCycle = inputFIFO.deqVec(reductionPar)
      val reduced = thisCycle.reduce(reducer)
      val result = mux(j === 0, reduced, reducer(reduced, accum(parity)))
      accum(parity) = result


      val isLastIter = (j + parity) === (reductionIterations - 1)
      // If we're on the last iter:
      val outputVal = Seq.tabulate(accumSize) { r => accum(r) } reduceTree reducer
      outputFIFO.enq(outputVal, isLastIter)
    }
  }
}
