package spatial.tests.compiler

import spatial.dsl._

@spatial class ComputedPoint extends SpatialTest {
  def main(args: Array[String]) = {

    val fixType = Fix.createFixType(true, 3, 13)

    // Needed to make casting work
    type sEVT = argon.lang.BOOL[fixType.FixType#ST]
    implicit def bEV: sEVT = fixType.fmt.S.asInstanceOf[sEVT]
    type iEVT = argon.lang.INT[fixType.FixType#IT]
    implicit def iEV: iEVT = fixType.fmt.I.asInstanceOf[iEVT]
    type fEVT = argon.lang.INT[fixType.FixType#FT]
    implicit def fEV: fEVT = fixType.fmt.I.asInstanceOf[fEVT]

    // Needed to make storage work
    implicit def bitsEV: Bits[fixType.FixType] = fixType.asInstanceOf[Bits[fixType.FixType]]

    val arg0 = ArgIn[fixType.FixType]
    val arg1 = ArgIn[Float]

    val argOut = ArgOut[fixType.FixType]

    setArg(arg0, args(0).to[fixType.FixType])
    setArg(arg1, args(1).to[Float])


    Accel {
      val v0 = arg0.value
      val v1 = arg1.value.to[fixType.FixType]
      argOut := v0.asInstanceOf[fixType.FixType] + v1.asInstanceOf[fixType.FixType]
    }

    println(r"$arg0 + $arg1 = $argOut")
  }
}
