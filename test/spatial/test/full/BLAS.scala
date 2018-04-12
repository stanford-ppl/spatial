package spatial.test.full

import spatial.math.LinearAlgebra._
import spatial.dsl._
import spatial.test.Testbench


@spatial object GEMM {
  def main(args: Array[String]): Void = Accel {
    val M = 96  // Rows of output
    val N = 96  // Cols of output
    val K = 96  // Common dimension
    val a = SRAM[I32](M,K)
    val b = SRAM[I32](K,N)
    val y = SRAM[I32](M,N)
    gemm[I32](y,a,b,0,false,false,1,2)
  }
}

class BLAS extends Testbench {
  test(GEMM)
}
