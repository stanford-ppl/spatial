package spatial.tests.feature.dense

import spatial.dsl._

@spatial class Differentiator extends SpatialTest {
  type T = FixPt[TRUE,_16,_16]

  def main(args: Array[String]): Unit = {
    type T = FixPt[TRUE,_16,_16]
    val coltile = 64
    val data = loadCSV1D[T](s"$DATA/slac/slacsample1d.csv", ",")
    val memcols = ArgIn[Int]
    setArg(memcols, data.length.to[Int])
    val srcmem = DRAM[T](memcols)
    setMem(srcmem, data)
    val dstmem = DRAM[T](memcols)

    val window = 16

    Accel {
      val sr = RegFile[T](1,window)
      val rawdata = SRAM[T](coltile)
      val results = SRAM[T](coltile)
      Foreach(window by 1) { i => results(i) = 0} // Temporary fix to regression screwing up write to address 1 only
      // Work on each tile of a row
      Foreach(memcols by coltile) { c =>
        rawdata load srcmem(c::c+coltile)
        // Scan through tile to get deriv
        Foreach(coltile by 1) { j =>
          // Pipe{sr.reset(j == 0)}
          sr(0,*) <<= rawdata(j)
          val mean_right = Reduce(Reg[T](0.to[T]))(window/2 by 1) { k => sr(0,k) }{_+_} / window.to[T]
          val mean_left = Reduce(Reg[T](0.to[T]))(window/2 by 1) { k => sr(0,k+window/2) }{_+_} / window.to[T]
          val slope = (mean_right - mean_left) / (window/2).to[T]
          val idx = j + c
          results(j) = mux(idx < window, 0.to[T], slope)
        }
        dstmem(c::c+coltile) store results
      }
    }


    // Extract results from accelerator
    val results = getMem(dstmem)

    // // Write answer for first time
    // writeCSV1D(results, s"$DATA/slac/deriv_gold.csv", ",")
    // Read answer
    val gold = loadCSV1D[T](s"$DATA/slac/deriv_gold.csv", ",")

    // Create validation checks and debug code
    printArray(results, "Results:")
    val margin = 0.5.to[T]

    val cksum = gold.zip(results){case (a,b) => abs(a-b) < margin}.reduce{_&&_}
    println("PASS: " + cksum + " (Differentiator) * Look into issue with addr 1 screwing up with --retiming on and --naming off but only with waveforms off in dc6db05")
    assert(cksum)
  }
}
