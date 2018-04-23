package spatial.tests.feature.host


import spatial.dsl._


@test class CSVFileIO extends SpatialTest {
  override def runtimeArgs = "32"


  def main(args: Array[String]): Unit = {

    val data_1 = Array[Float](
      0.4136132299900055.to[Float],
      0.874595582485199.to[Float],
      0.684117317199707.to[Float],
      0.5394867658615112.to[Float],
      0.2196749448776245.to[Float],
      0.16619449853897095.to[Float],
      0.1406947374343872.to[Float],
      0.776077389717102.to[Float],
      0.22084681689739227.to[Float],
      0.018980296328663826.to[Float],
      0.09581465274095535.to[Float],
      0.8470779657363892.to[Float],
      0.35807961225509644.to[Float],
      0.6214657425880432.to[Float],
      0.3178744316101074.to[Float],
      0.9223478436470032.to[Float]
    ).reshape(1, 16)
    val dram_1 = DRAM[Float](1, 16)
    setMem(dram_1, data_1)

    val data_2 = loadCSV1D[Float]("data_4.csv").reshape(64, 16)
    val dram_2 = DRAM[Float](64, 16)
    setMem(dram_2, data_2)

    val data_3 = loadNumpy1D[Float]("numpy_1d")
    val dram_3 = DRAM[Float](1536)
    setMem(dram_3, data_3)

    val data_4 = loadNumpy2D[Float]("numpy_2d")
    val dram_4 = DRAM[Float](64, 1536)
    setMem(dram_4, data_4)

    Accel {}
    writeCSV1D(getMem(dram_1), "out_1.csv")
  }
}
