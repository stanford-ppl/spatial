package spatial.tests.feature.host

import spatial.dsl._

@test class CSVFileIO extends SpatialTest {
  override def runtimeArgs = "32"

  def main(args: Array[String]): Unit = {
    Accel { /* no hardware */ }

    val array = Array.tabulate(32){i => random[Float] }

    writeCSV1D(array, "array.csv")

    val array_2 = loadCSV1D[Float]("array.csv")

    val matrix = Matrix.tabulate(32,32){(i,j) => random[Float] }
    writeCSV2D(matrix, "matrix.csv")

    val matrix_2 = loadCSV2D[Float]("matrix.csv")

    assert(array == array_2)
    assert(matrix == matrix_2)
  }
}
