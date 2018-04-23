package spatial.tests.syntax

import spatial.dsl._

@struct case class MyStruct(x: I32, y: I32, z: I32)

@test class SimpleStructTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val x = random[MyStruct]
    val y = random[MyStruct]
    println(r"x: $x")
    println(r"y: $y")
    println(r"x + y: ${x + y}")
  }
}
