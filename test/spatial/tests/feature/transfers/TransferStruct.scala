package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class TransferStruct extends SpatialTest {
  @struct case class mystruct(el1: Int, el2: Int)  

  def foo() : Int = {
    val out = ArgOut[Int]
    val dr = DRAM[mystruct](10)
    Accel {
      val s = SRAM[mystruct](10)
      s(5) = mystruct(42, 43)
      dr(0::10) store s

      val s1 = SRAM[mystruct](10)
      s1 load dr(0::10)
      out := s1(5).el1 * s1(5).el2
    }
    val x = getArg(out)
    assert(x == 42*43)
    x
  }


  def main(args: Array[String]): Unit = {
    val result = foo()
    println(result)
  }
}
