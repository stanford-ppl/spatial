package spatial.tests.feature.vectors

import spatial.dsl._

@test class TwiddlingWithStructs extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type UInt8 = FixPt[FALSE,_8,_0]
  type SInt8 = FixPt[TRUE,_8,_0]

  @struct case class UBytes(a: UInt8, b: UInt8, c: UInt8, d: UInt8)
  @struct case class SBytes(a: SInt8, b: SInt8, c: SInt8, d: SInt8)


  def main(args: Array[String]): Unit = {
    Accel {
      val x = 255.to[Int]
      println(x.asBits)
      val y = x.as[UBytes]
      println("ua: " + y.a)
      println("ub: " + y.b)
      println("uc: " + y.c)
      println("ud: " + y.d)
      assert(y.d == 255.to[UInt8], "d should be 255 (0b11111111)")
      assert(y.c == 0.to[UInt8], "c should be 0")
      assert(y.b == 0.to[UInt8], "b should be 0")
      assert(y.a == 0.to[UInt8], "a should be 0")

      val z = x.as[SBytes]
      println("sa: " + z.a)
      println("sb: " + z.b)
      println("sc: " + z.c)
      println("sd: " + z.d)
      assert(z.d == -1.to[SInt8], "d should be -1 (0b11111111)")
    }
  }
}