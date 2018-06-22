package spatial.tests.feature.transfers


import spatial.dsl._


@test class DRAMBuffer extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    type T = Int32

    val dram_in = DRAM[T](16)
    val dram_intermediate = DRAM[T](16)
    val dram_out = DRAM[T](16,16)
    val dummy = ArgOut[T]

    val src = Array.tabulate[T](16){i => 0.to[T]}
    setMem(dram_in, src)

    Accel {
      val x1 = SRAM[T](16)
      val x2 = SRAM[T](16)
      Foreach(16 by 1){i => 
        x1 load dram_in
        Foreach(16 by 1){j => x1(j) = x1(j) + i}
        dram_intermediate store x1
        Foreach(16 by 1){j => dummy := x1(j)}
        x2 load dram_intermediate
        dram_out(i,0::16) store x2
      }
    }

    val gold = (0::16, 0::16){(i,j) => i}
    printMatrix(getMatrix(dram_out), "DRAM Out")
    printMatrix(gold, "Gold")
    println(r"Pass? ${getMatrix(dram_out) == gold}")
    assert(getMatrix(dram_out) == gold)
  }
}
