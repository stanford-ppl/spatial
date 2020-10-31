package spatial.tests.compiler

import spatial.dsl._

@spatial class VecInStruct extends SpatialTest {

  // just used for the Bits
  implicit val VecBits = Vec.fromSeq(Seq(I32(0), I32(0)))
  @struct case class NestedStruct(index: I32, payload: Vec[I32])

  def main(args: Array[String]): Unit = {

    val yIdx = ArgIn[I32]
    val y0 = ArgIn[I32]
    val y1 = ArgIn[I32]

    setArg(yIdx, 0)
    setArg(y0, 1)
    setArg(y1, 2)

    val zIdx = ArgOut[I32]
    val z0 = ArgOut[I32]
    val z1 = ArgOut[I32]
    Accel {
      val y = NestedStruct(yIdx, Vec.fromSeq(Seq(y0, y1)))
      val payload: Vec[I32] = y.payload
      val tmp = NestedStruct(y.index + 1, Vec.fromSeq(Seq(payload(0) + 2, payload(1) + 3)))
      zIdx := tmp.index
      val zPayload = tmp.payload
      z0 := zPayload(0)
      z1 := zPayload(1)
    }

    assert(getArg(zIdx) == 1)
    assert(getArg(z0) == 3)
    assert(getArg(z1) == 5)
  }
}