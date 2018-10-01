package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class FloatTransfer extends SpatialTest {
  type T1 = Float

  def main(args: Array[String]): Void = {
    // Declare SW-HW interface vals
    val N = 256

    val big_pt = ArgOut[T1]
    val big_dst = DRAM[T1](N)
    val big_src = DRAM[T1](N)

    val init_T1 = Array.tabulate(N){i => 0.25.to[T1] * (i % 8).to[T1]}

    setMem(big_src, init_T1)

    Accel {
      val big_sram_load = SRAM[T1](N)
      val big_sram_store = SRAM[T1](N)

      Foreach(N by 1){i => 
        big_sram_store(i) = 0.25.to[T1] * (i % 8).to[T1]
      }

      big_sram_load load big_src

      big_pt := big_sram_load(5)
      big_dst store big_sram_store
    }


    // Extract results from accelerator
    val big_pt_res = getArg(big_pt)
    val big_sram_store_res = getMem(big_dst)

    printArray(big_sram_store_res, "Big store")
    printArray(init_T1, "Big gold")
    println("big_pt: " + big_pt_res + " ( =?= " + init_T1(5) + " )")

    println("Big Store: " + big_sram_store_res.zip(init_T1){_==_}.reduce{_&&_})
    println("BigPt: " + {big_pt_res == init_T1(5)})

    val cksum = big_sram_store_res.zip(init_T1){_==_}.reduce{_&&_} && big_pt_res == init_T1(5)

    println("PASS: " + cksum + " (FloatTransfer)")

    assert(cksum)
  }
}
