package spatial.tests.feature.math


import spatial.dsl._


@spatial class FloatBasics extends SpatialTest { // Regression (Unit) // Args: 3.2752 -283.70
  override def runtimeArgs: Args = "3.2752 -283.70"

  type T = Float

  def main(args: Array[String]): Unit = {

    val in1 = ArgIn[T]
    val in2 = ArgIn[T]
    val ffadd_out = ArgOut[T]
    val ffmul_out = ArgOut[T]
    val ffdiv_out = ArgOut[T]
    val ffsqrt_out = ArgOut[T]
    val ffpow_out = ArgOut[T]
    val ffsub_out = ArgOut[T]
    val ffmax_out = ArgOut[T]
    val fflt_out = ArgOut[Boolean]
    val ffgt_out = ArgOut[Boolean]
    val ffeq_out = ArgOut[Boolean]

    setArg(in1, args(0).to[T])
    setArg(in2, args(1).to[T])
    Accel{
      ffmax_out := max(in1 , in2)
      ffadd_out := in1 + in2
      ffmul_out := in1 * in2
      ffdiv_out := in1 / in2
      ffsqrt_out := sqrt(in1)
      ffpow_out := pow(in1,2)
      ffsub_out := in1 - in2
      fflt_out := in1 < in2
      ffgt_out := in1 > in2
      ffeq_out := in1.value == in2.value
    }

    val ffadd_result = getArg(ffadd_out)
    val ffmul_result = getArg(ffmul_out)
    val ffsub_result = getArg(ffsub_out)
    val ffmax_result = getArg(ffmax_out)
    val ffdiv_result = getArg(ffdiv_out)
    val ffsqrt_result = getArg(ffsqrt_out)
    val ffpow_result = getArg(ffpow_out)
    val fflt_result = getArg(fflt_out)
    val ffgt_result = getArg(ffgt_out)
    val ffeq_result = getArg(ffeq_out)

    val ffadd_gold = args(0).to[T] + args(1).to[T]
    val ffmul_gold = args(0).to[T] * args(1).to[T]
    val ffsub_gold = args(0).to[T] - args(1).to[T]
    val ffmax_gold = max(args(0).to[T] , args(1).to[T])
    val ffdiv_gold = args(0).to[T] / args(1).to[T]
    val ffsqrt_gold = sqrt(args(0).to[T])
    val ffpow_gold = pow(args(0).to[T], 2)
    val ffgt_gold = args(0).to[T] > args(1).to[T]
    val fflt_gold = args(0).to[T] < args(1).to[T]
    val ffeq_gold = args(0).to[T] == args(1).to[T]

    println("sum: " + ffadd_result + " == " + ffadd_gold)
    println("prod: " + ffmul_result + " == " + ffmul_gold)
    println("sub: " + ffsub_result + " == " + ffsub_gold)
    println("max: " + ffmax_result + " == " + ffmax_gold)
    println("div: " + ffdiv_result + " == " + ffdiv_gold)
    println("sqrt: " + ffsqrt_result + " == " + ffsqrt_gold)
    println("pow: " + ffpow_result + " == " + ffpow_gold)
    println("gt: " + ffgt_result + " == " + ffgt_gold)
    println("lt: " + fflt_result + " == " + fflt_gold)
    println("eq: " + ffeq_result + " == " + ffeq_gold)

    assert (abs(ffsqrt_result - ffsqrt_gold) < 0.0001.to[T], "check Sqrt")
    assert (abs(ffpow_result - ffpow_gold) < 0.0001.to[T], "check power of 2")
    assert (abs(ffdiv_result - ffdiv_gold) < 0.0001.to[T], "check div")
    assert (abs(ffadd_result - ffadd_gold) < 0.0001.to[T], "check add")
    assert (abs(ffmul_result - ffmul_gold) < 0.0001.to[T], "check mul")
    assert (abs(ffsub_result - ffsub_gold) < 0.0001.to[T], "check sub")
    assert (abs(ffmax_result - ffmax_gold) < 0.0001.to[T], "check max")
    assert (ffgt_result == ffgt_gold , "check gt")
    assert (fflt_result == fflt_gold , "check lt")
    assert (ffeq_result == ffeq_gold , "check eq")
    println("PASS: 1 (FloatBasics)")   // will not get here if any assertion fire
  }
}