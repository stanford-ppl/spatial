package spatial.tests.syntax

import spatial.dsl._

@spatial class IntPrinting extends SpatialTest {

  def main(args: Array[String]): Unit = {
    Accel {}
    val x = -391880660.to[Int]
    println("x: " + x)
    assert(x.toText == "-391880660")
  }

}
