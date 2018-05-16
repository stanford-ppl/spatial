package spatial.tests.feature.vectors

import spatial.dsl._



@test class ShiftTest extends SpatialTest {
  override def runtimeArgs: Args = "-14 2"

   def main(args: Array[String]): Unit = {
    val x = ArgIn[Int]
    val m = ArgIn[Int]
    val t1 = ArgOut[Int]
    val t3 = ArgOut[Int]
    val t4 = ArgOut[Int]
    setArg(x, args(0).to[Int])
    setArg(m, args(1).to[Int])
    Accel {
      val lsh = x << m
      val rsh = x >> m
      val ursh = x >>> m
      t1 := lsh
      t3 := rsh
      t4 := ursh
      assert(lsh == -48, "lsh: " + lsh + ", expected: -48")
      assert(rsh == -3, "rsh: " + rsh + ", expected: -3")
      assert(ursh == 1073741821, "ursh: " + ursh + ", expected: 1073741821")
    }

    println(r"test1: ${getArg(t1)} =?= -48")
    println(r"test3: ${getArg(t3)} =?= -3")
    println(r"test4: ${getArg(t4)} =?= 1073741821")
  }
}