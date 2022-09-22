package spatial.tests.compiler.streaming

import spatial.dsl._

/**
  * This test runs a pipeline consisting of a deep two-iter pipeline, and a small reduction at the end.
  */
@spatial class HierarchicalSlowdown extends SpatialTest {

  val N = 64
  val inner = 4

  override def main(args: Array[String]) = {
    implicitly[argon.State].config.setV(3)
    val output = DRAM[I32](N)
    Accel {
      val outputSR = SRAM[I32](N)
      Foreach(0 until N) {
        i =>
          // Deep pipeline
          val smallSRAM = SRAM[I32](inner)
          Foreach(inner by 1) {
            j =>
              smallSRAM(j) = HierarchicalSlowdown.deepPipeline(j, 10)
          }
          val v = Reduce(Reg[I32])(0 until inner) {
            j => smallSRAM(j)
          } { _ + _ }
          outputSR(i) = v.value
      }
      output store outputSR
    }
    printArray(getArray(output))
    assert(Bit(true))
  }
}

class HierarchicalSlowdownNS extends HierarchicalSlowdown {
  override def compileArgs = "--nostreamify"
}

object HierarchicalSlowdown {
  @forge.tags.stateful def deepPipeline(input: I32, copies: scala.Int) = {
    var current: I32 = input
    for (i <- 1 to copies) {
      current = ((current + I32(i)) * I32(i) - I32(i)) / I32(i)
    }
    current
  }
}