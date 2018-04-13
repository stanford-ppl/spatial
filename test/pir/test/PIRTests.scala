package pir.test

import spatial.lib._
import pir.dsl._

@pir object PIR_LSTM {
  val STAGES = 6
  val LANES  = 16

  def main(): Void = {
    val M = 96
    val N = 1
    val K = 96
    val a = SRAM[I32](M,K)
    val b = SRAM[I32](K,N)
    val c = SRAM[I32](M,N)

    val par = 4

    def getWeightPMUs(s: => SRAM2[I32]) = {
      List.fill(par) {
        val pmu = PMU(s)
        pmu.read(Seq(0::STAGES::M, 0::LANES::N, 0::LANES::K, 0::STAGES, LANES par LANES)){case Seq(i,j,k,ii,kk) =>
          (i+ii)*M + k + kk
        }
        pmu
      }
    }

    def GEMV(rows: scala.Int, cols: scala.Int) = PCU(0::STAGES::rows,0::LANES::cols) { (i,j) =>
      val a = pcu.in[I32](0)
      val b = pcu.in[I32](1)
      val y = pcu.out[I32](0)
      y := BBox.GEMM(a,b)
    }

    def StreamingPCU(numIns: scala.Int, tileSize: scala.Int) = PCU(0::tileSize::1) { i =>
      val y = pcu.out[I32](0)
      y := (numIns match {
        case 1 => BBox.Stream1(pcu.in[I32](0))
        case 2 => BBox.Stream2(pcu.in[I32](0), pcu.in[I32](1))
        case 3 => BBox.Stream3(pcu.in[I32](0), pcu.in[I32](1), pcu.in[I32](2))
        case 4 => BBox.Stream4(pcu.in[I32](0), pcu.in[I32](1), pcu.in[I32](2), pcu.in[I32](3))
        case _ => throw new Exception(s"Unsupported number of inputs ($numIns) into StreamingPCU!")
      })
    }

    def getGEMVPCUs(w: List[PMUPtr], xin: PMUPtr, res: PMUPtr) = {
      List.tabulate(w.size) { i =>
        val pcu = GEMV(M, N)
        pcu.out[I32](0) ==> res.in[I32](i)
        w(i).out[I32](0) ==> pcu.in[I32](0)
        xin.out[I32](0) ==> pcu.in[I32](1)
      }
    }

    def getResultPMU(s: SRAM2[I32]) = {
      val pmu = PMU(s)
      pmu.write(Seq(0::STAGES::M,0::LANES::N, 0::STAGES, LANES par LANES)){case Seq(i,j,ii,jj) =>
        val addr = (i+ii)*M + j + jj
        val data = pmu.in[I32](0)
        (addr, data)
      }
      pmu
    }

    val x = PMU(b)
    x.read(Seq(0::M, 0::LANES::N, 0::K, LANES par LANES)){case Seq(i,j,k,jj) =>
      k*K + jj
    }

    val ct = PMU(b)
    ct.read(Seq(0::M, 0::LANES::N, 0::K, LANES par LANES)){case Seq(i,j,k,jj) =>
      k*K + jj
    }
    ct.write(Seq(0::M::1)) { case Seq(i) =>
      val addr = i
      val data = pmu.in[I32](0)
      (addr, data)
    }


    // All the Ws
    val wf = getWeightPMUs(SRAM[I32](M,K))
    val wo = getWeightPMUs(SRAM[I32](M,K))
    val wc = getWeightPMUs(SRAM[I32](M,K))
    val wi = getWeightPMUs(SRAM[I32](M,K))

    val wfResult = getResultPMU(SRAM[I32](M,N))
    val woResult = getResultPMU(SRAM[I32](M,N))
    val wcResult = getResultPMU(SRAM[I32](M,N))
    val wiResult = getResultPMU(SRAM[I32](M,N))

    val wf_GEMV_PCUs = getGEMVPCUs(wf, x, wfResult)
    val wo_GEMV_PCUs = getGEMVPCUs(wo, x, woResult)
    val wc_GEMV_PCUs = getGEMVPCUs(wc, x, wcResult)
    val wi_GEMV_PCUs = getGEMVPCUs(wi, x, wiResult)

    val h = PMU(b)
    h.read(Seq(0::M, 0::LANES::N, 0::K, LANES par LANES)){case Seq(i,j,k,jj) =>
      k*K + jj
    }
    h.write(Seq(0::M::1)) { case Seq(i) =>
      val addr = i
      val data = pmu.in[I32](0)
      (addr, data)
    }

    // All the Rs
    val rf = getWeightPMUs(SRAM[I32](M,K))
    val ro = getWeightPMUs(SRAM[I32](M,K))
    val rc = getWeightPMUs(SRAM[I32](M,K))
    val ri = getWeightPMUs(SRAM[I32](M,K))

    val rfResult = getResultPMU(SRAM[I32](M,N))
    val roResult = getResultPMU(SRAM[I32](M,N))
    val rcResult = getResultPMU(SRAM[I32](M,N))
    val riResult = getResultPMU(SRAM[I32](M,N))

    val rf_GEMV_PCUs = getGEMVPCUs(rf, h, rfResult)
    val ro_GEMV_PCUs = getGEMVPCUs(ro, h, roResult)
    val rc_GEMV_PCUs = getGEMVPCUs(rc, h, rcResult)
    val ri_GEMV_PCUs = getGEMVPCUs(ri, h, riResult)

    // All the Bs
    val bf = getResultPMU(SRAM[I32](M,N))
    val bo = getResultPMU(SRAM[I32](M,N))
    val bc = getResultPMU(SRAM[I32](M,N))
    val bi = getResultPMU(SRAM[I32](M,N))

//    val int1_f = getResultPMU(SRAM[I32](M,N))
//    val int1_o = getResultPMU(SRAM[I32](M,N))

    val stream1_f = StreamingPCU(3, M*N)
//    stream1_f.out[I32](0) ==> int1_f.in[I32](0)
    bf.out[I32](0) ==> stream1_f.in[I32](0)
    wfResult.out[I32](0) ==> stream1_f.in[I32](1)
    rfResult.out[I32](0) ==> stream1_f.in[I32](2)

    val ptwiseMul_ct_f = StreamingPCU(2, M*N)
    stream1_f.out[I32](0) ==> ptwiseMul_ct_f.in[I32](0)
    ct.out[I32](0) ==> ptwiseMul_ct_f.in[I32](1)


    val stream1_o = StreamingPCU(3, M*N)
    bo.out[I32](0) ==> stream1_o.in[I32](0)
    woResult.out[I32](0) ==> stream1_o.in[I32](1)
    roResult.out[I32](0) ==> stream1_o.in[I32](2)


    val stream1_c = StreamingPCU(3, M*N)
    val stream1_i = StreamingPCU(3, M*N)
    bc.out[I32](0) ==> stream1_c.in[I32](0)
    wcResult.out[I32](0) ==> stream1_c.in[I32](1)
    rcResult.out[I32](0) ==> stream1_c.in[I32](2)

    bi.out[I32](0) ==> stream1_i.in[I32](0)
    wiResult.out[I32](0) ==> stream1_i.in[I32](1)
    riResult.out[I32](0) ==> stream1_i.in[I32](2)

    val ptwiseMul_ci = StreamingPCU(2, M*N)
    stream1_c.out[I32](0) ==> ptwiseMul_ci.in[I32](0)
    stream1_i.out[I32](0) ==> ptwiseMul_ci.in[I32](1)

    val ptwiseAdd_ct_f_ci = StreamingPCU(2, M*N)
    ptwiseAdd_ct_f_ci.out[I32](0) ==> ct.in[I32](0)
    ptwiseMul_ct_f.out[I32](0) ==> ptwiseAdd_ct_f_ci.in[I32](0)
    ptwiseMul_ci.out[I32](0) ==> ptwiseAdd_ct_f_ci.in[I32](1)

    val tanh_ptwiseMul_h = StreamingPCU(2, M*N)
    ptwiseAdd_ct_f_ci.out[I32](0) ==> tanh_ptwiseMul_h.in[I32](0)
    stream1_o.out[I32](0) ==> tanh_ptwiseMul_h.in[I32](1)
    tanh_ptwiseMul_h.out[I32](0) ==> h.in[I32](0)
  }
}

@pir object PIR_GEMM {
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

class PIRTests extends Testbench {
  "PIR_GEMM" should "compile" in test(PIR_GEMM)
  "PIR_LSTM" should "compile" in test(PIR_LSTM)
}
