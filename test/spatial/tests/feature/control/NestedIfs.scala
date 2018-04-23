package spatial.tests.feature.control


import spatial.dsl._


@test class NestedIfs extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def nestedIfTest(x: Int): Int = {
    val in = ArgIn[Int]
    val out = ArgOut[Int]
    setArg(in, x)
    Accel {
      val sram = SRAM[Int](3)
      if (in >= 42.to[Int]) {     // if (43 >= 42)
        if (in <= 43.to[Int]) {   // if (43 <= 43)
          sram(in - 41.to[Int]) = 10.to[Int] // sram(2) = 10
        }
      }
      else {
        if (in <= 2.to[Int]){
          sram(in) = 20.to[Int]
        }
      }
      out := sram(2)
    }
    getArg(out)
  }


  def main(args: Array[String]): Unit = {
    val result = nestedIfTest(43)
    println("result:   " + result)
    assert(result == 10.to[Int])
  }
}
