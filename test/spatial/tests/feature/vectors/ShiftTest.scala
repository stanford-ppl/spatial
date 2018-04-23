package spatial.tests.feature.vectors

import spatial.dsl._



@test class ShiftTest extends SpatialTest {
  override def runtimeArgs: Args = "-14 2"

   def main(args: Array[String]): Unit = {
    val x = ArgIn[Int]
    val m = ArgIn[Int]
    setArg(x, args(0).to[Int])
    setArg(m, args(1).to[Int])
    Accel {
      val lsh = x << m
      val rsh = x >> m
      val ursh = x >>> m
      assert(lsh == -56, "lsh: " + lsh + ", expected: -56")
      assert(rsh == -4, "rsh: " + rsh + ", expected: -3")
      assert(ursh == 1073741820, "ursh: " + ursh + ", expected: 1073741820")
    }
  }
}