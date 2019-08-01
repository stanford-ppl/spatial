package spatial.tests.feature.vectors

import spatial.dsl._
import scala.math.pow

@spatial class UserVectors extends SpatialTest {
  type Q8 = FixPt[FALSE,_8,_0]
  type Q32 = FixPt[FALSE,_32,_0]

  def main(args: Array[String]): Unit = {
    /**  fat =  11111110 .... 00110010 00010000 */
    val fat = ArgIn[Q32]
    setArg(fat, scala.List.tabulate(32/8){i => pow(2,i*8) + i}.sum.to[Q32])
    println(r"raw fat: $fat")
    val x1 = ArgOut[Q32]
    val x2 = ArgOut[Q8]
    Accel {
      val A = 10.to[Q8]
      val B = 11.to[Q8]
      val C = 12.to[Q8]
      val vL = Vec.ZeroLast(A, B, C)
      val bitsL = vL.asBits
      val sliceL = bitsL(7::0) // bits from (B,C)   0b1011,1100

      val vB = Vec.ZeroFirst(A, B, C)
      val bitsB = vB.asBits
      val sliceB = bitsB(0::7) // bits from (A,B)   0b1011,1010

      val x = vB(0)

      val asVec = fat.asVec[Q8]
      val vecAsFat = asVec.zipWithIndex{case (v,i) => if (i == 0) (v + 1) else v}.asPacked[Q32]

      println(vL(1::0)) // Should be Vector.ZeroFirst(12,11)
      println(vB(0::1)) // Should be Vector.ZeroFirst(10,11)

      println(bitsL) // Should be 0b1010,1011,1100
      println(bitsB) // Should be 0b1100,1011,1010

      println(sliceL.asBits) // Should be 0b1011,1100
      println(sliceB.asBits) // Should be 0b1011,1010

      println(asVec)
      println(vecAsFat)

      x1 := vecAsFat
      x2 := asVec(0)

      assert(vL(0) == C, "0th element of little vector should be C")
      assert(vL(1) == B, "1st element of little vector should be B")
      assert(vL(2) == A, "2nd element of little vector should be A")

      assert(vB(0) == A, "0th element of big vector should be A")
      assert(vB(1) == B, "1st element of big vector should be B")
      assert(vB(2) == C, "2nd element of big vector should be C")
    }
    /** asVec = 1111 (idx = 0), 1110, ...., 0010, 0001, 0000 (idx = 16) */
    val asVec = fat.asVec[Q8]
    /** vecAsFat = fat */
    val vecAsFat = asVec.zipWithIndex{case (v,i) => if (i == 0) (v + 1) else v}.asPacked[Q32]
    println(r"Host Vec0 = ${asVec(0)}")
    println(r"Host As fat = ${vecAsFat}")
    println(r"Accel Vec0 = ${x2}")
    println(r"Accel As fat = ${x1}")
    assert(asVec(0) == 7)
    assert(vecAsFat == fat + 1)//pow(2,24))
  }
}
