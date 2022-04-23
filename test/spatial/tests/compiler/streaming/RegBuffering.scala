package spatial.tests.compiler.streaming

import spatial.dsl._

@spatial class RegBuffering extends SpatialTest {
  val outerIters = 16
  val innerIters = 4

  override def main(args: Array[String]) = {
    val output = ArgOut[I32]
    Accel {
      val reg = Reg[I32](0)
      reg.buffer
      reg := 3
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
    println(r"Output: ${output.value}")
    assert(Bit(true))
  }
}
