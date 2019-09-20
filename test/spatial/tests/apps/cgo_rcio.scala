import spatial.dsl._

@spatial class SingleLayerConv_RCIO_1 extends SpatialTest {
  override def dseModelArgs: Args = "16 16 16 0 0 8 16 16 16 16 16 16 0 0 16"
  override def finalModelArgs: Args = "16 16 16 0 0 8 16 16 16 16 16 16 16 16"
  override def runtimeArgs: Args = "16 32 16 16 2 0" and "16 32 16 16 1 0"
  
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
      val kernel_sram = SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX).flat.effort(0)
      val bias_sram = SRAM[T](OUTPUT_CHANS_MAX)
      // Parallel{
      kernel_sram load KERNEL_DATA(0::OUTPUT_CHANS, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INPUT_CHANS)
      bias_sram load BIAS_DATA(0::OUTPUT_CHANS)        
      // }
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

@spatial class SingleLayerConv_RCIO_2 extends SpatialTest {
  override def dseModelArgs: Args = "16 16 16 0 0 8 16 16 16 16 16 16 0 0 16"
  override def finalModelArgs: Args = "16 16 16 0 0 8 16 16 16 16 16 16 16 16"
  override def runtimeArgs: Args = "16 32 16 16 2 0" and "16 32 16 16 1 0"
  
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
      val kernel_sram = SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX).flat.axesfission(List(List(0,1,2,3))).effort(0)
      val bias_sram = SRAM[T](OUTPUT_CHANS_MAX)
      // Parallel{
      kernel_sram load KERNEL_DATA(0::OUTPUT_CHANS, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INPUT_CHANS)
      bias_sram load BIAS_DATA(0::OUTPUT_CHANS)        
      // }
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


@spatial class SingleLayerConv_RCIO_3 extends SpatialTest {
  override def dseModelArgs: Args = "16 16 16 0 0 8 16 16 16 16 16 16 0 0 16"
  override def finalModelArgs: Args = "16 16 16 0 0 8 16 16 16 16 16 16 16 16"
  override def runtimeArgs: Args = "16 32 16 16 2 0" and "16 32 16 16 1 0"
  
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
      val kernel_sram = SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX).hierarchical.effort(0)
      val bias_sram = SRAM[T](OUTPUT_CHANS_MAX)
      // Parallel{
      kernel_sram load KERNEL_DATA(0::OUTPUT_CHANS, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INPUT_CHANS)
      bias_sram load BIAS_DATA(0::OUTPUT_CHANS)        
      // }
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

@spatial class SingleLayerConv_RCIO_4 extends SpatialTest {
  override def dseModelArgs: Args = "16 16 16 0 0 8 16 16 16 16 16 16 0 0 16"
  override def finalModelArgs: Args = "16 16 16 0 0 8 16 16 16 16 16 16 16 16"
  override def runtimeArgs: Args = "16 32 16 16 2 0" and "16 32 16 16 1 0"
  
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
      val kernel_sram = SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX).hierarchical.axesfission(List(List(0,1,2,3))).effort(0)
      val bias_sram = SRAM[T](OUTPUT_CHANS_MAX)
      // Parallel{
      kernel_sram load KERNEL_DATA(0::OUTPUT_CHANS, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INPUT_CHANS)
      bias_sram load BIAS_DATA(0::OUTPUT_CHANS)        
      // }
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
