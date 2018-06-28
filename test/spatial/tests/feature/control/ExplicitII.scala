package spatial.tests.feature.control

import spatial.dsl._

// Example of user syntax for explicitly setting II of a pipeline
@spatial class ExplicitII extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val y = ArgIn[Int]
    val z = ArgOut[Int]
    setArg(y, 16)

    Accel {
      val x = SRAM[Int](32)

      Foreach(0 until 32){i => x(i) = i }

      Pipe.II(1).Foreach(0 until 32) { i =>
        x(i) = (x(i) * 32) / y.value
      }

      Reduce(z)(0 until 32){i => x(i) }{_+_}
    }

    assert(getArg(z) == Array.tabulate(32){i => i*2}.reduce{_+_})
  }

}