package spatial.tests.feature.dynamic
import spatial.dsl._

@spatial class DynamicLoopbounds extends SpatialTest {
  override def main(args: Array[String]) = {
    val setIters = 15
    val maxIters = 32
    val iterations = ArgIn[I32]
    setArg(iterations, setIters)
    val dram = DRAM[I32](maxIters)
    Accel {
      val out = SRAM[I32](maxIters)
      Foreach(32 by 1) { i => out(i) = -1 }
      Foreach((iterations.value+1) by 1) {
        i => out(i) = i
      }
      dram store out
    }

    val result = getMem(dram)
    Foreach(maxIters by 1) {
      i =>
        val expected = if (i <= setIters) { i } else { -1 }
        assert(result(i) == expected, r"Error on iteration $i")
    }
  }
}
