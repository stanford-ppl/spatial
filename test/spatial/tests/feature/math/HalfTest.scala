package spatial.tests.feature.math


import spatial.dsl._


@test class HalfTest extends SpatialTest {
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

    println("out: " + getArg(hOut))
  }
}

