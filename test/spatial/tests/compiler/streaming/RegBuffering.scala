package spatial.tests.compiler.streaming

import spatial.dsl._

@spatial class RegBuffering extends SpatialTest {
  val outerIters = 16
  val innerIters = 4

  override def compileArgs = "--max_cycles=1000"

  override def main(args: Array[String]) = {
    val output = ArgOut[I32]
    Accel {
      val reg = Reg[I32](0)
      reg.buffer
      Foreach(0 until 1) {
        i =>
          reg := 3
      }
      Pipe.Foreach(0 until outerIters by 1) {
        outer =>
          'Producer.Foreach(0 until innerIters) {
            i =>
              reg := reg + i * outer
          }
          'Consumer.Foreach(0 until innerIters) {
            inner =>
              output.write(reg.value + inner, inner == 0)
              reg := 0
          }
      }
    }
    assert(output.value == 90, r"Expected 90, received ${output.value}")
  }
}

class RegBufferingNoStream extends RegBuffering {
  override def compileArgs = "--nostreamify"
}
