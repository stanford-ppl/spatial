package spatial.tests.feature.math


import spatial.dsl._


@test class NumericTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    Accel {
      val x = random[Int]
      val y = random[Int]
      val z = -x
      val q = z + y
      val m = z + x
      val f = m + y
      println(q)
      println(m)
      println(f)
    }
  }
}
