package spatial.tests.feature.dense

import spatial.dsl._

@spatial class Differentiator extends SpatialTest {
  override def dseModelArgs: Args = "1024"
  override def finalModelArgs: Args = "1024"

  // Set low pass filter window size
  val window: scala.Int = 16

  def compute_kernel[T:Num](start: scala.Int, sr: RegFile1[T]): T = {
    Reduce(Reg[T](0.to[T]))(window/2 by 1) { k => sr(k+start) }{_+_}.value / window
  }

  def main(args: Array[String]): Unit = {
    type T = FixPt[TRUE,_16,_16]
    
    // Set tile size
    val coltile = 64
    
    // Load data
    val data = loadCSV1D[T](s"$DATA/slac/slacsample1d.csv", ",")
    
    // Set full size of input vector for use by FPGA
    val memcols = ArgIn[Int]
    setArg(memcols, data.length.to[Int])
    
    // Create input and output DRAMs
    val srcmem = DRAM[T](memcols)
    setMem(srcmem, data)
    val dstmem = DRAM[T](memcols)

    Accel {
      // Create shift register window
      val sr = RegFile[T](window)
    
      // Allocate memories for input and output data
      val rawdata = SRAM[T](coltile)
      val results = SRAM[T](coltile)

      // Work tile by tile on input vector
      Foreach(memcols by coltile) { c =>
        
        // Fetch this tile
        rawdata load srcmem(c::c+coltile)
                                   
        // Scan through tile to get deriv
        Foreach(coltile by 1) { j =>
        
          // Shift next element into sliding window
          sr <<= rawdata(j)
          
          // Compute mean of points in first half of window
          val mean_right = compute_kernel[T](0, sr)
                               
          // Compute mean of points in second half of window
          val mean_left = compute_kernel[T](window/2, sr)
                       
          // Subtract and average
          val slope = (mean_right - mean_left) / (window/2).to[T]
          
          // Store result (if all data in window is valid)
          val idx = j + c
          results(j) = mux(idx < window, 0.to[T], slope)
        }
        dstmem(c::c+coltile) store results
      }
    }


    // Extract results from accelerator
    val results = getMem(dstmem)

    // Read answer
    val gold = loadCSV1D[T](s"$DATA/slac/deriv_gold.csv", ",")

    // Create validation checks and debug code
    printArray(results, "Results:")
    val margin = 0.5.to[T]

    val cksum = gold.zip(results){case (a,b) => abs(a-b) < margin}.reduce{_&&_}
    assert(cksum)
  }
}

