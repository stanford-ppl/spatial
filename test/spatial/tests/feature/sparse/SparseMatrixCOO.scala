package spatial.tests.feature.sparse

import spatial.dsl._

@spatial class SparseMatrixCOO extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val nnzA = 9
    val AM = 4
    val AK = 2
    val BK = 4
    val BN = 2

    val DP = 16

    val rowA = Array[Int](0,0,1,1,1,2,3,3,3)
    val colA = Array[Int](0,3,0,1,3,2,0,2,3)
    val datA = Array[Float](1,2,3,4,5,6,7,8,9)

    val matB = Array[Float](10,0,0,0,0,20,30,40).reshape(BK,BN)

    val dramAr = DRAM[Int](nnzA)
    val dramAc = DRAM[Int](nnzA)
    val dramAd = DRAM[Float](nnzA)
    val dramB  = DRAM[Float](BK,BN)
    val dramR  = DRAM[Float](AM,BN)

    setMem(dramAr, rowA)
    setMem(dramAc, colA)
    setMem(dramAd, datA)
    setMem(dramB, matB)

    println("Matrix A in COO:")
    printArray(rowA, "Row: ")
    printArray(colA, "Col: ")
    printArray(datA, "Dat: ")
    printMatrix(matB, "Matrix B: ")

    Accel {
      val Ar = SRAM[Int](nnzA)
      val Ac = SRAM[Int](nnzA)
      val Ad = SRAM[Float](nnzA)
      Ar load dramAr(0 :: nnzA par DP)
      Ac load dramAc(0 :: nnzA par DP)
      Ad load dramAd(0 :: nnzA par DP)

      val B = SRAM[Float](BK,BN)
      B load dramB(0 :: BK, 0 :: BN par DP)
      val R = SRAM[Float](AM,BN)

      Foreach(0 :: AM, 0 :: BN) { (i,j) =>
        R(i,j) = 0.to[Float]
      }

      Foreach(0 :: nnzA) {i =>
        Foreach(0 :: BN) {j =>
          R(Ar(i),j) = R(Ar(i),j) + Ad(i)*B(Ac(i),j)
        }
      }

      dramR(0 :: AM, 0 :: BN) store R

    }
    val outR = getMatrix(dramR)
    printMatrix(outR, "Output R: ")

  }
}
