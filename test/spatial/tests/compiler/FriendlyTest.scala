package spatial.tests.compiler



import spatial.dsl._

@test class FriendlyTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val y = ArgIn[Int]
    val z = ArgOut[Int]
    setArg(y, 3)
    Accel {
      z := y + 2
    }
    println("z: " + getArg(z))
    assert(getArg(z) == 5.to[Int])
  }
}