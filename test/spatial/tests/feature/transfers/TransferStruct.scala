package spatial.tests.feature.transfers


import spatial.dsl._


@test class TransferStruct extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def foo() : Int = {
    type Tup = Tup2[Int, Int]
    val out = ArgOut[Int]
    val dr = DRAM[Tup](10)
    Accel {
      val s = SRAM[Tup](10)
      s(5) = pack(42, 43)
      dr(0::10) store s

      val s1 = SRAM[Tup](10)
      s1 load dr(0::10)
      out := s1(5)._1 * s1(5)._2
    }
    getArg(out)
  }


  def main(args: Array[String]): Unit = {
    val result = foo()
    println(result)
  }
}
