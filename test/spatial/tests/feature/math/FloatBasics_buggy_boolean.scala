package spatial.tests.feature.math

import spatial.dsl._

@test class FloatBasics_buggy_boolean extends SpatialTest {
  override def runtimeArgs: Args = "3.2752 -283.70"
  override def backends: Seq[Backend] = DISABLED

  type T = Float//FixPt[TRUE,_16,_16]

  def main(args: Array[String]): Unit = {
    val length = 8

    val data1        = Array.tabulate(length){i => i.to[Float] * 3.3.to[Float]}
    val data2        = Array.tabulate(length){i => i.to[Float] * 2.2.to[Float]}
    val data3        = Array.tabulate(length){i => i.to[Float] * 1.1.to[Float]}

    val dram1        = DRAM[Float](length)
    val dram2        = DRAM[Float](length)
    val dram3        = DRAM[Float](length)

    setMem(dram1, data1)
    setMem(dram2, data2)
    setMem(dram3, data3)

    val ffadd_out       = DRAM[Float](length)
    val ffsub_out       = DRAM[Float](length)
    val ffmul_out       = DRAM[Float](length)
    val ffdiv_out       = DRAM[Float](length)
    val ffsqrt_out      = DRAM[Float](length)
    val fflt_out        = DRAM[Boolean](length)
    val ffgt_out        = DRAM[Boolean](length)
    val ffeq_out        = DRAM[Boolean](length)
    val ffabs_out       = DRAM[Float](length)
    val ffexp_out       = DRAM[Float](length)
    val fflog_out       = DRAM[Float](length)
    val ffrec_out       = DRAM[Float](length)
    val ffrsqrt_out     = DRAM[Float](length)
    val ffsigmoid_out   = DRAM[Float](length)
    val fftanh_out     = DRAM[Float](length)
    val fffma_out     = DRAM[Float](length)

    Accel{
      val sram1    = SRAM[Float](length)
      val sram2    = SRAM[Float](length)
      val sram3    = SRAM[Float](length)

      sram1 load dram1
      sram2 load dram2
      sram3 load dram3

      val ffadd_out_sram       = SRAM[Float](length)
      val ffsub_out_sram       = SRAM[Float](length)
      val ffmul_out_sram       = SRAM[Float](length)
      val ffdiv_out_sram       = SRAM[Float](length)
      val ffsqrt_out_sram      = SRAM[Float](length)
      val fflt_out_sram        = SRAM[Boolean](length)
      val ffgt_out_sram        = SRAM[Boolean](length)
      val ffeq_out_sram        = SRAM[Boolean](length)
      val ffabs_out_sram       = SRAM[Float](length)
      val ffexp_out_sram       = SRAM[Float](length)
      val fflog_out_sram       = SRAM[Float](length)
      val ffrec_out_sram       = SRAM[Float](length)
      val ffrsqrt_out_sram     = SRAM[Float](length)
      val ffsigmoid_out_sram   = SRAM[Float](length)
      val fftanh_out_sram      = SRAM[Float](length)
      val fffma_out_sram       = SRAM[Float](length)

      Foreach (0 until length) { i=>

        ffadd_out_sram(i) = sram1(i) + sram2(i)
        ffmul_out_sram(i) = sram1(i) * sram2(i)
        ffdiv_out_sram(i) = sram1(i) / sram2(i)
        ffsqrt_out_sram(i) = sqrt(sram1(i))
        ffsub_out_sram(i) = sram1(i) - sram2(i)
        fflt_out_sram(i) = sram1(i) < sram2(i)
        ffgt_out_sram(i) = sram1(i) > sram2(i)
        ffeq_out_sram(i) = sram1(i) == sram2(i)

        // New FP operators
        ffabs_out_sram(i) = abs(sram1(i))
        ffexp_out_sram(i) = exp(sram1(i))
        fflog_out_sram(i) = ln(sram1(i))
        ffrec_out_sram(i) = 1.toFloat/sram1(i)
        ffrsqrt_out_sram(i) = 1.toFloat/sqrt(sram1(i))
        ffsigmoid_out_sram(i) = sigmoid(sram1(i))
        fftanh_out_sram(i) = tanh(sram1(i))
        fffma_out_sram(i) = sram1(i) * sram2(i) + sram3(i)
      }

      ffadd_out store ffadd_out_sram
      ffmul_out store ffmul_out_sram
      ffdiv_out store ffdiv_out_sram
      ffsqrt_out store ffsqrt_out_sram
      ffsub_out store ffsub_out_sram
      fflt_out store fflt_out_sram
      ffgt_out store ffgt_out_sram
      ffeq_out store ffeq_out_sram
      ffabs_out store ffabs_out_sram
      ffexp_out store ffexp_out_sram
      fflog_out store fflog_out_sram
      ffrec_out store ffrec_out_sram
      ffrsqrt_out store ffrsqrt_out_sram
      ffsigmoid_out store ffsigmoid_out_sram
      fffma_out store fffma_out_sram
      fftanh_out store fftanh_out_sram
    }

    printArray((getMem(dram1)), "A : ")
    printArray((getMem(dram2)), "B : ")
    printArray((getMem(dram3)), "C : ")
    printArray((getMem(ffadd_out)), "Result ADD: ")
    printArray((getMem(ffmul_out)), "Result MUL: ")
    printArray((getMem(ffdiv_out)), "Result DIV: ")
    printArray((getMem(ffsqrt_out)), "Result SRT: ")
    printArray((getMem(ffsub_out)), "Result SUB: ")
    printArray((getMem(fflt_out)), "Result FLT: ")
    printArray((getMem(ffgt_out)), "Result FGT: ")
    printArray((getMem(ffeq_out)), "Result FEQ: ")
    printArray((getMem(ffabs_out)), "Result ABS: ")
    printArray((getMem(ffexp_out)), "Result EXP: ")
    printArray((getMem(fflog_out)), "Result LOG: ")
    printArray((getMem(ffrec_out)), "Result REC: ")
    printArray((getMem(ffrsqrt_out)), "Result RST: ")
    printArray((getMem(ffsigmoid_out)), "Result SIG: ")
    printArray((getMem(fftanh_out)), "Result TAN: ")
    printArray((getMem(fffma_out)), "Result FMA: ")

    //    val ffadd_result = getArg(ffadd_out)
    //    val ffmul_result = getArg(ffmul_out)
    //    val ffsub_result = getArg(ffsub_out)
    //    val ffdiv_result = getArg(ffdiv_out)
    //    val ffsqrt_result = getArg(ffsqrt_out)
    //    val fflt_result = getArg(fflt_out)
    //    val ffgt_result = getArg(ffgt_out)
    //    val ffeq_result = getArg(ffeq_out)
    //
    //    // New FP operators
    //    val ffabs_result = getArg(ffabs_out)
    //    val ffexp_result = getArg(ffexp_out)
    //    val fflog_result = getArg(fflog_out)
    //    val ffrec_result = getArg(ffrec_out)
    //    val ffrsqrt_result = getArg(ffrsqrt_out)
    //    val ffsigmoid_result = getArg(ffsigmoid_out)
    //    val fftanh_result = getArg(fftanh_out)
    //
    //    val ffadd_gold = args(0).to[T] + args(1).to[T]
    //    val ffmul_gold = args(0).to[T] * args(1).to[T]
    //    val ffsub_gold = args(0).to[T] - args(1).to[T]
    //    val ffdiv_gold = args(0).to[T] / args(1).to[T]
    //    val ffsqrt_gold = sqrt(args(0).to[T])
    //    val ffgt_gold = args(0).to[T] > args(1).to[T]
    //    val fflt_gold = args(0).to[T] < args(1).to[T]
    //    val ffeq_gold = args(0).to[T] == args(1).to[T]
    //
    //    // New FP operators
    //    val ffabs_gold = abs(args(0).to[T])
    //    val ffexp_gold = exp(args(0).to[T])
    //    val fflog_gold = ln(args(0).to[T])
    //    val ffrec_gold = 1.toFloat/args(0).to[T]
    //    val ffrsqrt_gold = 1.toFloat/sqrt(args(0).to[T])
    //    val ffsigmoid_gold = sigmoid(args(0).to[T])
    //    val fftanh_gold = tanh(args(0).to[T])
    //
    //    println("sum        : " + ffadd_result + " == " + ffadd_gold)
    //    println("prod       : " + ffmul_result + " == " + ffmul_gold)
    //    println("sub        : " + ffsub_result + " == " + ffsub_gold)
    //    println("div        : " + ffdiv_result + " == " + ffdiv_gold)
    //    println("sqrt       : " + ffsqrt_result + " == " + ffsqrt_gold)
    //    println("gt         : " + ffgt_result + " == " + ffgt_gold)
    //    println("lt         : " + fflt_result + " == " + fflt_gold)
    //    println("eq         : " + ffeq_result + " == " + ffeq_gold)
    //
    //    // New FP operators
    //    println("abs        : " + ffabs_result + " == " + ffabs_gold)
    //    println("exp        : " + ffexp_result + " == " + ffexp_gold)
    //    println("log        : " + fflog_result + " == " + fflog_gold)
    //    println("sqrt       : " + ffsqrt_result + " == " + ffsqrt_gold)
    //    println("rec        : " + ffrec_result + " == " + ffrec_gold)
    //    println("rsqrt      : " + ffrsqrt_result + " == " + ffrsqrt_gold)
    //    println("sigmoid    : " + ffsigmoid_result + " == " + ffsigmoid_gold)
    //    println("tanh       : " + fftanh_result + " == " + fftanh_gold)
    //
    //    val cksum = ffsqrt_result == ffsqrt_gold && ffdiv_result == ffdiv_gold && ffadd_result == ffadd_gold && ffmul_result == ffmul_gold && ffsub_result == ffsub_gold && fflt_result == fflt_gold && ffgt_result == ffgt_gold && ffeq_result == ffeq_gold && ffabs_result == ffabs_gold && ffexp_result == ffexp_gold && fflog_result == fflog_gold && ffrec_result == ffrec_gold && ffrsqrt_result == ffrsqrt_gold
    //    println("PASS: " + cksum + " (SambaFloatBasics)")
  }
}

