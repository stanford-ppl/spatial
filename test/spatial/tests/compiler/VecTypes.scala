package spatial.tests.compiler
import spatial.dsl._
@spatial class VecInStruct extends SpatialTest {
  // just used for the Bits
  implicit val VecBits = Vec.fromSeq(Seq(I32(0), I32(0)))
  @struct case class NestedStruct(index: I32, payload: Vec[I32])
  def main(args: Array[String]): Unit = {
    val y_index = ArgIn[I32]
    val y_payload0 = ArgIn[I32]
    val y_payload1 = ArgIn[I32]
    val z_index = ArgOut[I32]
    val z_payload0 = ArgOut[I32]
    val z_payload1 = ArgOut[I32]
    setArg(y_index, 0)
    setArg(y_payload0, 1)
    setArg(y_payload1, 2)
    Accel {
      val intermediate = NestedStruct(y_index + 1, Vec.fromSeq(Seq(y_payload0 + 2, y_payload1 + 3)))
      z_index := intermediate.index
      val v = intermediate.payload
      z_payload0 := v(0)
      z_payload1 := v(1)
    }
    println("z_index: " + getArg(z_index))
    println("z_payload0: " + getArg(z_payload0))
    println("z_payload1: " + getArg(z_payload1))
    assert(z_index == 1)
    assert(z_payload0 == 3)
    assert(z_payload1 == 5)
  }
}

@spatial class FIFOVecType extends SpatialTest {
  implicit val tV: Vec[U16] = Vec.bits[U16](2)
	@struct case class VEC(s : U16, v : Vec[U16])
	@struct case class SCAL(s1 : U16, s2 : U16)

  def main(args: Array[String]): Unit = {

    val tileSize = 64

    val dstFPGA = DRAM[I64](8)
    val out = ArgOut[U16]

    Accel {
      val f1 = FIFO[SCAL](8)
      val f2 = FIFO[VEC](8)
      Foreach(8 by 1) { i =>
        // Put 8 elements in FIFO
        f1.enq(SCAL(i.to[U16], (8 - i).to[U16]))
        f2.enq(VEC(i.to[U16], Vec.ZeroFirst[U16](i.to[U16], (8 - i).to[U16])))
      }
      Foreach(8 by 1) { i =>
        val x1 = f1.deq()
        val x2 = f2.deq()
        out := x1.s1 + x1.s2 + x2.s + x2.v.apply(0) * 2 + x2.v.apply(1)
      }
    }
    val dst = getArg(out)

    val gold = 7 + 1 + 7 + 7 * 2 + 1

    println(r"Wanted: $gold")
    println(r"Got: $dst")

    println(r"PASS: ${gold == dst} (FIFOVecType)")
    assert(gold == dst)
  }
}




@spatial class bad extends SpatialTest {
  // just used for the Bits
  implicit val VecBits = Vec.fromSeq(Seq(I32(0), I32(0)))
  @struct case class NestedStruct(index: I32, payload: Vec[I32])
  def main(args: Array[String]): Unit = {
    val y = ArgIn[NestedStruct]
    val z = ArgOut[NestedStruct]
    setArg(y, NestedStruct(I32(0), Vec.fromSeq(Seq(I32(0), I32(1)))))
    Accel {
      val payload: Vec[I32] = y.payload
      z := NestedStruct(y.index + 1, Vec.fromSeq(Seq(payload(0) + 2, payload(2) + 3)))
    }
    println("z: " + getArg(z))
    val zRes = getArg(z)
    val zPayload: Vec[I32] = zRes.payload
    assert(zRes.index == 1)
    assert(zPayload(0) == 2)
    assert(zPayload(1) == 3)
  }
}