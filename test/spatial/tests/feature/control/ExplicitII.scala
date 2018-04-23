package spatial.tests.feature.control


import spatial.dsl._


// Example of user syntax for explicitly setting II of a pipeline
@test class ExplicitII extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val y = ArgIn[Int]

    Accel {
      val x = SRAM[Int](32)
      Pipe.II(1).Foreach(0 until 32) { i =>
        x(i) = (x(i) * 32) / y.value
      }
    }
  }

}