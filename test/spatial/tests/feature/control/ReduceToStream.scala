package spatial.tests.feature.control

import spatial.dsl._

class ReduceToStream(iters: scala.Int, innerIters: scala.Int) extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val dram = DRAM[I32](iters, innerIters)
    Accel {
      val sram = SRAM[I32](iters, innerIters)
      Foreach(0 until iters) {
        i =>
          val intermediateSram = SRAM[I32](innerIters)
          Foreach(innerIters by 1) {
            inner =>
              intermediateSram(inner) = inner * inner
          }

          val v = Reduce(Reg[I32])(innerIters by 1 par innerIters) {
            inner =>
              intermediateSram(inner)
          } {_ + _}

          // v = sum of squares up to innerIters
          Foreach(innerIters by 1) {
            it =>
              sram(i, it) = (v + it) * i
          }
      }
      dram store sram
    }
    val mat = getMatrix(dram)
    printMatrix(mat)
    assert(Bit(true))
  }
}

class ReduceToStreamBasic extends ReduceToStream(16, 16)
class ReduceToStreamStreamed extends ReduceToStreamBasic with Streamified