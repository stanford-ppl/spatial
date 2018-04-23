package spatial.tests.feature.math


import spatial.dsl._


@test class SpecialMath extends SpatialTest {
  override def runtimeArgs: Args = "0.125 5.625 14 1.875 -3.4375 -5"

  type USGN = FixPt[FALSE,_4,_4]
  type SGN = FixPt[TRUE,_4,_4]


  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val a_usgn = args(0).to[USGN] //2.625.to[USGN]
    val b_usgn = args(1).to[USGN] //5.625.to[USGN]
    val c_usgn = args(2).to[USGN] //4094.to[USGN]
    val a_sgn = args(3).to[SGN]
    val b_sgn = args(4).to[SGN]
    val c_sgn = args(5).to[SGN]
    // assert(b_usgn.to[FltPt[_24,_8]] + c_usgn.to[FltPt[_24,_8]] > 15.to[FltPt[_24,_8]], "b_usgn + c_usgn must saturate (false,4,4) FP number")
    // assert(b_sgn.to[FltPt[_24,_8]] + c_sgn.to[FltPt[_24,_8]] < -8.to[FltPt[_24,_8]], "b_sgn + c_sgn must saturate (true,4,4) FP number")
    val A_usgn = ArgIn[USGN]
    val B_usgn = ArgIn[USGN]
    val C_usgn = ArgIn[USGN]
    val A_sgn = ArgIn[SGN]
    val B_sgn = ArgIn[SGN]
    val C_sgn = ArgIn[SGN]
    setArg(A_usgn, a_usgn)
    setArg(B_usgn, b_usgn)
    setArg(C_usgn, c_usgn)
    setArg(A_sgn, a_sgn)
    setArg(B_sgn, b_sgn)
    setArg(C_sgn, c_sgn)
    val N = 256

    // Conditions we will check
    val unbiased_mul_unsigned = DRAM[USGN](N) // 1
    val unbiased_mul_signed = DRAM[SGN](N) // 2
    val satur_add_unsigned = ArgOut[USGN] // 3
    val satur_add_signed = ArgOut[SGN] // 4
    val unbiased_sat_mul_unsigned = ArgOut[USGN] // 5
    val unbiased_lower_sat_mul_signed = ArgOut[SGN] // 6
    val unbiased_upper_sat_mul_signed = ArgOut[SGN] // 6


    Accel {
      val usgn = SRAM[USGN](N)
      val sgn = SRAM[SGN](N)
      Foreach(N by 1) { i =>
        usgn(i) = A_usgn *& B_usgn // Unbiased rounding, mean(yy) should be close to a*b
        sgn(i) = A_sgn *& B_sgn
      }
      unbiased_mul_unsigned store usgn
      unbiased_mul_signed store sgn
      Pipe{ satur_add_unsigned := C_usgn +! B_usgn}
      Pipe{ satur_add_signed := C_sgn +! B_sgn}
      Pipe{ unbiased_sat_mul_unsigned := B_usgn *&! C_usgn}
      Pipe{ unbiased_lower_sat_mul_signed := C_sgn *&! A_sgn}
      Pipe{ unbiased_upper_sat_mul_signed := C_sgn *&! (-1.to[SGN]*A_sgn)}
    }


    // Extract results from accelerator
    val unbiased_mul_unsigned_res = getMem(unbiased_mul_unsigned)
    val satur_add_unsigned_res = getArg(satur_add_unsigned)
    val unbiased_mul_signed_res = getMem(unbiased_mul_signed)
    val satur_add_signed_res = getArg(satur_add_signed)
    val unbiased_sat_mul_unsigned_res = getArg(unbiased_sat_mul_unsigned)
    val unbiased_lower_sat_mul_signed_res = getArg(unbiased_lower_sat_mul_signed)
    val unbiased_upper_sat_mul_signed_res = getArg(unbiased_upper_sat_mul_signed)

    // Create validation checks and debug code
    val gold_unbiased_mul_unsigned = (a_usgn * b_usgn).to[FltPt[_24,_8]]
    val gold_mean_unsigned = unbiased_mul_unsigned_res.map{_.to[FltPt[_24,_8]]}.reduce{_+_} / N
    val gold_unbiased_mul_signed = (a_sgn * b_sgn).to[FltPt[_24,_8]]
    val gold_mean_signed = unbiased_mul_signed_res.map{_.to[FltPt[_24,_8]]}.reduce{_+_} / N
    val gold_satur_add_signed = (-8).to[Float]
    val gold_satur_add_unsigned = (15.9375).to[Float]
    val gold_unbiased_sat_mul_unsigned = (15.9375).to[Float]
    val gold_unbiased_lower_sat_mul_signed = (-8).to[Float]
    val gold_unbiased_upper_sat_mul_signed = (7.9375).to[Float]

    // Get cksums
    val margin = scala.math.pow(2,-4).to[FltPt[_24,_8]]
    val cksum1 = (abs(gold_unbiased_mul_unsigned - gold_mean_unsigned).to[FltPt[_24,_8]] < margin)
    val cksum2 = (abs(gold_unbiased_mul_signed - gold_mean_signed).to[FltPt[_24,_8]] < margin)
    val cksum3 = satur_add_unsigned_res == gold_satur_add_unsigned.to[USGN]
    val cksum4 = satur_add_signed_res == gold_satur_add_signed.to[SGN]
    val cksum5 = unbiased_sat_mul_unsigned_res == gold_unbiased_sat_mul_unsigned.to[USGN]
    val cksum6 = unbiased_lower_sat_mul_signed_res == gold_unbiased_lower_sat_mul_signed.to[SGN]
    val cksum7 = unbiased_upper_sat_mul_signed_res == gold_unbiased_upper_sat_mul_signed.to[SGN]
    val cksum = cksum1 && cksum2 && cksum3 && cksum4 && cksum5 && cksum6 && cksum7

    // Helpful prints
    println(cksum1 + " Unbiased Rounding Multiplication Unsigned: |" + gold_unbiased_mul_unsigned + " - " + gold_mean_unsigned + "| = " + abs(gold_unbiased_mul_unsigned-gold_mean_unsigned) + " <? " + margin)
    println(cksum2 + " Unbiased Rounding Multiplication Signed: |" + gold_unbiased_mul_signed + " - " + gold_mean_signed + "| = " + abs(gold_unbiased_mul_signed-gold_mean_signed) + " <? " + margin)
    println(cksum3 + " Saturating Addition Unsigned: " + satur_add_unsigned_res + " =?= " + gold_satur_add_unsigned.to[USGN])
    println(cksum4 + " Saturating Addition Signed: " + satur_add_signed_res + " =?= " + gold_satur_add_signed.to[SGN])
    println(cksum5 + " Unbiased Saturating Multiplication Unsigned: " + unbiased_sat_mul_unsigned_res + " =?= " + gold_unbiased_sat_mul_unsigned.to[SGN])
    println(cksum6 + " Unbiased (lower) Saturating Multiplication Signed: " + unbiased_lower_sat_mul_signed_res + " =?= " + gold_unbiased_lower_sat_mul_signed.to[SGN])
    println(cksum6 + " Unbiased (upper) Saturating Multiplication Signed: " + unbiased_upper_sat_mul_signed_res + " =?= " + gold_unbiased_upper_sat_mul_signed.to[SGN])


    println("PASS: " + cksum + " (SpecialMath) * Need to check subtraction and division ")
    assert(cksum)
  }
}
