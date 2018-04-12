package spatial.test.full

import spatial.test.Testbench

//
//@spatial object BitSelects {
//
//
//  def main(): Void = {
//    Accel {
//      val x = 9.to[Int]
//      val v = x.asBits
//      println(v)
//      println("bit 0: " + x.bit(0))
//      println("bit 1: " + x.bit(1))
//      println("bit 2: " + x.bit(2))
//      println("bit 3: " + x.bit(3))
//
//      assert(x(0), "First bit should be 1")
//      assert(!x(1), "Second bit should be 0")
//      assert(!x(2), "Third bit should be 0")
//      assert(x(3), "Fourth bit should be 1")
//    }
//  }
//}
//
//@spatial object UserVectors {
//  type Q4 = FixPt[FALSE,_4,_0]
//
//  def main(): Void = {
//    Accel {
//      val A = 10.to[Q4]
//      val B = 11.to[Q4]
//      val C = 12.to[Q4]
//      val vL = Vec.ZeroLast(A, B, C)
//      val bitsL = vL.as12b
//      val sliceL = bitsL(7::0) // bits from (B,C)   0b1011,1100
//
//      val vB = Vec.ZeroFirst(A, B, C)
//      val bitsB = vB.as12b
//      val sliceB = bitsB(0::7) // bits from (A,B)   0b1011,1010
//
//      val x = vB(0)
//
//      println(vL(1::0).asVector2) // Should be Vector.ZeroFirst(12,11)
//      println(vB(0::1).asVector2) // Should be Vector.ZeroFirst(10,11)
//
//      println(bitsL) // Should be 0b1010,1011,1100
//      println(bitsB) // Should be 0b1100,1011,1010
//
//      println(sliceL.as8b) // Should be 0b1011,1100
//      println(sliceB.as8b) // Should be 0b1011,1010
//
//      assert(vL(0) == C, "0th element of little vector should be C")
//      assert(vL(1) == B, "1st element of little vector should be B")
//      assert(vL(2) == A, "2nd element of little vector should be A")
//
//      assert(vB(0) == A, "0th element of big vector should be A")
//      assert(vB(1) == B, "1st element of big vector should be B")
//      assert(vB(2) == C, "2nd element of big vector should be C")
//    }
//  }
//}
//
//object TwiddlingWithStructs extends SpatialTest {
//  import spatial.dsl._
//
//  type UInt8 = FixPt[FALSE,_8,_0]
//  type SInt8 = FixPt[TRUE,_8,_0]
//
//  @struct class UBytes(a: UInt8, b: UInt8, c: UInt8, d: UInt8)
//  @struct class SBytes(a: SInt8, b: SInt8, c: SInt8, d: SInt8)
//
//  @virtualize
//  def main() {
//    Accel {
//      val x = 255.to[Int]
//      println(x.as32b)
//      val y = x.as[UBytes]
//      println("ua: " + y.a)
//      println("ub: " + y.b)
//      println("uc: " + y.c)
//      println("ud: " + y.d)
//      assert(y.d == 255.to[UInt8], "d should be 255 (0b11111111)")
//      assert(y.c == 0.to[UInt8], "c should be 0")
//      assert(y.b == 0.to[UInt8], "b should be 0")
//      assert(y.a == 0.to[UInt8], "a should be 0")
//
//      val z = x.as[SBytes]
//      println("sa: " + z.a)
//      println("sb: " + z.b)
//      println("sc: " + z.c)
//      println("sd: " + z.d)
//      assert(z.d == -1.to[SInt8], "d should be -1 (0b11111111)")
//    }
//  }
//}
//
//object ShiftTest extends SpatialTest {
//  import spatial.dsl._
//  testArgs = List("-14", "2")
//
//  @virtualize def main(): Unit = {
//    val x = ArgIn[Int]
//    val m = ArgIn[Int]
//    setArg(x, args(0).to[Int])
//    setArg(m, args(1).to[Int])
//    Accel {
//      val lsh = x << m
//      val rsh = x >> m
//      val ursh = x >>> m
//      assert(lsh == -56, "lsh: " + lsh + ", expected: -56")
//      assert(rsh == -4, "rsh: " + rsh + ", expected: -3")
//      assert(ursh == 1073741820, "ursh: " + ursh + ", expected: 1073741820")
//    }
//  }
//}

class BitTwiddling extends Testbench {
//  "Bit selection" should "compile" in { BitSelects.main(Array.empty) }
//  "UserVectors" should "compile" in { UserVectors.main(Array.empty) }
//  "TwiddlingWithStructs" should "compile" in { TwiddlingWithStructs.main(Array.empty) }
//  "ShiftTest" should "compile" in { ShiftTest.main(Array.empty) }
}
