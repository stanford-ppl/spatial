package spatial.tests.plasticine

import spatial.dsl._
import utils.io.files
import spatial.util.spatialConfig

@spatial class SPMV_ELL extends SpatialTest { // Regression (Sparse) // Args: none

  val L = 16 // param [ 64 ]
  val N = 64 // param [ pmuSize / <L> * 16 ]
  val ts = 32 // param [ pmuSize / <L> ] | <N> % p == 0
  val op = 1 // param [1] | <N> % p == 0
  val mp = 1 // param [1,2,4] | <N> % p == 0
  val ip = 16

 /*                                                                                                  
   Sparse Matrix is the IEEE 494 bus interconnect matrix from UF Sparse Datasets   
    Datastructures in Ellpack:
                                                                                                                                                 
                                             ←   L    →
        _____________________                __________         _________                                                                              
       | 9  0  2  0  0  0  1 |          ↑   | 0  2  6  |       | 9  2  1 |                                                                             
       |                     |              |          |       |         |                                                                             
       | 3  0  0  0  0  2  0 | ===>     N   | 0  5  *  |       | 3  2  * |                                                                             
       |                     |              |          |       |         |                                                                             
       | 0  0  0  0  5  0  0 |          ↓   | 4  *  *  |       | 5  *  * |                                                                             
        `````````````````````                ``````````         `````````                                                                              
         uncompressed                        cols               data                                                                                             
                                                                                       
 */
  def toFile(array:scala.Array[_], fileName:java.lang.String) = {
    val pw = new java.io.PrintWriter(new java.io.File(fileName))
    array.foreach { 
      case a:scala.Array[_] => pw.write(s"${a.mkString(",")}\n")
      case a => pw.write(s"$a\n")
    }
    pw.close
  }

  def generateData = {
    val denseMat = scala.Array.fill(N,N)(0)
    val sparseValues = scala.Array.fill(N,L)(0)
    val sparseCols = scala.Array.fill(N,L)(0)
    denseMat.zipWithIndex.foreach { case (row,r) =>
      scala.util.Random.shuffle(List.tabulate(N){i => i}).slice(0,L).zipWithIndex.foreach { case (c,l) =>
        val v = scala.util.Random.nextInt(100) 
        row(c) = v
        sparseValues(r)(l) = v
        sparseCols(r)(l) = c
      }
    }
    val denseVec = scala.Array.fill(N) { scala.util.Random.nextInt(100) }
    val denseGold = denseMat.map { case row => row.zip(denseVec).map { case (e,v) => e*v }.sum }
    toFile(denseMat, files.buildPath(spatialConfig.genDir,  s"ell_mat.csv"))
    toFile(sparseValues, files.buildPath(spatialConfig.genDir,  s"ell_values.csv"))
    toFile(sparseCols, files.buildPath(spatialConfig.genDir,  s"ell_cols.csv"))
    toFile(denseVec, files.buildPath(spatialConfig.genDir,  s"ell_vec.csv"))
    toFile(denseGold, files.buildPath(spatialConfig.genDir,  s"ell_gold.csv"))
  }

  def main(args: Array[String]): Unit = {
    type T = Int

    generateData
    val raw_values = loadCSV2D[T](files.buildPath(spatialConfig.genDir, "ell_values.csv"))
    val raw_cols = loadCSV2D[Int](files.buildPath(spatialConfig.genDir, s"ell_cols.csv"))
    val raw_vec = loadCSV1D[T](files.buildPath(spatialConfig.genDir, s"ell_vec.csv"), "\n")

    val values_dram = DRAM[T](N,L)
    val cols_dram = DRAM[Int](N,L)
    val vec_dram = DRAM[T](N)
    val result_dram = DRAM[T](N)

    setMem(values_dram, raw_values)
    setMem(cols_dram, raw_cols)
    setMem(vec_dram, raw_vec)

    Accel {
      Foreach(N by ts par op){ tile => 
        val cols_sram = SRAM[Int](ts, L)
        val values_sram = SRAM[T](ts, L)
        val result_sram = SRAM[T](ts)

        cols_sram load cols_dram(tile::tile+ts, 0::L par ip)
        values_sram load values_dram(tile::tile+ts, 0::L par ip)

        Foreach(ts by 1 par mp) { i => 
          val vec_sram = SRAM[T](L)
          val gather_addrs = SRAM[Int](L)
          Foreach(L by 1 par ip) { j => gather_addrs(j) = cols_sram(i, j) }
          vec_sram gather vec_dram(gather_addrs par ip, L)
          val element = Reduce(Reg[T](0))(L by 1 par ip) { k => values_sram(i,k) * vec_sram(k) }{_+_}
          result_sram(i) = element
        }

        result_dram(tile::tile+ts par ip) store result_sram

      }
    }

    val data_gold = loadCSV1D[T](files.buildPath(spatialConfig.genDir,  s"ell_gold.csv"), "\n")
    val data_result = getMem(result_dram)

    printArray(data_gold, "Gold: ")
    printArray(data_result, "Result: ")
    printArray(data_result.zip(data_gold){_-_}, "delta")

    val margin = 0.2.to[T] // Scala does not stay in bounds as tightly as chisel
    val cksum = data_gold.zip(data_result){(a,b) => abs(a-b) <= margin}.reduce{_&&_}

    println("PASS: " + cksum + " (SPMV_ELL)")

  }
}
