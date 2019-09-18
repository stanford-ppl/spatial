package spatial.tests.feature.memories.lut

import spatial.dsl._

@spatial class LUTSimple extends SpatialTest {
  override def runtimeArgs: Args = "2"

  //type T = FixPt[TRUE,_32,_32]
  type T = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val i = ArgIn[Int]
    val y = ArgOut[T]
    val ii = args(0).to[Int]

    // Connect SW vals to HW vals
    setArg(i, ii)

    // Create HW accelerator
    Accel {
      val lut = LUT[T](4, 4)(
        0,  (1*1E0).to[T],  2,  3,
        4,  -5,  6,  7,
        8,  9, -10, 11,
        12, 13, 14, -15
      )
      val red = Reduce(Reg[T](0))(3 by 1 par 3) {q =>
        lut(q,q)
      }{_^_}
      y := lut(1, 3) ^ lut(3, 3) ^ red ^ lut(i,0)
    }


    // Extract results from accelerator
    val result = getArg(y)

    // Create validation checks and debug code
    val gold = (-15 ^ 7 ^ -0 ^ -5 ^ -10 ^ 4*ii).to[T]
    println("expected: " + gold)
    println("result: " + result)

    val cksum = gold == result
    println("PASS: " + cksum + " (LUTSimple)")
    assert(gold == result)
  }
}

@spatial class LUTFromFile extends SpatialTest {
  override def runtimeArgs: Args = "2"

  type T = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val filename = "lut/data.csv"
    val y = ArgOut[T]
    val i = ArgIn[Int]
    val ii = args(0).to[Int]

    // Connect SW vals to HW vals
    setArg(i, ii)

    // Create HW accelerator
    Accel {
      val lut = LUT.fromFile[T](256)(s"${DATA}/$filename")
      y := lut(i)
    }


    // Extract results from accelerator
    val result = getArg(y)

    val gold = loadCSV1D[T](s"${DATA}/$filename").apply(ii)

    println("expected: " + gold)
    println("result: " + result)

    val cksum = gold == result
    println("PASS: " + cksum + " (LUTFromFile)")
    assert(gold == result)
  }
}
