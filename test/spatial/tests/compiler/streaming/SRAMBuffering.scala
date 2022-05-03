package spatial.tests.compiler.streaming
import spatial.dsl._

@spatial class SRAMBufferingSimple extends SpatialTest {
  val outerIters = 16
  val innerIters = 4

  override def main(args: Array[String]) = {
    implicitly[argon.State].config.stop = 49
    val output = DRAM[I32](outerIters, innerIters)
    Accel {
      val outputSR = SRAM[I32](outerIters, innerIters)
      Foreach(0 until outerIters by 1) {
        outer =>
          val sr = SRAM[I32](innerIters)
          'Producer.Foreach(0 until innerIters by 1) {
            inner =>
              sr(inner) = inner + outer
          }
          'Consumer.Foreach(0 until innerIters by 1) {
            inner =>
              outputSR(outer, inner) = sr(inner)
          }
      }
      output store outputSR
    }
    printMatrix(getMatrix(output))
    assert(Bit(true))
  }
}
