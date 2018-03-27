package spatial.test

import spatial.dsl._
import utest._

import forge.tags._

@struct case class MyStruct(x: I32, y: I32, z: I32)

@spatial object SimpleStructTest {

  def main(): Void = {
    val x = random[MyStruct]
    val y = random[MyStruct]
    println(r"x: $x")
    println(r"y: $y")
    println(r"x + y: ${x + y}")
  }
}

object StructTests extends Testbench { val tests = Tests {
  'SimpleStructTest - test(SimpleStructTest)
}}