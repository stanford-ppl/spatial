package spatial.tests.feature.vectors

import spatial.dsl._



@spatial class ShiftTest extends SpatialTest {
  override def runtimeArgs: Args = "-14 2"

   def main(args: Array[String]): Unit = {
    val x = ArgIn[Int]
    val m = ArgIn[Int]
    val t1 = ArgOut[Int]
    val t3 = ArgOut[Int]
    val t4 = ArgOut[Int]
    val neg14 = args(0).to[Int]
    val two = args(1).to[Int]
    setArg(x, neg14)
    setArg(m, two)
    Accel {
      val lsh = x << m.value.to[I16]
      val rsh = x >> m.value.to[I16]
      val ursh = x >>> m.value.to[I16]
      t1 := lsh
      t3 := rsh
      t4 := ursh
      assert(lsh == -56, "lsh: " + lsh + ", expected: -48")
      assert(rsh == -4, "rsh: " + rsh + ", expected: -4")
      assert(ursh == 1073741820, "ursh: " + ursh + ", expected: 1073741820")
    }

    println(r"test1: ${getArg(t1)} =?= -56")
    println(r"test3: ${getArg(t3)} =?= -4")
    println(r"test4: ${getArg(t4)} =?= 1073741820")
    assert(t1 == -56)
    assert(t3 == -4)
    assert(t4 == 1073741820)
  }
}
