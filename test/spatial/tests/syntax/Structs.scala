package spatial.tests.syntax

import spatial.dsl._

@struct case class MyStruct(x: I32, y: I32, z: I32)

@test class SimpleStructTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val x = random[MyStruct]
    val y = random[MyStruct]
    val z = x + y
    println(r"x: $x")
    println(r"y: $y")
    println(r"x + y: $z")
    assert(x.x + y.x == z.x)
    assert(x.y + y.y == z.y)
    assert(x.z + y.z == z.z)
  }
}
