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
    val t5 = ArgOut[Int]
    val t6 = ArgOut[Int]
    val t7 = ArgOut[Int]
    val t8 = ArgOut[Int]
    val t9 = ArgOut[Int]
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
      assert(lsh == -56, "lsh: " + lsh + ", expected: -56")
      // assert(rsh == -4, "rsh: " + rsh + ", expected: -4")
      // assert(ursh == 1073741820, "ursh: " + ursh + ", expected: 1073741820")

      val lshrsh = (x << 3) >> 2
      val rshlsh = (x >> 3) << 2   // Cannot rewrite
      val lshlsh = (x << 3) << 2
      val rshrsh = (x >> 3) >> 2
      val nosh = (x >> 3) << 3   // Cannot rewrite
      t5 := lshrsh
      t6 := rshlsh
      t7 := lshlsh
      t8 := rshrsh
      t9 := nosh
    }

    println(r"test1: ${getArg(t1)} =?= -56")
    println(r"test3: ${getArg(t3)} =?= -4")
    println(r"test4: ${getArg(t4)} =?= 1073741820")
    println(r"test5: ${getArg(t5)} =?= ${args(0).to[Int] << 1}")
    println(r"test6: ${getArg(t6)} =?= ${(args(0).to[Int] >> 3) << 2}")
    println(r"test7: ${getArg(t7)} =?= ${args(0).to[Int] << 5}")
    println(r"test8: ${getArg(t8)} =?= ${args(0).to[Int] >> 5}")
    println(r"test9: ${getArg(t9)} =?= ${(args(0).to[Int] >> 3) << 3}")
    assert(t1 == -56)
    assert(t3 == -4)
    assert(t4 == 1073741820)
    assert(t5 == args(0).to[Int] << 1)
    assert(t6 == (args(0).to[Int] >> 3) << 2)
    assert(t7 == args(0).to[Int] << 5)
    assert(t8 == args(0).to[Int] >> 5)
    assert(t9 == (args(0).to[Int] >> 3) << 3)
  }
}
