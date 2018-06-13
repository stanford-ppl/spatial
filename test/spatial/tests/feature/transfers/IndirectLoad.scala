package spatial.tests.feature.transfers


import spatial.dsl._

@test class IndirectLoad extends SpatialTest { // This hangs with retime on in SPMV_CRS
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val ids = Array.tabulate(16){i => 32*i}
    val data = Array.tabulate(32*16){i => random[Int](5)}
    val id_dram = DRAM[Int](16)
    val data_dram = DRAM[Int](32*16)
    val result_dram = DRAM[Int](32)
    setMem(id_dram, ids)
    setMem(data_dram, data)
    Accel{
      val id_sram = SRAM[Int](16)
      val data_sram = SRAM[Int](32)
      id_sram load id_dram
      Foreach(8 by 1) {i =>
        val start = id_sram(i)
        val end = id_sram(i+1)
        Parallel{
          Pipe{data_sram load data_dram(start::end)}
        }
        result_dram store data_sram
      }
    }
    val result = getMem(result_dram)
    val gold = Array.tabulate(32){i => data(ids(7) + i)}
    printArray(result, "result")
    printArray(gold, "gold")
    val cksum = gold.zip(result){_==_}.reduce{_&&_}
    assert(cksum)
    println("PASS: " + cksum + " (IndirectLoad)")
  }
}
