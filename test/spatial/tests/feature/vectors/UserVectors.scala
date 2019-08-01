package spatial.tests.feature.vectors

import spatial.dsl._

@spatial class UserVectors extends SpatialTest {
  type Q4 = FixPt[FALSE,_4,_0]
  type Q64 = FixPt[FALSE,_64,_0]

  def main(args: Array[String]): Unit = {
    Accel {
      val A = 10.to[Q4]
      val B = 11.to[Q4]
      val C = 12.to[Q4]
      val vL = Vec.ZeroLast(A, B, C)
      val bitsL = vL.asBits
      val sliceL = bitsL(7::0) // bits from (B,C)   0b1011,1100

      val vB = Vec.ZeroFirst(A, B, C)
      val bitsB = vB.asBits
      val sliceB = bitsB(0::7) // bits from (A,B)   0b1011,1010

      val x = vB(0)

      val fat = 9999.to[Q64]
      val asVec = fat.asVec[Q4]

      println(vL(1::0)) // Should be Vector.ZeroFirst(12,11)
      println(vB(0::1)) // Should be Vector.ZeroFirst(10,11)

      println(bitsL) // Should be 0b1010,1011,1100
      println(bitsB) // Should be 0b1100,1011,1010

      println(sliceL.asBits) // Should be 0b1011,1100
      println(sliceB.asBits) // Should be 0b1011,1010

      println(asVec) // Should be 9999 sliced into 16 elements

      assert(vL(0) == C, "0th element of little vector should be C")
      assert(vL(1) == B, "1st element of little vector should be B")
      assert(vL(2) == A, "2nd element of little vector should be A")

      assert(vB(0) == A, "0th element of big vector should be A")
      assert(vB(1) == B, "1st element of big vector should be B")
      assert(vB(2) == C, "2nd element of big vector should be C")
    }
  }
}
