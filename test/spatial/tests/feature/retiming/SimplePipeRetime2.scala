package spatial.tests.feature.retiming

import spatial.dsl._

@spatial class SimplePipeRetime2 extends SpatialTest {

  def main(args: Array[String]): Unit = {
    // add one to avoid dividing by zero
    val a = random[Int](10) + 1
    val b = random[Int](10) + 1

    val aIn = ArgIn[Int]
    val bIn = ArgIn[Int]
    setArg(aIn, a)
    setArg(bIn, b)

    val out1 = ArgOut[Int]
    val out2 = ArgOut[Int]
    Accel {
      out1 := (aIn * bIn) + aIn
      out2 := (aIn / bIn) + aIn
    }
    val gold1 = (a * b) + a
    val gold2 = (a / b) + a
    val cksum = gold1 == getArg(out1) && gold2 == getArg(out2)
    assert(cksum)
  }
}
