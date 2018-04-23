package spatial.tests.feature.host


import spatial.dsl._


@test class TensorConstructors extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val array = Array.tabulate(32){i => random[Int](10) }
    val matrix = (0::4,0::10){(i,j) => random[Int](10) }

    Accel { }

    printArray(array)
    printMatrix(matrix)
  }

}
