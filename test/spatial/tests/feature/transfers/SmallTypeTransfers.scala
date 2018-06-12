package spatial.tests.feature.transfers

import spatial.dsl._

@test class SmallTypeTransfers extends SpatialTest {  
  override def runtimeArgs: Args = NoArgs

  type T1 = FixPt[FALSE,_8,_8]
  type T2 = FixPt[FALSE,_4,_4]

  def main(args: Array[String]): Void = {
    // Declare SW-HW interface vals
    val N = 256

    val big_pt = ArgOut[T1]
    val big_dst = DRAM[T1](N)
    val big_src = DRAM[T1](N)
    val little_pt = ArgOut[T2]
    val little_dst = DRAM[T2](N)
    val little_src = DRAM[T2](N)

    val init_T1 = Array.tabulate(N){i => 0.5.to[T1] * (i % 4).to[T1]}
    val init_T2 = Array.tabulate(N){i => 0.5.to[T2] * (i % 4).to[T2]}

    setMem(big_src, init_T1)
    setMem(little_src, init_T2)

    Accel {
      val big_sram_load = SRAM[T1](N)
      val big_sram_store = SRAM[T1](N)
      val little_sram_load = SRAM[T2](N)
      val little_sram_store = SRAM[T2](N)

      Foreach(N by 1){i => 
        big_sram_store(i) = 0.5.to[T1] *& (i % 4).to[T1]
        little_sram_store(i) = 0.5.to[T2] *& (i % 4).to[T2]
      }

      big_sram_load load big_src
      little_sram_load load little_src

      big_pt := big_sram_load(5)
      little_pt := little_sram_load(5)
      big_dst store big_sram_store
      little_dst store little_sram_store
    }


    // Extract results from accelerator
    val big_pt_res = getArg(big_pt)
    val little_pt_res = getArg(little_pt)
    val big_sram_store_res = getMem(big_dst)
    val little_sram_store_res = getMem(little_dst)

    printArray(big_sram_store_res, "Big store")
    printArray(init_T1, "Big gold")
    printArray(little_sram_store_res, "Little store")
    printArray(init_T2, "Little gold")
    println("big_pt: " + big_pt_res + " ( =?= " + init_T1(5) + " )")
    println("little_pt: " + little_pt_res + " ( =?= " + init_T2(5) + " )")

    println("Big Store: " + big_sram_store_res.zip(init_T1){_==_}.reduce{_&&_})
    println("Little Store: " + little_sram_store_res.zip(init_T2){_==_}.reduce{_&&_})
    println("BigPt: " + {big_pt_res == init_T1(5)})
    println("LittlePt: " + {little_pt_res == init_T2(5)})

    val cksum = big_sram_store_res.zip(init_T1){_==_}.reduce{_&&_} && little_sram_store_res.zip(init_T2){_==_}.reduce{_&&_} && big_pt_res == init_T1(5) && little_pt_res == init_T2(5)

    println("PASS: " + cksum + " (SmallTypeTransfers)")

    assert(cksum)
  }
}
