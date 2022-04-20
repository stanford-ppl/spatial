package spatial.tests.compiler.streaming

import spatial.dsl._

@spatial class RegBuffering extends SpatialTest {
  val outerIters = 16
  val innerIters = 4

  override def main(args: Array[String]) = {
    implicitly[argon.State].config.stop = 43
    val output = DRAM[I32](outerIters, innerIters)
    Accel {
      val outputSR = SRAM[I32](outerIters, innerIters)
      val reg = Reg[I32](0)
      reg.buffer
      reg := 3 //x4 (wr)
      Pipe.Foreach(0 until outerIters by 1) {
        outer =>
          'Producer.Sequential.Foreach(0 until innerIters) {
            i =>
              reg := reg + i * outer // x14 (wr), x11(rd)
          }
          'Consumer.Sequential.Foreach(0 until innerIters) {
            inner =>
              outputSR(outer, inner) = reg.value + inner // x19 (rd)
              reg := 0 // x22 (wr)
          }
      }
      output store outputSR
    }
    printMatrix(getMatrix(output))
    assert(Bit(true))
  }
}
