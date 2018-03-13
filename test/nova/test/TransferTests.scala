package nova.test

import spatial.dsl._
import utest._

@spatial object DenseTransfer1D {
  def main(): Void = {
    val dram256 = DRAM[I32](256)
    val dram128 = DRAM[I32](128)
    val dram64  = DRAM[I32](64)

    Accel {
      val sram1 = SRAM[I32](128)

      sram1 load dram256(0::128)
      dram128 store sram1

      sram1 load dram128(64::128)
      dram64(32::64) store sram1(16::48)

      sram1(17::49) load dram64(0::32)
      dram256(99::131) store sram1(1::17)
    }
  }
}

@spatial object SparseTransfer1D {
  def main(): Void = {
    val dram256 = DRAM[F32](256)
    val addr128 = DRAM[I32](128)

    Accel {
      val sram1 = SRAM[F32](128)
      val addr  = SRAM[I32](128)

      addr load addr128
      sram1 gather dram256(addr)
    }
  }
}

object TransferTests extends Testbench { val tests = Tests {
  'DenseTransfer1D - test(DenseTransfer1D)
  'SparseTransfer1D - test(SparseTransfer1D)
}}
