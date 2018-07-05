package spatial.tests.feature.control


import spatial.dsl._


@spatial class ReduceArbitraryLambda extends SpatialTest {
  override def runtimeArgs: Args = "8"


  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val x = ArgIn[Int]
    val r_xor = ArgOut[Int]
    val f_xor = ArgOut[Int]
    val N = args(0).to[Int]

    // Connect SW vals to HW vals
    setArg(x, N)

    // Create HW accelerator
    Accel {
      val reduce_xor = Reg[Int](99)
      Reduce(reduce_xor)(x by 1){i =>
        val temp = mux(i % 3 == 1, i, i+1)
        temp
      } { _^_ }
      r_xor := reduce_xor

      val fold_xor = Reg[Int](99)
      Fold(fold_xor)(x by 1){i =>
        val temp = Reg[Int](0)
        temp := mux(i % 3 == 1, i, i+1)
        temp
      } { _^_ }
      f_xor := fold_xor
    }


    // Extract results from accelerator
    val reduce_xor_result = getArg(r_xor)
    val fold_xor_result = getArg(f_xor)

    // Create validation checks and debug code
    val gold_reduce_xor = Array.tabulate(N){i => if (i % 3 == 1) i else i+1}.reduce{_^_}
    val gold_fold_xor = Array.tabulate(N){i => if (i % 3 == 1) i else i+1}.reduce{_^_} ^ 99
    println("Reduce XOR: ")
    println("  expected: " + gold_reduce_xor)
    println("  result: " + reduce_xor_result)
    println("Fold XOR: ")
    println("  expected: " + gold_fold_xor)
    println("  result: " + fold_xor_result)

    val cksum_reduce_xor = gold_reduce_xor == reduce_xor_result
    val cksum_fold_xor = gold_fold_xor == fold_xor_result
    val cksum = cksum_reduce_xor && cksum_fold_xor
    assert(cksum)
  }
}
