package spatial.tests.feature.transfers


import spatial.dsl._


@test class UnalignedLoadStore2D extends SpatialTest {
  override def runtimeArgs: Args = "400"


  def main(args: Array[String]): Unit = {

    val src1 = (0::16,0::16){(i,j) => i}
    val dram1 = DRAM[Int](16,16)
    val dram2 = DRAM[Int](16,16)

    setMem(dram1,src1)

    Accel {
      val sram1 = SRAM[Int](16)
      Foreach(-5 until 15 by 1){i =>
        val ldrow: Int = if ((i.to[Int] + 1.to[Int]) >= 0.to[Int] && (i.to[Int] + 1.to[Int]) <= 15) {i.to[Int] + 1.to[Int]} else 0
        sram1 load dram1(ldrow,0::16)
        dram2(ldrow,0::16) store sram1
      }
    }
    val out = getMatrix(dram2)
    printMatrix(out, "Result:")
    printMatrix(src1, "Gold:")
    assert(out == src1)
  }
}
