package pcc.test

import pcc.lang._
import pcc.lang.pir._

object PIR_GEMM extends Test {
  override val isPIR = true
  val STAGES = 6
  val LANES  = 16

  def main(): Void = {
    val M = 96
    val N = 96
    val K = 96
    val a = SRAM[I32](M,K)
    val b = SRAM[I32](K,N)
    val c = SRAM[I32](M,N)

    val pmuA = PMU(a)
    pmuA.read(Seq(0::STAGES::M, 0::LANES::N, 0::LANES::K, 0::STAGES, LANES par LANES)){case Seq(i,j,k,ii,kk) =>
      (i+ii)*M + k + kk
    }

    val pmuB = PMU(b)
    pmuB.read(Seq(0::M, 0::LANES::N, 0::K, LANES par LANES)){case Seq(i,j,k,jj) =>
      k*K + jj
    }

    val pmuC = PMU(c)
    //pmuC.buffer = 2
    pmuC.write(Seq(0::STAGES::M,0::LANES::N, 0::STAGES, LANES par LANES)){case Seq(i,j,ii,jj) =>
      val addr = (i+ii)*M + j + jj
      val data = pmu.in[I32](0)
      (addr, data)
    }

    val pcu1 = PCU(0::STAGES::M,0::LANES::N){(i,j) =>
      val a = pcu.in[I32](0)
      val b = pcu.in[I32](1)
      val y = pcu.out[I32](0)
      y := BBox.GEMM(a,b)
    }

    pcu1.out[I32](0) ==> pmuC.in[I32](0)
    pmuA.out[I32](0) ==> pcu1.in[I32](0)
    pmuB.out[I32](0) ==> pcu1.in[I32](1)

  }
}

class PIRTests extends Tests {
  "PIR_GEMM" should "compile" in { test(PIR_GEMM) }
}
