package spatial.tests.compiler

import argon._
import spatial.data._
import spatial.dsl._

@spatial class CLITest extends SpatialTest {
  override def runtimeArgs: Args = "10 2 0.1 15"

  def main(args: Array[String]): Unit = {
    val x = args(0).to[Int] * 32
    val y = args(1)
    val z = args(2).to[Float]
    val q = (args(4) + "32").to[Int] * 55 - 3

    val f = x + q

    println(r"x: $x, y: $y, z: $z, q: $q, f: $f")
    assert(q == (args(4).to[Int] + 32)*55 - 3)
  }

  override def checkIR(block: Block[_]): Result = {
    Console.out.println("CHECKING IR!!")
    CLIArgs.apply(0) shouldBe "x"
    CLIArgs.apply(1) shouldBe "y"
    CLIArgs.apply(2) shouldBe "z"
    CLIArgs.apply(4) shouldBe "q"
    Unknown
  }
}
