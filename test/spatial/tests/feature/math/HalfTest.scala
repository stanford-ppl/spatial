package spatial.tests.feature.math

import spatial.dsl._

@spatial class HalfTest extends SpatialTest {
  override def runtimeArgs: Args = "true" and "false"

  def main(args: Array[String]): Unit = {
    val b = args(0).to[Bit]
    val bIn = ArgIn[Bit]
    val hOut = ArgOut[Half]
    setArg(bIn, b)

    Accel {
      val b = bIn.value
      val fancyMux = mux(b, 1.to[Half], 0.to[Half])
      hOut := fancyMux
    }

    val out = getArg(hOut)
    println("out: " + out)
    if (b) assert(out == 1) else assert(out == 0)
  }
}

