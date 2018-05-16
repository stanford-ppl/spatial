package spatial.tests.feature.math


import spatial.dsl._


@test class SigmoidFloat extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type T = Float//FixPt[TRUE,_16,_16]


  def main(args: Array[String]): Unit = {
    val data = 
       Array[Float](1.0.to[Float], 2.0.to[Float], 3.0.to[Float], 4.0.to[Float], -5.0.to[Float], 6.0.to[Float], -7.0.to[Float], 8.0.to[Float], 9.0.to[Float]).reshape(3, 3)
    val dram = DRAM[Float](3, 3)
    val out_dram = DRAM[Float](3, 3)

    setMem(dram, data)

    Accel{
      val sram     = SRAM[Float](3, 3)
      val out_sram = SRAM[Float](3, 3)

      sram load dram

      Foreach(0 :: sram.rows, 0 :: sram.cols) { (d0, d1) =>
        out_sram(d0, d1) = 1 / (1 + exp(-sram(d0, d1)))
      }

      out_dram store out_sram
    }

    printArray((getMem(out_dram)), "Sigmoid : ")

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

