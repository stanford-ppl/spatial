package pcc.test

import pcc.lang._

object GEMM extends Testbench {
  def main(): Void = {
    val M = 96  // Rows of output
    val N = 96  // Cols of output
    val K = 96  // Common dimension
    val a = SRAM[I32](M,K)
    val b = SRAM[I32](K,N)
    val c = SRAM[I32](M,N)


  }
}

class BLASTests extends Tests {
  "GEMM" should "compile" in { test(GEMM) }
}
