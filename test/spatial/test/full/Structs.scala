package spatial.test.full

import spatial.dsl._
import spatial.test.Testbench


@struct case class MyStruct(x: I32, y: I32, z: I32)

@spatial object SimpleStructTest {
  def main(args: Array[String]): Void = {
    val x = random[MyStruct]
    val y = random[MyStruct]
    println(r"x: $x")
    println(r"y: $y")
    println(r"x + y: ${x + y}")
  }
}

class Structs extends Testbench {
  test(SimpleStructTest)
}