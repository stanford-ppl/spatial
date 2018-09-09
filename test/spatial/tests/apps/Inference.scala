package spatial.tests.apps

import spatial.dsl._

@spatial class SingleLayerConv_RCIO extends SpatialTest {
  override def runtimeArgs: Args = "16 32 32 32 2 0"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]
  type REALT = FixPt[TRUE,_4,_12]
  type REALT2 = FixPt[TRUE,_8,_24]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Unsafe parallelization if OC < 16 (1 burst) because multiple writers may perform unaligned store to same burst simultaneously
    val P2 = 1 //2 // Unsafe parallelization if OC < 16 (1 burst) because multiple writers may perform unaligned store to same burst simultaneously
    val P3 = 2 //2
    val P4 = 2 //2
    val P5 = 2 //4
    val P6 = 1 //16
    val loadPar = 16 (1 -> 16)
    val storePar = 16 (1 -> 16)
    // Scalar params
    val INPUT_ROWS = ArgIn[Int]
    val INPUT_COLS = ArgIn[Int]
    val INPUT_CHANS = ArgIn[Int]
    val OUTPUT_CHANS = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val input_rows = args(0).to[Int]
    val input_cols = args(1).to[Int]
    val input_chans = args(2).to[Int]
    val output_chans = args(3).to[Int]
    val stride = args(4).to[Int]
    val print_data = args(5).to[Bit]

    if (P1 > 1) assert(output_chans > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(INPUT_ROWS, input_rows)
    setArg(INPUT_COLS, input_cols)
    setArg(INPUT_CHANS, input_chans)
    setArg(OUTPUT_CHANS, output_chans)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INPUT_CHANS_MAX = 96
    val OUTPUT_CHANS_MAX = 96

    // Memories
    val INPUT_DATA = DRAM[T](INPUT_COLS, INPUT_ROWS, INPUT_CHANS)
    val OUTPUT_DATA = DRAM[T]((INPUT_COLS-(KERNEL_COLS-STRIDE))/STRIDE, (INPUT_ROWS-(KERNEL_ROWS-STRIDE))/STRIDE, OUTPUT_CHANS)
    val KERNEL_DATA = DRAM[T](OUTPUT_CHANS, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS)
    val BIAS_DATA =   DRAM[T](OUTPUT_CHANS)

    // Load data (placeholder)
    val input = (0::INPUT_COLS, 0::INPUT_ROWS, 0::INPUT_CHANS) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::(INPUT_COLS-(KERNEL_COLS-STRIDE))/STRIDE, 0::(INPUT_ROWS-(KERNEL_ROWS-STRIDE))/STRIDE, 0::OUTPUT_CHANS){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTPUT_CHANS, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INPUT_CHANS) {(i,j,k,l) => if (random[Int](10) > 7) 64.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTPUT_CHANS){i => 1.to[T]}

    printTensor3(input, "Input")
    printTensor4(kernel, "Kernel")
    printArray(bias, "Bias")

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    // setMem(KERNEL_COPY_CPU, kernel)
    // println("MANUALLY COPY KERNEL_DATA PTR TO ME!")

    Accel{
      val kernel_sram = SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX)
      val bias_sram = SRAM[T](OUTPUT_CHANS_MAX)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTPUT_CHANS, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INPUT_CHANS)
        bias_sram load BIAS_DATA(0::OUTPUT_CHANS)        
      }
      val rowspan = if (STRIDE.value == 1) INPUT_ROWS-(KERNEL_ROWS-STRIDE) else (INPUT_ROWS-(KERNEL_ROWS-STRIDE) >> 1)
      val colspan = if (STRIDE.value == 1) INPUT_COLS-(KERNEL_COLS-STRIDE) else (INPUT_COLS-(KERNEL_COLS-STRIDE) >> 1)
      Foreach(rowspan par P1){ R =>
        val row = R*STRIDE.value
        Foreach(colspan par P2){ C =>
          val col = C*STRIDE.value
          val local_data = SRAM[T](3,3,INPUT_CHANS_MAX)
          val accum_line_upcast = SRAM[T2](OUTPUT_CHANS_MAX)
          val accum_line = SRAM[T](OUTPUT_CHANS_MAX)
          local_data load INPUT_DATA(col::col+3, row::row+3, 0::INPUT_CHANS par loadPar)
          MemReduce(accum_line_upcast(0::OUTPUT_CHANS par P5))(INPUT_CHANS by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTPUT_CHANS_MAX)
            Foreach(OUTPUT_CHANS by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,j,i,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                local_data(j,i,ic).to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduce{_+_}
              local_accum_line(oc) = accum
              // if (debug) println(" at " + oc + "," + R + "," + C + " = " + filter_elements(0) + " * " + data_elements(0) + " + " +
              //                     filter_elements(1) + " * " + data_elements(1) + " + " +
              //                     filter_elements(2) + " * " + data_elements(2) + " + " +
              //                     filter_elements(3) + " * " + data_elements(3) + " + " +
              //                     filter_elements(4) + " * " + data_elements(4) + " + " +
              //                     filter_elements(5) + " * " + data_elements(5) + " + " +
              //                     filter_elements(6) + " * " + data_elements(6) + " + " +
              //                     filter_elements(7) + " * " + data_elements(7) + " + " +
              //                     filter_elements(8) + " * " + data_elements(8)
              //                   )
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTPUT_CHANS by 1 par P6){i => 
            val bitshifted = if (accum_line_upcast(i) < (-134217728).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(i) > (134217727).to[T2]) 32767.to[T]
                             else accum_line_upcast(i).bits(27::12).as[T]
            accum_line(i) = max(0.to[T], bitshifted +! bias_sram(i))
          }
          OUTPUT_DATA(col/STRIDE,row/STRIDE,0::OUTPUT_CHANS par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

    // Compute Checks
    val gold = (0::(INPUT_COLS-(KERNEL_COLS-STRIDE)) / STRIDE, 0::(INPUT_ROWS-(KERNEL_ROWS-STRIDE)) / STRIDE, 0::OUTPUT_CHANS){(j,i,k) => 
      val el = Array.tabulate(INPUT_CHANS){page => 
        if (debug && print_data) {
            println("Result working on " + k + "," + page + "," + i + "," + j)
            println(" Window: ")
            for (ii <- 0 until KERNEL_ROWS) { 
              for (jj <- 0 until KERNEL_COLS){
                print(" " + input(i*STRIDE+ii,j*STRIDE+jj, page))
              } 
              println(" ")
            }
        }


        Array.tabulate(KERNEL_COLS){jj => Array.tabulate(KERNEL_ROWS){ii => 
          val pxl = input(j*STRIDE+jj,i*STRIDE+ii, page)
          val f = kernel(k, ii, jj, page)
          if (debug && print_data && f != 0.to[T]) println(" Partial is " + pxl + " * " + f + " @ " + ii + "," + jj)
          pxl.to[T2] * f.to[T2]
        }}.flatten.reduce{_+_}
      }.reduce{_+_} >> 12
      if (el.to[T] < 0.to[T]) 0.to[T] else {el.to[T] + bias(k)}
    }
  
    if (print_data) {
      printTensor3(input, "Input")
      printTensor4(kernel, "Kernel")
      printTensor3(gold, "Gold")
      printTensor3(results, "Extracted")  

      val bitmask = (0::(INPUT_COLS-(KERNEL_COLS-STRIDE)) / STRIDE, 0::(INPUT_ROWS-(KERNEL_ROWS-STRIDE)) / STRIDE, 0::OUTPUT_CHANS){(j,i,k) =>
        if (results(i,j, k) == gold(i,j,k)) 1.to[Int] else 0.to[Int]
      }
      val num_wrong = bitmask.length - bitmask.reduce{_+_}
      printTensor3(bitmask, "Matchup")
      println("Error rate: " + num_wrong + " / " + bitmask.length + " incorrect")

    }
    val cksum = gold.zip(results){_==_}.reduce{_&&_}    

    println("PASS: " + cksum + " (SingleLayerConv_RCIO)")
    assert(cksum)

  }
}

@spatial class SingleLayerConv_RCIO_NoPad extends SpatialTest{
  override def runtimeArgs: Args = "31 63 15 31 2 15 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 //2
    val P4 = 1 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val loadPar = 1 (1 -> 16)
    val storePar = 1 (1 -> 16)
    // Scalar params
    val IMAX_0 = ArgIn[Int]
    val IMAX_1 = ArgIn[Int]
    val OMAX_0 = ArgIn[Int]
    val OMAX_1 = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val imax_0 = args(0).to[Int]
    val imax_1 = args(1).to[Int]
    val omax_0 = args(2).to[Int]
    val omax_1 = args(3).to[Int]
    val inchans_max = args(4).to[Int]
    val outchans_max = args(5).to[Int]
    val stride = args(6).to[Int]
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(IMAX_0, imax_0 + 1)
    setArg(IMAX_1, imax_1 + 1)
    setArg(OMAX_0, omax_0 + 1)
    setArg(OMAX_1, omax_1 + 1)
    setArg(INCHANS_MAX, inchans_max + 1)
    setArg(OUTCHANS_MAX, outchans_max + 1)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INCHANS_MAX_UPPERBOUND = 96
    val OUTCHANS_MAX_UPPERBOUND = 96

    // Memories
    val INPUT_DATA = DRAM[T](IMAX_0, IMAX_1, INCHANS_MAX)
    val OUTPUT_DATA = DRAM[T](OMAX_0, OMAX_1, OUTCHANS_MAX)
    val KERNEL_DATA = DRAM[T](OUTCHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX)
    val BIAS_DATA =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    printTensor3(input.reorder(Seq(2,1,0)), "Input")
    printTensor4(kernel.reorder(Seq(0,3,2,1)), "Kernel")
    printArray(bias, "Bias")

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    // setMem(KERNEL_COPY_CPU, kernel)
    // println("MANUALLY COPY KERNEL_DATA PTR TO ME!")

    Accel{
      val kernel_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND, KERNEL_COLS, KERNEL_ROWS, INCHANS_MAX_UPPERBOUND)
      val bias_sram = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX)
        bias_sram load BIAS_DATA(0::OUTCHANS_MAX)        
      }
      Foreach(OMAX_0 par P1){ C =>
        Foreach(OMAX_1 par P2){ R =>
          val idx0 = C*STRIDE
          val idx1 = R*STRIDE
          val local_data = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND)
          val accum_line_upcast = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
          val accum_line = SRAM[T](OUTCHANS_MAX_UPPERBOUND)
          Foreach(0 until 3 by 1, 0 until 3 by 1){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i::i+1,j::j+1,0::INCHANS_MAX) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::INCHANS_MAX par loadPar)
          }
          MemReduce(accum_line_upcast(0::OUTCHANS_MAX par P5))(INCHANS_MAX by 1 par P3){ ic =>
            val local_accum_line = SRAM[T2](OUTCHANS_MAX_UPPERBOUND)
            Foreach(OUTCHANS_MAX by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,i,j,ic).to[T2]
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (idx0 - 1 + i >= 0 && idx0 - 1 + i < IMAX_0.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < IMAX_1) local_data(i,j,ic).to[T2] else 0.to[T2]
              }}.flatten
              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduce{_+_}
              local_accum_line(oc) = accum
              println(r" at $oc $R $C = ${filter_elements(0)} ${filter_elements(3)} ${filter_elements(6)}    ${data_elements(0)} ${data_elements(3)} ${data_elements(6)} ")
              println(r"            ${filter_elements(1)} ${filter_elements(4)} ${filter_elements(7)}    ${data_elements(1)} ${data_elements(4)} ${data_elements(7)}")
              println(r"            ${filter_elements(2)} ${filter_elements(5)} ${filter_elements(8)}    ${data_elements(2)} ${data_elements(5)} ${data_elements(8)}") 
            }
            local_accum_line
          }{_+_}
          // RELU
          Foreach(OUTCHANS_MAX by 1 par P6){i => 
            val bitshifted = if (accum_line_upcast(i) < (-134217728).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(i) > (134217727).to[T2]) 32767.to[T]
                             // else accum_line_upcast(i).bits(27::12).as[T]
                             else accum_line_upcast(i).bits(15::0).as[T]
            println(r"  bitshifted @ outchan $i = max(0, ${bitshifted +! bias_sram(i)}")
            accum_line(i) = max(0.to[T], bitshifted +! bias_sram(i))
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

    // Compute Checks
    val gold = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 
      val el = Array.tabulate(INCHANS_MAX){page => 
        Array.tabulate(KERNEL_COLS){ii => Array.tabulate(KERNEL_ROWS){jj => 
          val idx0 = i*stride - 1 + ii
          val idx1 = j*stride - 1 + jj

          val pxl = if (idx0 >= 0 && idx0 < IMAX_0 && idx1 >= 0 && idx1 < IMAX_1) input(idx0,idx1, page) else 0.to[T]
          val f = kernel(k, ii, jj, page)
          // println(r"for $idx0 $idx1: $pxl * $f")
          pxl.to[T2] * f.to[T2]
        }}.flatten.reduce{_+_}
      }.reduce{_+_} //>> 12
      if (el.to[T] + bias(k) < 0.to[T]) 0.to[T] else {el.to[T] + bias(k)}
    }
  
    if ((print_data == 1)) {
      printTensor3(input.reorder(Seq(2,1,0)), "Input")
      printTensor4(kernel.reorder(Seq(0,3,2,1)), "Kernel")
      printTensor3(gold.reorder(Seq(2,1,0)), "Gold")
      printTensor3(results.reorder(Seq(2,1,0)), "Extracted")  

      val bitmask = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) =>
        if (results(i,j,k) == gold(i,j,k)) 1.to[Int] else 0.to[Int]
      }
      val num_wrong = bitmask.length - bitmask.reduce{_+_}
      printTensor3(bitmask.reorder(Seq(2,1,0)), "Matchup")
      println("Error rate: " + num_wrong + " / " + bitmask.length + " incorrect")

    }
    val cksum = gold.zip(results){_==_}.reduce{_&&_}    

    println("PASS: " + cksum + " (SingleLayerConv_RCIO)")
    assert(cksum)

  }
}

@spatial class SingleLayerConv_RCIO_NonBuf extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "16 32 32 32 2 0"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]
  type REALT = FixPt[TRUE,_4,_12]
  type REALT2 = FixPt[TRUE,_8,_24]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 //2 // Unsafe parallelization if OC < 16 (1 burst) because multiple writers may perform unaligned store to same burst simultaneously
    val P3 = 4 //2
    val P4 = 1 //2
    val P5 = 1 //4
    val P6 = 1 //16
    val loadPar = 16 (1 -> 16)
    val storePar = 16 (1 -> 16)
    // Scalar params
    val INPUT_ROWS = ArgIn[Int]
    val INPUT_COLS = ArgIn[Int]
    val INPUT_CHANS = ArgIn[Int]
    val OUTPUT_CHANS = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val input_rows = args(0).to[Int]
    val input_cols = args(1).to[Int]
    val input_chans = args(2).to[Int]
    val output_chans = args(3).to[Int]
    val stride = args(4).to[Int]
    val print_data = args(5).to[Int]

    if (P1 > 1) assert(output_chans > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(INPUT_ROWS, input_rows)
    setArg(INPUT_COLS, input_cols)
    setArg(INPUT_CHANS, input_chans)
    setArg(OUTPUT_CHANS, output_chans)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INPUT_CHANS_MAX = 96
    val OUTPUT_CHANS_MAX = 96

    // Memories
    val INPUT_DATA = DRAM[T](INPUT_COLS, INPUT_ROWS, INPUT_CHANS)
    val OUTPUT_DATA = DRAM[T]((INPUT_COLS-(KERNEL_COLS-STRIDE))/STRIDE, (INPUT_ROWS-(KERNEL_ROWS-STRIDE))/STRIDE, OUTPUT_CHANS)
    val KERNEL_DATA = DRAM[T](OUTPUT_CHANS, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS)
    val BIAS_DATA =   DRAM[T](OUTPUT_CHANS)

    // Load data (placeholder)
    val input = (0::INPUT_COLS, 0::INPUT_ROWS, 0::INPUT_CHANS) {(j,i,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::(INPUT_COLS-(KERNEL_COLS-STRIDE))/STRIDE, 0::(INPUT_ROWS-(KERNEL_ROWS-STRIDE))/STRIDE, 0::OUTPUT_CHANS){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTPUT_CHANS, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INPUT_CHANS) {(i,j,k,l) => if (random[Int](10) > 7) 64.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTPUT_CHANS){i => 1.to[T]}

    printTensor3(input, "Input")
    printTensor4(kernel, "Kernel")
    printArray(bias, "Bias")

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    /*
      bufdim is number of cols in local_data. 
      HEAVYLOOP is the one that eats most of the cycles in this app.  If we make it sequential,
        bufdim needs to be at least 4 for the correct answer for strides 1 or 2.
      We can see by compiling the app and looking at the IR that local_data wants to be triple buffered,
        and we need 2 extra rows per buffer, so that we can collect as many lines as stride for each iter.
        This gives us the size for a proper "line buffer" or "non buffer" (meaning we don't want
        the compiler to make a truly n-buffered mem)
    */
    val bufdim = 8

    Accel{
      val kernel_sram = SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX)
      val bias_sram = SRAM[T](OUTPUT_CHANS_MAX)
      Parallel{
        kernel_sram load KERNEL_DATA(0::OUTPUT_CHANS, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INPUT_CHANS)
        bias_sram load BIAS_DATA(0::OUTPUT_CHANS)        
      }
      val rowspan = if (STRIDE.value == 1) INPUT_ROWS-(KERNEL_ROWS-STRIDE) else (INPUT_ROWS-(KERNEL_ROWS-STRIDE) >> 1)
      val colspan = if (STRIDE.value == 1) INPUT_COLS-(KERNEL_COLS-STRIDE) else (INPUT_COLS-(KERNEL_COLS-STRIDE) >> 1)
      val colstart = mux(STRIDE.value == 1, -2, -1)
      Foreach(rowspan par P1){ R =>
        val row = R*STRIDE.value
        'HEAVYLOOP.Foreach(colstart until colspan par PX){ C =>
          val col = C*STRIDE.value
          val local_data = SRAM[T](bufdim,3,INPUT_CHANS_MAX).nonbuffer
          val accum_line_upcast = SRAM[T2](OUTPUT_CHANS_MAX)
          val accum_line = SRAM[T](OUTPUT_CHANS_MAX)
          Foreach(STRIDE by 1){s => 
            val remotecol = (C - colstart)*STRIDE.value + s
            val localcol = remotecol % bufdim
            local_data(localcol::localcol+1, 0::3, 0::INPUT_CHANS) load INPUT_DATA(remotecol::remotecol+1, row::row+3, 0::INPUT_CHANS par loadPar)
          }
          /* Each stage of HEAVYLOOP should have its own condition wrapping it so the pipeline stays wide */
          if (C >= 0) {
            MemReduce(accum_line_upcast(0::OUTPUT_CHANS par P5))(INPUT_CHANS by 1 par P3){ ic =>
              val local_accum_line = SRAM[T2](OUTPUT_CHANS_MAX)
              Foreach(OUTPUT_CHANS by 1 par P4){ oc =>
                val filter_elements = List.tabulate(3){j => List.tabulate(3){i => 
                  (kernel_sram(oc,j,i,ic).to[T2], j, i)
                }}.flatten
                /* We want to access local_data in a simple way to make banking happy and easy */
                val data_elements = List.tabulate(bufdim){j => List.tabulate(3){i => 
                  local_data(j,i,ic).to[T2]
                }}
                val accum = filter_elements.map{case (f, j, i) => 
                  /* We use muxes to reorder the elements from local_data here */
                  val local_col = (col + j) % bufdim
                  val sel = Seq.tabulate(bufdim){k => local_col == k}
                  val data = oneHotMux(sel, data_elements.map(_(i)))
                      data * f
                    }.reduce{_+_}
                local_accum_line(oc) = accum
              }
              local_accum_line
            }{_+_}
            () // Return Void here explicitly because MemReduce has a real return type
          }

          // RELU
          if (C >= 0) {
            Foreach(OUTPUT_CHANS by 1 par P6){i => 
              val bitshifted = if (accum_line_upcast(i) < (-134217728).to[T2]) -32768.to[T]
                               else if (accum_line_upcast(i) > (134217727).to[T2]) 32767.to[T]
                               else accum_line_upcast(i).bits(27::12).as[T]
              accum_line(i) = max(0.to[T], bitshifted +! bias_sram(i))
            }
          }
          if (C >= 0) {
            OUTPUT_DATA(col/STRIDE,row/STRIDE,0::OUTPUT_CHANS par storePar) store accum_line
          }
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

    // Compute Checks
    val gold = (0::(INPUT_COLS-(KERNEL_COLS-STRIDE)) / STRIDE, 0::(INPUT_ROWS-(KERNEL_ROWS-STRIDE)) / STRIDE, 0::OUTPUT_CHANS){(j,i,k) => 
      val el = Array.tabulate(INPUT_CHANS){page => 
        if (debug && (print_data == 1)) {
            println("Result working on " + k + "," + page + "," + i + "," + j)
            println(" Window: ")
            for (ii <- 0 until KERNEL_ROWS) { 
              for (jj <- 0 until KERNEL_COLS){
                print(" " + input(i*STRIDE+ii,j*STRIDE+jj, page))
              } 
              println(" ")
            }
        }


        Array.tabulate(KERNEL_COLS){jj => Array.tabulate(KERNEL_ROWS){ii => 
          val pxl = input(j*STRIDE+jj,i*STRIDE+ii, page)
          val f = kernel(k, ii, jj, page)
          if (debug && (print_data == 1) && f != 0.to[T]) println(" Partial is " + pxl + " * " + f + " @ " + ii + "," + jj)
          pxl.to[T2] * f.to[T2]
        }}.flatten.reduce{_+_}
      }.reduce{_+_} >> 12
      if (el.to[T] < 0.to[T]) 0.to[T] else {el.to[T] + bias(k)}
    }
  
    if ((print_data == 1)) {
      printTensor3(input, "Input")
      printTensor4(kernel, "Kernel")
      printTensor3(gold, "Gold")
      printTensor3(results, "Extracted")  

      val bitmask = (0::(INPUT_COLS-(KERNEL_COLS-STRIDE)) / STRIDE, 0::(INPUT_ROWS-(KERNEL_ROWS-STRIDE)) / STRIDE, 0::OUTPUT_CHANS){(j,i,k) =>
        if (results(j,i, k) == gold(j,i,k)) 1.to[Int] else 0.to[Int]
      }
      val num_wrong = bitmask.length - bitmask.reduce{_+_}
      printTensor3(bitmask, "Matchup")
      println("Error rate: " + num_wrong + " / " + bitmask.length + " incorrect")

    }
    val cksum = gold.zip(results){_==_}.reduce{_&&_}    

    println("PASS: " + cksum + " (SingleLayerConv_RCIO)")
    assert(cksum)

  }
}


@spatial class SingleLayerConv_IRCO extends SpatialTest {
  type T = FixPt[TRUE,_16,_0]

  override def runtimeArgs: Args = "16 32 32 32 2 0"

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1
    val P1 = 1
    val P2 = 1
    val P3 = 1 // Not working
    val loadPar = 32 (1 -> 16)
    val storePar = 32 (1 -> 16)
    // Scalar params
    val INPUT_ROWS = ArgIn[Int]
    val INPUT_COLS = ArgIn[Int]
    val INPUT_CHANS = ArgIn[Int]
    val OUTPUT_CHANS = ArgIn[Int]
    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val input_rows = args(0).to[Int]
    val input_cols = args(1).to[Int]
    val input_chans = args(2).to[Int]
    val output_chans = args(3).to[Int]
    val stride = args(4).to[Int]
    val print_data = args(5).to[Bit]

    // Set args
    setArg(INPUT_ROWS, input_rows)
    setArg(INPUT_COLS, input_cols)
    setArg(INPUT_CHANS, input_chans)
    setArg(OUTPUT_CHANS, output_chans)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INPUT_CHANS_MAX = 64
    val OUTPUT_CHANS_MAX = 64

    // Memories
    val INPUT_DATA = DRAM[T](INPUT_CHANS, INPUT_ROWS, INPUT_COLS)
    val OUTPUT_DATA = DRAM[T](OUTPUT_CHANS, INPUT_ROWS/STRIDE, INPUT_COLS/STRIDE)
    val KERNEL_DATA = DRAM[T](OUTPUT_CHANS, INPUT_CHANS, KERNEL_ROWS, KERNEL_COLS)
    val BIAS_DATA = DRAM[T](OUTPUT_CHANS, INPUT_ROWS/STRIDE, INPUT_COLS/STRIDE)

    // Load data (placeholder)
    val input = (0::INPUT_CHANS, 0::INPUT_ROWS, 0::INPUT_COLS) {(i,j,k) => ((i + j + k) % 8).to[T]}
    val output = (0::OUTPUT_CHANS, 0::INPUT_ROWS/STRIDE, 0::INPUT_COLS/STRIDE){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTPUT_CHANS, 0::INPUT_CHANS, 0::KERNEL_ROWS, 0::KERNEL_COLS) {(i,j,k,l) => if (random[Int](10) > 8) 1.to[T] else 0.to[T]}
    val bias = (0::OUTPUT_CHANS, 0::INPUT_ROWS/STRIDE, 0::INPUT_COLS/STRIDE){(i,j,k) => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    // setMem(KERNEL_COPY_CPU, kernel)
    // println("MANUALLY COPY KERNEL_DATA PTR TO ME!")

    Accel{
      val kernel_sram = SRAM[T](OUTPUT_CHANS_MAX, INPUT_CHANS_MAX, KERNEL_ROWS, KERNEL_COLS)
      kernel_sram load KERNEL_DATA(0::OUTPUT_CHANS, 0::INPUT_CHANS, 0::KERNEL_ROWS, 0::KERNEL_COLS)
      Foreach(INPUT_CHANS by 1){ ic => 
        val lb1 = LineBuffer.strided[T](KERNEL_ROWS, INPUT_COLS_MAX, 1)
        val lb2 = LineBuffer.strided[T](KERNEL_ROWS, INPUT_COLS_MAX, 2)
        Foreach(INPUT_ROWS by STRIDE){ row => 
          // val bias_srams = SRAM[T](OUTPUT_CHANS_MAX, INPUT_COLS_MAX) // / STRIDE?
          val accum_lines = SRAM[T](OUTPUT_CHANS_MAX, INPUT_COLS_MAX).buffer.flat
          Parallel {
            Foreach(OUTPUT_CHANS by 1 par P3) {oc => 
              if (ic == 0) accum_lines(oc::oc+1, 0::INPUT_COLS/STRIDE) load BIAS_DATA(oc, row/STRIDE::(row/STRIDE)+1, 0::INPUT_COLS/STRIDE par loadPar)
              else accum_lines(oc::oc+1,0::INPUT_COLS/STRIDE) load OUTPUT_DATA(oc, row/STRIDE::(row/STRIDE)+1, 0::INPUT_COLS/STRIDE par loadPar)
            }
            Pipe{
              if (STRIDE.value == 1) lb1 load INPUT_DATA(ic, row,0::INPUT_COLS par loadPar)
              else lb2 load INPUT_DATA(ic, row::row+2,0::INPUT_COLS par loadPar)
            }
          }
          Foreach(INPUT_COLS by STRIDE par PX){ col =>
            val sr1 = RegFile[T](KERNEL_ROWS,KERNEL_COLS)
            val sr2 = RegFile[T](KERNEL_ROWS,KERNEL_COLS)
            if (STRIDE.value == 1) Foreach(3 by 1 par 3){i => sr1(i,*) <<= lb1(i, col)}
            else Foreach(2 by 1 /* par 2*/,3 by 1 par 3){(e,i) => sr2(i,*) <<= lb2(KERNEL_ROWS-1-i, col+e)}
            val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
              if (row.to[Int] + STRIDE.value - KERNEL_ROWS + i.to[Int] < 0 
                  || col.to[Int] + STRIDE.value - KERNEL_COLS + j.to[Int] < 0 ) 
                       {0.to[T]}
              else 
                if (STRIDE.value == 1) {sr1(i,KERNEL_COLS - 1 - j)} 
                else {sr2(i,KERNEL_COLS - 1 - j)}
            }}.flatten

            Foreach(OUTPUT_CHANS by 1 par P1) { oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,ic,i,j)
              }}.flatten

              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduce{_+_}

              accum_lines(oc, col/STRIDE) = accum + accum_lines(oc, col/STRIDE)
              if (debug) println(" = " + accum_lines(oc, col/STRIDE))

            }
            // val accum = Reduce(Reg[T](0))(3 by 1, 3 by 1 par P2){(i,j) => 
            //   val data = if (    row.to[Int] + STRIDE.value - KERNEL_ROWS + i.to[Int] < 0 
            //                   || col.to[Int] + STRIDE.value - KERNEL_COLS + j.to[Int] < 0 ) 
            //                {0.to[T]}
            //              else 
            //                 if (STRIDE.value == 1) {sr1(i,KERNEL_COLS - 1 - j)} 
            //                 else {sr2(i,KERNEL_COLS - 1 - j)}
            //     if (debug) println(" Partial is " + data + " * " + kernel_sram(oc, ic, i, j) + " @ " + i + "," + j)
            //     val k = kernel_sram(oc, ic, i, j)
            //     data * k
            //   }{_+_}
          }

          // Store line back
          Foreach(OUTPUT_CHANS by 1 par P2) { oc =>
            OUTPUT_DATA(oc, row/STRIDE::(row/STRIDE)+1, 0::INPUT_COLS/STRIDE par storePar) store accum_lines(oc::oc+1, 0::INPUT_COLS/STRIDE)
          }
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

    // Compute Checks
    val gold = (0::OUTPUT_CHANS, 0::INPUT_ROWS by STRIDE, 0::INPUT_COLS by STRIDE){(k,i,j) => 
      Array.tabulate(INPUT_CHANS){page => 
        if (debug && print_data) {
            println("Result working on " + k + "," + page + "," + i/STRIDE + "," + j/STRIDE)
            println(" Window: ")
            for (ii <- 0 until KERNEL_ROWS) { 
              for (jj <- 0 until KERNEL_COLS){
                if (i-KERNEL_ROWS+STRIDE+ii < 0 || j-KERNEL_COLS+STRIDE+jj < 0) print(" X") 
                else print(" " + input(page,i-(KERNEL_ROWS-STRIDE-ii),j-(KERNEL_COLS-STRIDE-jj)))
              } 
              println(" ")
            }
        }


        Array.tabulate(KERNEL_ROWS){ii => Array.tabulate(KERNEL_COLS){jj => 
          val pxl = if (i-KERNEL_ROWS+STRIDE+ii < 0 || j-KERNEL_COLS+STRIDE+jj < 0) 0.to[T] 
                    else input(page,i-(KERNEL_ROWS-STRIDE-ii),j-(KERNEL_COLS-STRIDE-jj))
          val f = kernel(k,page, ii, jj)
          if (debug) println(" Partial is " + pxl + " * " + f + " @ " + ii + "," + jj)
          pxl * f
        }}.flatten.reduce{_+_}
      }.reduce{_+_} + bias(k, i/STRIDE, j/STRIDE)
    }
  
    if (print_data) {
      printTensor3(input, "Input")
      printTensor4(kernel, "Kernel")
      printTensor3(gold, "Gold")
      printTensor3(results, "Extracted")  

      val bitmask = (0::OUTPUT_CHANS, 0::INPUT_ROWS by STRIDE, 0::INPUT_COLS by STRIDE){(k,i,j) =>
        if (results(k,i/STRIDE,j/STRIDE) == gold(k,i/STRIDE,j/STRIDE)) 1.to[Int] else 0.to[Int]
      }
      val num_wrong = bitmask.length - bitmask.reduce{_+_}
      printTensor3(bitmask, "Matchup")
      println("Error rate: " + num_wrong + " / " + bitmask.length + " incorrect")

    }
    val cksum = gold.zip(results){_==_}.reduce{_&&_}    

    println("PASS: " + cksum + " (SingleLayerConv_design2)")
    assert(cksum)

  }
}


@spatial class SingleLayerConv_OIRC extends SpatialTest {
  type T = FixPt[TRUE,_16,_0]

  override def runtimeArgs: Args = "16 32 32 32 2 0"

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1
    val loadPar = 16 (1 -> 16)
    val storePar = 16 (1 -> 16)
    // Scalar params
    val INPUT_ROWS = ArgIn[Int]
    val INPUT_COLS = ArgIn[Int]
    val INPUT_CHANS = ArgIn[Int]
    val OUTPUT_CHANS = ArgIn[Int]
    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match
    val KERNEL_ROWS = 3
    val KERNEL_COLS = 3

    // Shadow params (input args)
    val input_rows = args(0).to[Int]
    val input_cols = args(1).to[Int]
    val input_chans = args(2).to[Int]
    val output_chans = args(3).to[Int]
    val stride = args(4).to[Int]
    val print_data = args(5).to[Bit]

    // Set args
    setArg(INPUT_ROWS, input_rows)
    setArg(INPUT_COLS, input_cols)
    setArg(INPUT_CHANS, input_chans)
    setArg(OUTPUT_CHANS, output_chans)
    setArg(STRIDE, stride)

    // HW Design properties
    val INPUT_COLS_MAX = 640
    val INPUT_CHANS_MAX = 64
    val OUTPUT_CHANS_MAX = 64

    // Memories
    val INPUT_DATA = DRAM[T](INPUT_CHANS, INPUT_ROWS, INPUT_COLS)
    val OUTPUT_DATA = DRAM[T](OUTPUT_CHANS, INPUT_ROWS/STRIDE, INPUT_COLS/STRIDE)
    val KERNEL_DATA = DRAM[T](OUTPUT_CHANS, INPUT_CHANS, KERNEL_ROWS, KERNEL_COLS)
    val BIAS_DATA = DRAM[T](OUTPUT_CHANS, INPUT_ROWS/STRIDE, INPUT_COLS/STRIDE)

    // Load data (placeholder)
    val input = (0::INPUT_CHANS, 0::INPUT_ROWS, 0::INPUT_COLS) {(i,j,k) => ((i + j + k) % 8).to[T]}
    val output = (0::OUTPUT_CHANS, 0::INPUT_ROWS/STRIDE, 0::INPUT_COLS/STRIDE){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTPUT_CHANS, 0::INPUT_CHANS, 0::KERNEL_ROWS, 0::KERNEL_COLS) {(i,j,k,l) => if (random[Int](10) > 8) 1.to[T] else 0.to[T]}
    val bias = (0::OUTPUT_CHANS, 0::INPUT_ROWS/STRIDE, 0::INPUT_COLS/STRIDE){(i,j,k) => 1.to[T]}

    // Set data
    setMem(INPUT_DATA, input)
    setMem(OUTPUT_DATA, output)
    setMem(KERNEL_DATA, kernel)
    setMem(BIAS_DATA, bias)

    // setMem(KERNEL_COPY_CPU, kernel)
    // println("MANUALLY COPY KERNEL_DATA PTR TO ME!")

    Accel{
      val kernel_sram = SRAM[T](OUTPUT_CHANS_MAX, INPUT_CHANS_MAX, KERNEL_ROWS, KERNEL_COLS)
      kernel_sram load KERNEL_DATA(0::OUTPUT_CHANS, 0::INPUT_CHANS, 0::KERNEL_ROWS, 0::KERNEL_COLS)

      Foreach(OUTPUT_CHANS by 1){ oc => 
        Foreach(INPUT_CHANS by 1){ ic => 
          val lb1 = LineBuffer.strided[T](KERNEL_ROWS, INPUT_COLS_MAX, 1)
          val lb2 = LineBuffer.strided[T](KERNEL_ROWS, INPUT_COLS_MAX, 2)
          Foreach(INPUT_ROWS by STRIDE){ row => 
            val bias_sram = SRAM[T](INPUT_COLS_MAX) // / STRIDE?
            val accum_line = SRAM[T](INPUT_COLS_MAX).buffer
            if (ic == 0) bias_sram load BIAS_DATA(oc, row/STRIDE, 0::INPUT_COLS/STRIDE par loadPar)
            // Parallel{
              Pipe{accum_line load OUTPUT_DATA(oc, row/STRIDE, 0::INPUT_COLS/STRIDE par loadPar)}
              Pipe{
                if (STRIDE.value == 1) lb1 load INPUT_DATA(ic, row,0::INPUT_COLS par loadPar)
                else lb2 load INPUT_DATA(ic, row::row+2,0::INPUT_COLS par loadPar)
              }
            // }
            Foreach(INPUT_COLS by STRIDE par PX){ col =>
              val sr1 = RegFile[T](KERNEL_ROWS,KERNEL_COLS)
              val sr2 = RegFile[T](KERNEL_ROWS,KERNEL_COLS)
              if (STRIDE.value == 1) Foreach(3 by 1 par 3){i => sr1(i,*) <<= lb1(i, col)}
              else Foreach(2 by 1 /*par 2*/, 3 by 1 par 3){(e,i) => sr2(i,*) <<= lb2(KERNEL_ROWS-1-i, col+e)}
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j => 
                kernel_sram(oc,ic,i,j)
              }}.flatten
              val data_elements = List.tabulate(3){i => List.tabulate(3){j => 
                if (row.to[Int] + STRIDE.value - KERNEL_ROWS + i.to[Int] < 0 
                    || col.to[Int] + STRIDE.value - KERNEL_COLS + j.to[Int] < 0 ) 
                         {0.to[T]}
                else 
                  if (STRIDE.value == 1) {sr1(i,KERNEL_COLS - 1 - j)} 
                  else {sr2(i,KERNEL_COLS - 1 - j)}
              }}.flatten

              val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduce{_+_}
              accum_line(col/STRIDE) = accum + accum_line(col/STRIDE) + mux(ic == 0, bias_sram(col/STRIDE), 0.to[T])
              if (debug) println(" = " + accum_line(col/STRIDE) + ", bias = " + mux(ic == 0, bias_sram(col/STRIDE), 0.to[T]))
            }

            // Store line back
            OUTPUT_DATA(oc, row/STRIDE, 0::INPUT_COLS/STRIDE par storePar) store accum_line
          }
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

    // Compute Checks
    val gold = (0::OUTPUT_CHANS, 0::INPUT_ROWS by STRIDE, 0::INPUT_COLS by STRIDE){(k,i,j) => 
      Array.tabulate(INPUT_CHANS){page => 
        if (debug && print_data) {
            println("Result working on " + k + "," + page + "," + i/STRIDE + "," + j/STRIDE)
            println(" Window: ")
            for (ii <- 0 until KERNEL_ROWS) { 
              for (jj <- 0 until KERNEL_COLS){
                if (i-KERNEL_ROWS+STRIDE+ii < 0 || j-KERNEL_COLS+STRIDE+jj < 0) print(" X") 
                else print(" " + input(page,i-(KERNEL_ROWS-STRIDE-ii),j-(KERNEL_COLS-STRIDE-jj)))
              } 
              println(" ")
            }
        }


        Array.tabulate(KERNEL_ROWS){ii => Array.tabulate(KERNEL_COLS){jj => 
          val pxl = if (i-KERNEL_ROWS+STRIDE+ii < 0 || j-KERNEL_COLS+STRIDE+jj < 0) 0.to[T] 
                    else input(page,i-(KERNEL_ROWS-STRIDE-ii),j-(KERNEL_COLS-STRIDE-jj))
          val f = kernel(k,page, ii, jj)
          if (debug) println(" Partial is " + pxl + " * " + f + " @ " + ii + "," + jj)
          pxl * f
        }}.flatten.reduce{_+_}
      }.reduce{_+_} + bias(k, i/STRIDE, j/STRIDE)
    }
  
    if (print_data) {
      printTensor3(input, "Input")
      printTensor4(kernel, "Kernel")
      printTensor3(gold, "Gold")
      printTensor3(results, "Extracted")  

      val bitmask = (0::OUTPUT_CHANS, 0::INPUT_ROWS by STRIDE, 0::INPUT_COLS by STRIDE){(k,i,j) =>
        if (results(k,i/STRIDE,j/STRIDE) == gold(k,i/STRIDE,j/STRIDE)) 1.to[Int] else 0.to[Int]
      }
      val num_wrong = bitmask.length - bitmask.reduce{_+_}
      printTensor3(bitmask, "Matchup")
      println("Error rate: " + num_wrong + " / " + bitmask.length + " incorrect")

    }
    val cksum = gold.zip(results){_==_}.reduce{_&&_}    

    println("PASS: " + cksum + " (SingleLayerConv_design1)")
    assert(cksum)

  }
}

