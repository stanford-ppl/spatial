package pcc.test

import pcc.lang._
import pcc.lib._

object GEMM extends Testbench {
  def main(): Void = {
    val M = 96  // Rows of output
    val N = 96  // Cols of output
    val K = 96  // Common dimension
    val a = SRAM[I32](M,K)
    val b = SRAM[I32](K,N)
    val y = SRAM[I32](M,N)
    gemm[I32](y,a,b,0,false,false,1,2)
  }
}

class BLASTests extends Tests {
  "GEMM" should "compile" in { test(GEMM) }
}
