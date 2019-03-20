package spatial.tests.feature.dense

import spatial.dsl._

@spatial class EdgeDetector extends SpatialTest {
  override def dseModelArgs: Args = "16 1024"
  override def finalModelArgs: Args = "16 1024"

  type T = FixPt[TRUE,_16,_16]

  def main(args: Array[String]): Unit = {
    type T = FixPt[TRUE,_16,_16]
    val rowtile = 16
    val coltile = 64
    val data = loadCSV2D[T](s"$DATA/slac/slacsample2d.csv", ",", "\n")
    val memrows = ArgIn[Int]
    val memcols = ArgIn[Int]
    setArg(memrows, data.rows.to[Int])
    setArg(memcols, data.cols.to[Int])
    val srcmem = DRAM[T](memrows, memcols)
    setMem(srcmem, data)
    val risingEdges = DRAM[Int](memrows)
    // val fallingEdges = DRAM[Int](memrows)

    val window = 16

    Accel {
      val sr = RegFile[T](1,window)
      val rawdata = SRAM[T](coltile)
      val results = SRAM[Int](rowtile)
      // Work on each row
      Sequential.Foreach(memrows by rowtile) { r =>
        Sequential.Foreach(rowtile by 1) { rr =>
          // Work on each tile of a row
          val globalMax = Reduce(Reg[Tuple2[Int,T]](pack(0.to[Int], -1000.to[T])))(memcols by coltile) { c =>
            // Load tile from row
            rawdata load srcmem(r + rr, c::c+coltile)
            // Scan through tile to get deriv
            val localMax = Reduce(Reg[Tuple2[Int,T]](pack(0.to[Int], -1000.to[T])))(coltile by 1) { j =>
              sr(0,*) <<= rawdata(j)
              val mean_right = Reduce(Reg[T](0.to[T]))(window/2 by 1) { k => sr(0,k) }{_+_} / window.to[T]
              val mean_left = Reduce(Reg[T](0.to[T]))(window/2 by 1) { k => sr(0,k+window/2) }{_+_} / window.to[T]
              val slope = (mean_right - mean_left) / (window/2).to[T]
              val idx = j + c
              mux(idx < window, pack(idx, 0.to[T]), pack(idx,slope))
            }{(a,b) => mux(a._2 > b._2, a, b)}
            localMax
          }{(a,b) => mux(a._2 > b._2, a, b)}
          results(rr) = globalMax._1
        }
        risingEdges(r::r+rowtile) store results
      }
    }


    // Extract results from accelerator
    val results = getMem(risingEdges)
    val gold = loadCSV1D[Int](s"$DATA/slac/edge_gold.csv", ",")
    val margin = 2.to[Int]

    // Create validation checks and debug code
    printArray(results, "Results:")

    val cksum = results.zip(gold) {case (a,b) => a < b + margin && a > b - margin}.reduce{_&&_}
    println("PASS: " + cksum + " (EdgeDetector)")
    assert(cksum)
  }
}
