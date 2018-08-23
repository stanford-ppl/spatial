package spatial.tests.feature.host

import spatial.dsl._

@spatial class CSVFileIO extends SpatialTest {
  override def runtimeArgs = "32"

  def main(args: Array[String]): Unit = {
    Accel { /* no hardware */ }

    val array = Array.tabulate(32){i => random[Float] }

    writeCSV1D(array, "array.csv")

    val array_2 = loadCSV1D[Float]("array.csv")

    val matrix = Matrix.tabulate(32,32){(i,j) => random[Float] }
    writeCSV2D(matrix, "matrix.csv")

    val matrix_2 = loadCSV2D[Float]("matrix.csv")

    printArray(array, "Array In:")
    printArray(array_2, "Array Out:")
    printMatrix(matrix, "Matrix In:")
    printMatrix(matrix_2, "Matrix Out:")

    assert(array.zip(array_2){case (a,b) => abs(a-b) < 0.00001}.reduce{_&&_})
    assert(matrix.zip(matrix_2){case (a,b) => abs(a-b) < 0.00001}.reduce{_&&_})
  }
}
