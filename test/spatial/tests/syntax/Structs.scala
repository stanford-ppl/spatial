package spatial.tests.syntax

import spatial.dsl._

@struct case class MyStruct(x: I32, y: I32, z: I32) {
  @forge.tags.api def +(that: MyStruct): MyStruct = {
    MyStruct(x + that.x, y + that.y, z + that.z)
  }
}

@spatial class SimpleStructTest extends SpatialTest {

  def main(args: Array[String]): Void = {
    val x = random[MyStruct]
    val y = random[MyStruct]
    val z: MyStruct = x + y
    println(r"x: $x")
    println(r"y: $y")
    println(r"x + y: $z")
    assert(x.x + y.x == z.x)
    assert(x.y + y.y == z.y)
    assert(x.z + y.z == z.z)
  }
}

@struct case class GenericStruct[T: Num](x: T, y: T, z: T) {
  @forge.tags.api def +(that: GenericStruct[T]): GenericStruct[T] = {
    GenericStruct(x + that.x, y + that.y, z + that.z)
  }
}

@spatial class GenericStructTest extends SpatialTest {
  def main(args: Array[String]): Void = {
    val x = random[GenericStruct[I32]]
    val y = random[GenericStruct[I32]]
    val z = x + y
    println(r"x: $x")
    println(r"y: $y")
    println(r"x + y: $z")
    assert(x.x + y.x == z.x)
    assert(x.y + y.y == z.y)
    assert(x.z + y.z == z.z)
  }
}
