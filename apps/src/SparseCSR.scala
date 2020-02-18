import spatial.dsl._

@spatial object SparseCSR extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val nRows = 32
    val nData = 102
    val nIndices = 102
    val nIndPtr = 33
    val nParallelRowAccumulator = 2
    val tileSize = 16
    val ldPar = 16
    val stPar = ldPar
    val innerPar = ldPar

    // In the ideal case, we always want to load a whole row in...
    val tileSize = nRows
    val sVector = Array.tabulate[Float](nRows)(i => i.to[Float])
    val sData = Array.fromSeq[Float](
      Seq(0.82538526, 0.23436548, 0.67456224, 0.345829, 0.91303013, 0.26630543,
        0.84685881, 0.70958745, 0.84813863, 0.6638723, 0.84735932, 0.80099993,
        0.72452399, 0.63030495, 0.31487657, 0.04477041, 0.53748159, 0.51650527,
        0.61109176, 0.64149434, 0.57513183, 0.41826543, 0.67734873, 0.97215257,
        0.3756502, 0.28356195, 0.3773123, 0.90568419, 0.70155668, 0.90019546,
        0.80764669, 0.28152205, 0.36799031, 0.82249431, 0.8117535, 0.71898733,
        0.25474263, 0.44926126, 0.50620262, 0.70056397, 0.4344637, 0.61624304,
        0.17154159, 0.79273699, 0.4056032, 0.09524313, 0.93211774, 0.82795316,
        0.72619263, 0.35859505, 0.07734777, 0.55759033, 0.02369857, 0.50777856,
        0.00229054, 0.46404079, 0.10736844, 0.74750711, 0.3139777, 0.28427252,
        0.91964132, 0.53888453, 0.57681754, 0.31808641, 0.31749014, 0.16538921,
        0.64981869, 0.63993348, 0.76721419, 0.75473634, 0.30987503, 0.86673804,
        0.09086649, 0.81305195, 0.64693354, 0.94255718, 0.99452586, 0.49097112,
        0.50741287, 0.98493087, 0.73007804, 0.47950613, 0.76688629, 0.79615313,
        0.19615125, 0.22192891, 0.97819849, 0.9973014, 0.80784269, 0.75284276,
        0.21970308, 0.3327127, 0.28674467, 0.67709781, 0.44251328, 0.35022884,
        0.30406676, 0.17288318, 0.39838092, 0.84190886, 0.21676866, 0.56990291))

    val sIndices = Array.fromSeq[I32](
      Seq(5, 7, 17, 27, 0, 8, 12, 21, 28, 31, 3, 20, 27, 9, 13, 23, 28, 24, 1,
        2, 23, 31, 3, 7, 13, 21, 25, 8, 16, 14, 3, 18, 25, 3, 10, 18, 22, 15, 2,
        9, 17, 24, 31, 4, 20, 24, 25, 2, 14, 7, 16, 17, 24, 9, 11, 19, 25, 0, 5,
        14, 25, 28, 7, 8, 9, 11, 14, 12, 26, 0, 5, 15, 9, 10, 25, 31, 0, 13, 30,
        6, 8, 12, 16, 18, 1, 2, 13, 17, 25, 14, 24, 10, 30, 7, 11, 18, 31, 26,
        31, 10, 19, 21))
    val sIndptrs = Array.fromSeq[I32](
      Seq(0, 4, 10, 13, 17, 18, 22, 27, 29, 30, 33, 37, 38, 43, 47, 49, 53, 57,
        62, 67, 69, 70, 72, 76, 79, 84, 89, 91, 93, 97, 98, 99, 102))

    val data = DRAM[Float](nData)
    val indices = DRAM[I32](nIndices)
    val indptrs: DRAM1[I32] = DRAM[I32](nIndPtr)
    val vector: DRAM1[Float] = DRAM[Float](nRows)
    val result = DRAM[Float](nRows)

    setMem(data, sData)
    setMem(indices, sIndices)
    setMem(indptrs, sIndptrs)
    setMem(vector, sVector)

    Accel {
      // TODO: Not sure what the size is for rowPtrs...
      //  For now, I'm assuming that we have enough on-chip capacity to store all the rowPtrs.
      val pointerB = SRAM[I32](nRows)
      val pointerE = SRAM[I32](nRows)
      pointerB load indptrs(0 :: nRows par ldPar)
      pointerE load indptrs(1 :: nRows + 1 par ldPar)
      val vec = SparseSRAM[Float](nRows)
      val resultVec = SRAM[Float](nRows)
      vec load vector(0 :: nRows par ldPar)

      // TODO: For now, I'm assuming that we should have enough on-chip capacity to store the CSR vector.
      //  The ones we use for benchmarking at most takes 50K * 4Bytes = 200KBytes...

      Foreach(nRows by nParallelRowAccumulator) { iRow =>

        // TODO: Thoughts: do dataTiles and colTiles need to be SparseSRAM?
        val dataTiles = scala.List.tabulate(nParallelRowAccumulator)(_ =>
          SparseSRAM[Float](16))
        val colTiles = scala.List.tabulate(nParallelRowAccumulator)(_ =>
          SparseSRAM[I32](16)
        )
        val rowIndices = scala.List.tabulate(nParallelRowAccumulator)(i => iRow + i.to[I32])
        ((colTiles zip dataTiles) zip rowIndices).foreach{
          case ((colTile, dataTile), loadIndex) =>
            val reduceTile = SparseSRAM[Float](16) // Should be of the same size as dataTile

            val startIdx = pointerB(loadIndex)
            val endIdx = pointerB(loadIndex + 1)
            val valLen = endIdx - startIdx
            colTile load indices(startIdx :: endIdx par ldPar)
            dataTile load data(startIdx :: endIdx par ldPar)
            val accumReg = Reg[Float]
            Foreach (0 until valLen par innerPar) { i =>
              val addr = colTile(i)
              splitter(addr) {
                reduceTile(i) = dataTile(i) * vec(addr) // This one is definitely sparse read...
              }
            }

            // Followed by a reduce to do the right thing?
            val storeIndex = loadIndex
            resultVec(storeIndex) = Reduce(Reg[Float])(0 :: valLen par innerPar) { i =>
              reduceTile(i)
            }{_+_}.value
        }
      }

      result store resultVec(0 :: nRows par stPar)
    }
  }
}
