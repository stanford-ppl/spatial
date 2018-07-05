package spatial.tests.feature.host

import spatial.dsl._

@spatial class TensorConstructors extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val array = Array.tabulate(32){i => random[Int](10) }
    val matrix = (0::4,0::10){(i,j) => random[Int](10) }

    Accel { }

    printArray(array)
    printMatrix(matrix)
    assert(array.length == 32)
    assert(matrix.rows == 4 && matrix.cols == 10)
  }

}
