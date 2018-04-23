package spatial.tests.feature.host


import spatial.dsl._


@test class FloatHost extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val x = random[Float]
    val y = 0.to[Float]
    val o = 1.to[Float]
    val z = x * y
    Accel { }
    println("(" + x + " * " + y + ") = 0 = " + z)
    println("x: " + x.to[Float])
    println(o / x)
    println(o / sqrt(x))
  }
}
