package spatial.tests.feature.dense

import spatial.dsl._
// import spatial.stdlib.Convolution

@spatial class Convolutions extends SpatialTest {
  override def runtimeArgs: Args = "16"
  // DSE Parameters
  val coltile = 32 // (16 -> 16 -> 1280)


  // def ConvolutionSlide[T:Num](output: DRAM2[T], // ReviveMe (LineBuffer)
  //   input: DRAM2[T],
  //   filter: LUT2[T],
  //   colstride: scala.Int, rowstride: scala.Int): Unit = {

  //   val lb = LineBuffer.strided[T](filter.rows, coltile, rowstride)
  //   val sr = RegFile[T](filter.rows, filter.cols)
  //   val lineout = SRAM[T](coltile/colstride)
  //   Foreach(input.rows by rowstride){row =>
  //     lb load input(row, 0::input.cols) // TODO: load with correct rowstride
  //     Foreach(input.cols by colstride){j =>
  //       Foreach(filter.rows by 1 par filter.rows){i => sr(i,*) <<= lb(i,j::j+colstride)}
  //       lineout(j/colstride) = Reduce(Reg[T](0.to[T]))(filter.rows by 1, filter.cols by 1){(ii,jj) =>
  //         val img = if ((row.to[Int]+rowstride-1) - (filter.rows - 1 - ii.to[Int]) < 0 || (j.to[Int]+colstride-1) - (filter.cols - 1 - jj.to[Int]) < 0) 0.to[T] else sr(ii,filter.cols - 1 - jj)
  //         img * filter(ii,jj)
  //       }{_+_}
  //       // lineout(j/colstride) = mux(row + (rowstride-1) < filter.rows.to[Int]-1 || j + (colstride-1) < filter.cols.to[Int]-1, 0.to[T], Reduce(Reg[T](0.to[T]))(filter.rows by 1, filter.cols by 1){(ii,jj) => sr(ii,jj) * filter(ii,jj)}{_+_}.value)
  //     }
  //     output(row/rowstride, 0::output.cols) store lineout
  //   }
  // }


  // gemm and gemmv specific
  val tileSizeN    = 16 (16 -> 16 -> 1024)
  val tileSizeM    = 16 (16 -> 16 -> 1024)
  val tileSizeK    = 16 (16 -> 16 -> 1024)
  val m_inner_par  = 1 (1 -> 1 -> 8)
  val n_inner_par  = 1 (1 -> 1 -> 8)
  val k_inner_par  = 1 (1 -> 1 -> 8)
  val m_outer_par  = 1 (1 -> 1 -> 8)
  val n_outer_par  = 1 (1 -> 1 -> 8)
  val k_outer_par  = 1 (1 -> 1 -> 8)
  val c_reduce_par = 1 (1 -> 1 -> 8)
  val y_reduce_par = 1 (1 -> 1 -> 8)
  val store_par = 1 (1 -> 1 -> 16)
  val load_par = 1 (1 -> 1 -> 16)


  def ConvolutionGEMM[T:Num](output: DRAM1[T],
    input: DRAM1[T],
    filter: DRAM2[T]): Unit = {
    Foreach(filter.rows by tileSizeM par m_outer_par){i =>
      // Compute leftover dim
      val elements_m = min(tileSizeM, filter.rows - i)
      // Create Y tile
      val y_tile = SRAM[T](tileSizeM)
      MemReduce(y_tile par y_reduce_par)(filter.cols by tileSizeN par n_outer_par){j =>
        // Compute leftover dim
        val elements_n = min(tileSizeN, filter.cols - j)
        // Create local Y tile for accumulating
        val y_tile_local = SRAM[T](tileSizeM)
        // Create X tile
        val x_tile = SRAM[T](tileSizeN)
        // Load vector tile
        x_tile load input(j::j+elements_n par load_par)
        // Create A tile
        val a_tile = SRAM[T](tileSizeM, tileSizeN)
        // Load matrix tile
        a_tile load filter(i::i+elements_m, j::j+elements_n par load_par)
        Foreach(elements_m by 1 par m_inner_par){ii =>
          y_tile_local(ii) = Reduce(Reg[T])(elements_n by 1 par n_inner_par){jj =>
            a_tile(ii,jj) * x_tile(jj)
          }{_+_}
        }
        y_tile_local
      }{_+_}
      output(i::i+elements_m par store_par) store y_tile
    }
  }

  type T = FixPt[TRUE,_16,_16]


  def main(args: Array[String]): Unit = {

    // Setup strides
    val row_stride1 = 1
    val col_stride1 = 1
    val row_stride2 = 2
    val col_stride2 = 2
    val row_stride3 = 1
    val col_stride3 = 1
    val row_stride4 = 2
    val col_stride4 = 2
    val row_stride5 = 1
    val col_stride5 = 1
    val row_stride6 = 1
    val col_stride6 = 1
    val row_stride7 = 1
    val col_stride7 = 1
    val D = 3

    // cmd-line args (i.e.- "20 0.5 0.5 64 64 64")
    val in_rows = args(0).to[Int]

    // Create random data structures
    val data1 = (0::in_rows,0::coltile){(i,j) => random[T](2)}
    val filter1_data = Array[T](1,2,1,0,0,0,-1,-2,-1)
    val filter1_list = List[T](1,2,1,0,0,0,-1,-2,-1)
    val img3d = (0::D, 0::in_rows, 0::coltile){(i,j,k) => ((i*10 + j + k)%32).to[T]}
    val filter5_data = List[T](1,0,0,
      0,0,1,
      1,0,0,

      0,1,0,
      1,1,1,
      0,1,0,

      0,0,0,
      0,0,0,
      0,1,1
    )


    // Create toeplitz for filter and padded image
    val data3 = (0::in_rows + (3 - row_stride3), 0::coltile + (3 - col_stride3)){(i,j) => if (i < (3 - row_stride3) || j < (3 - col_stride3)) 0 else data1( i-(3 - row_stride3), j-(3 - col_stride3) )}.flatten
    val data4 = (0::in_rows + (3 - row_stride4), 0::coltile + (3 - col_stride4)){(i,j) => if (i < (3 - row_stride4) || j < (3 - col_stride4)) 0 else data1( i-(3 - row_stride4), j-(3 - col_stride4) )}.flatten
    val filter3_tplz = filter1_data.toeplitz(3,3,in_rows,coltile, row_stride3, col_stride3)
    // println("Expanded filter is " + filter3_tplz.rows + " x " + filter3_tplz.cols)
    // println("Padded data is " + data3.length + " elements long")
    val filter4_tplz = filter1_data.toeplitz(3,3,in_rows,coltile, row_stride4, col_stride4)
    // println("Expanded filter is " + filter4_tplz.rows + " x " + filter4_tplz.cols)
    // println("Padded data is " + data4.length + " elements long")

    // Show inputs
    printMatrix(data1, "Img1")
    // printArray(data3, "Flattened padded img")
    // printMatrix(filter3_tplz, "Toeplitz Filter")
    // printMatrix(filter4_tplz, "Toeplitz Filter, colstride=2")

    // ArgIns
    val M = ArgIn[Int]
    val N = ArgIn[Int]
    val Mds1 = ArgIn[Int]
    val Nds1 = ArgIn[Int]
    val Mds2 = ArgIn[Int]
    val Nds2 = ArgIn[Int]
    val Len3 = ArgIn[Int]
    val Len4 = ArgIn[Int]
    val OutLen3 = ArgIn[Int]
    val Mds3 = ArgIn[Int]
    val Nds3 = ArgIn[Int]
    val Mds4 = ArgIn[Int]
    val Nds4 = ArgIn[Int]
    val OutLen4 = ArgIn[Int]
    setArg(M, in_rows)
    setArg(N, coltile)
    setArg(Mds1, in_rows / row_stride1)
    setArg(Nds1, coltile / col_stride1)
    setArg(Mds2, in_rows / row_stride2)
    setArg(Nds2, coltile / col_stride2)
    setArg(Len3, data3.length)
    setArg(Len4, data4.length)
    setArg(OutLen3, filter3_tplz.rows)
    setArg(OutLen4, filter4_tplz.rows)
    setArg(Mds3, filter3_tplz.rows)
    setArg(Nds3, filter3_tplz.cols)
    setArg(Mds4, filter4_tplz.rows)
    setArg(Nds4, filter4_tplz.cols)

    // Offchip structures
    val image = DRAM[T](M, N)
    val flatimg = DRAM[T](Len3)
    val flatimg4 = DRAM[T](Len4)
    val dram1 = DRAM[T](Mds1, Nds1)
    val dram2 = DRAM[T](Mds2, Nds2)
    val dram3 = DRAM[T](OutLen3)
    val dram4 = DRAM[T](OutLen4)
    val dram5 = DRAM[T](M, N)
    val dram6 = DRAM[T](2, Mds1, Nds1)
    val dram7 = DRAM[T](2, M, N)
    val filter3 = DRAM[T](Mds3, Nds3)
    val filter4 = DRAM[T](Mds4, Nds4)
    val image3d = DRAM[T](D,M,N)

    setMem(image, data1)
    setMem(image3d, img3d)
    setMem(flatimg, data3)
    setMem(flatimg4, data4)
    setMem(filter3, filter3_tplz)
    setMem(filter4, filter4_tplz)

    // Run Accel functions
    Accel{
      val filter = LUT[T](3,3)(filter1_list:_*)
      val filter5 = LUT[T](3,3,3)(filter5_data:_*)
      val filter6 = LUT[T](3,3)(filter1_list.map{_+1}:_*)
      val filter7 = LUT[T](3,3,3)(filter5_data.map{_+1}:_*)

      // Use stdlib defs
      // Pipe{ConvolutionSlide[T](dram1, image, filter, col_stride1, row_stride1, 16, 16)}
      // Pipe{ConvolutionSlide[T](dram2, image, filter, col_stride2, row_stride2, 16, 16)}
      Pipe{ConvolutionGEMM[T](dram3, flatimg, filter3)}
      Pipe{ConvolutionGEMM[T](dram4, flatimg4, filter4)}
      // Pipe{MCConvolutionSlide(dram5, image3d, filter5, col_stride5, row_stride5, 16, 16, 3)}
      // Pipe{MFConvolutionSlide[T](dram6, image, List(filter, filter6), col_stride6, row_stride6, 16, 16)}
      // Pipe{MCMFConvolutionSlide[T](dram7, image3d, List(filter5, filter7), col_stride7, row_stride7, 16, 16, 3)}

      // // Use defs in this app
      // ConvolutionSlide[T](dram1, image, filter, col_stride1, row_stride1)
      // ConvolutionSlide[T](dram2, image, filter, col_stride2, row_stride2)
      // ConvolutionGEMM[T](dram3, flatimg, filter3)
      // ConvolutionGEMM[T](dram4, flatimg, filter4)
    }

    // Get results
    val res1 = getMatrix(dram1)
    val res2 = getMatrix(dram2)
    val res3 = getMem(dram3).reshape(in_rows, coltile)
    val res4 = getMem(dram4).reshape(res2.rows, res2.cols)
    val res5 = getMatrix(dram5)
    val res6 = getTensor3(dram6)
    val res7 = getTensor3(dram7)

    // Compute Golds
    val gold1 = (0::in_rows / row_stride1, 0::coltile / col_stride1){(i,j) =>
      Array.tabulate(3){ii => Array.tabulate(3){jj =>
        val img = if (i*row_stride1-ii < 0 || j*col_stride1-jj < 0) 0 else data1(i*row_stride1-ii,j*col_stride1-jj)
        img * filter1_data((2-ii)*3+(2-jj))
      }}.flatten.reduce{_+_}
    }
    val gold2 = (0::in_rows / row_stride2, 0::coltile / col_stride2){(i,j) =>
      Array.tabulate(3){ii => Array.tabulate(3){jj =>
        val real_i = i*row_stride2-ii+(row_stride2-1)
        val real_j = j*col_stride2-jj+(col_stride2-1)
        val img = if (real_i < 0 || real_j < 0) 0 else data1(real_i,real_j)
        img * filter1_data((2-ii)*3+(2-jj))
      }}.flatten.reduce{_+_}
    }
    val gold3 = gold1
    val gold4 = gold2
    val friendly_filter5 = Array[T](filter5_data:_*)
    val gold5 = (0::M, 0::N){(i,j) =>
      Array.tabulate(D){page =>
        Array.tabulate(3){ii => Array.tabulate(3){jj =>
          val pxl = if (i-ii < 0 || j-jj < 0) 0.to[T] else img3d(page,i-ii,j-jj)
          pxl * friendly_filter5(page*9+(2-ii)*3+(2-jj))
        }}.flatten.reduce{_+_}
      }.reduce{_+_}
    }
    val gold6 = (0::2, 0::in_rows / col_stride6, 0::coltile / col_stride6){(k,i,j) =>
      Array.tabulate(3){ii => Array.tabulate(3){jj =>
        val f = if (k == 0) filter1_data((2-ii)*3+(2-jj)) else filter1_data((2-ii)*3+(2-jj)) + 1
        val img = if (i*row_stride1-ii < 0 || j*col_stride1-jj < 0) 0 else data1(i*row_stride1-ii,j*col_stride1-jj)
        // println("for " + k + "," + i + "," + j + " = " + f + " * " + img)
        img * f
      }}.flatten.reduce{_+_}
    }
    val gold7 = (0::2, 0::M, 0::N){(k,i,j) =>
      Array.tabulate(D){page =>
        Array.tabulate(3){ii => Array.tabulate(3){jj =>
          val pxl = if (i-ii < 0 || j-jj < 0) 0.to[T] else img3d(page,i-ii,j-jj)
          val f = if (k == 0) friendly_filter5(page*9+(2-ii)*3+(2-jj)) else friendly_filter5(page*9+(2-ii)*3+(2-jj)) + 1
          pxl * f
        }}.flatten.reduce{_+_}
      }.reduce{_+_}
    }

    // Collect cksums
    val margin = 0.25.to[T]
    val cksum1 = true // res1.zip(gold1){_==_}.reduce{_&&_}
    val cksum2 = true // res2.zip(gold2){_==_}.reduce{_&&_}
    val cksum3 = res3.zip(gold3){_==_}.reduce{_&&_}
    val cksum4 = res4.zip(gold4){_==_}.reduce{_&&_}
    val cksum5 = true // res5.zip(gold5){_==_}.reduce{_&&_}
    val cksum6 = true // res6.zip(gold6){_==_}.reduce{_&&_}
    val cksum7 = true // res7.zip(gold7){_==_}.reduce{_&&_}
    val cksum = cksum1 && cksum2 && cksum3 && cksum4 && cksum5 && cksum6 && cksum7

    // Print results
    println("Conv1 Result: ")
    printMatrix(res1, "  Got")
    printMatrix(gold1, "  Wanted")
    println("Conv2 Result: ")
    printMatrix(res2, "  Got")
    printMatrix(gold2, "  Wanted")
    println("Conv3 Result: ")
    printMatrix(res3, "  Got")
    printMatrix(gold3, "  Wanted")
    println("Conv4 Result: ")
    printMatrix(res4, "  Got")
    printMatrix(gold4, "  Wanted")
    println("Conv5 Result: ")
    printMatrix(res5, "  Got")
    printMatrix(gold5, "  Wanted")
    println("Conv6 Result: ")
    printTensor3(res6, "  Got")
    printTensor3(gold6, "  Wanted")
    println("Conv7 Result: ")
    printTensor3(res7, "  Got")
    printTensor3(gold7, "  Wanted")
    assert(cksum1)
    assert(cksum2)
    assert(cksum3)
    assert(cksum4)
    assert(cksum5)
    assert(cksum6)
    assert(cksum7)
    assert(cksum)
    println("PASS: " + cksum + " (Convolutions)")

  }
}

@spatial class Convolution extends SpatialTest {
  override def dseModelArgs: Args = "32 16 64 128 128 128 128 128 128 128 128 128 32 16 16 16"
  override def finalModelArgs: Args = "32 16 64 128 128 128 128 128 128 128 128 128 32 16 16 16"
  override def runtimeArgs: Args = "128 64 32 16 2"
  type T = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    // Set properties to be known at compile-time
    val window = 9
    val IN_CHANS_MAX = 256
    val OUT_CHANS_MAX = 256

    // Set parallelizations
    val P1 = 1
    val P2 = 1
    val P3 = 1
    val LP = 1
    val SP = 1

    // Create ArgIns/Outs
    val IN_POINTS = ArgIn[Int]
    val OUT_POINTS = ArgIn[Int]
    val IN_CHANS = ArgIn[Int]
    val OUT_CHANS = ArgIn[Int]
    val STRIDE = ArgIn[Int]
    val n_points_in = args(0).to[Int]
    val n_points_out = args(1).to[Int]
    val n_chans_in = args(2).to[Int]
    val n_chans_out = args(3).to[Int]
    val stride = args(4).to[Int]
    setArg(IN_POINTS, n_points_in)
    setArg(OUT_POINTS, n_points_out)
    setArg(IN_CHANS, n_chans_in)
    setArg(OUT_CHANS, n_chans_out)
    setArg(STRIDE, stride)

    // Create data structures
    val in_data = (0::n_points_in,0::n_chans_in){(i,j) => (i+j).to[T]}
    val kernel = (0::window, 0::n_chans_in, 0::n_chans_out){(i,j,k) => ((i+j+k)%3).to[T]}
    val DATA = DRAM[T](IN_POINTS,IN_CHANS)
    val KERNEL = DRAM[T](window, IN_CHANS, OUT_CHANS)
    val RESULT = DRAM[T](OUT_POINTS,OUT_CHANS)
    setMem(DATA, in_data)
    setMem(KERNEL, kernel)
    printMatrix(in_data.t, "Data: (n_points is leading dimension)")
    printTensor3(kernel, "Kernel: (each matrix is in_chans x out_chans)")

    Accel {
      // Create local memory for kernel values
      val kernel_sram = SRAM[T](window, IN_CHANS_MAX, OUT_CHANS_MAX)
      kernel_sram load KERNEL

      // Create stream controller to run once per output point (which includes all output channels per point)
      Foreach(OUT_POINTS by 1){ pt =>

        // Create FIFOs to hold input data (declare here so parallelizing stream results in duplication of these)
        val in_fifos = List.tabulate(window){_ => FIFO[T](IN_CHANS_MAX)}
        // Create FIFO to buffer output data
        val line_out = FIFO[T](OUT_CHANS_MAX)
        // Create signalling FIFO to mediate control
        val store_ready = FIFO[Bit](8)
        
        val C = pt * STRIDE

        // Fetch data
        // Parallel{
        in_fifos.zipWithIndex.map{case (f, i) => 
          Pipe{f load DATA(max(0,min(C - (window/2) + i, IN_POINTS-1)), 0::IN_CHANS par LP)}
        }
        // }

        // Allocate temp accumulator
        val line_out_sram = SRAM[T](OUT_CHANS_MAX)
        // Compute partial result for each IN_CHAN
        MemReduce(line_out_sram(0::OUT_CHANS par P3))(IN_CHANS by 1 par P1){ic => 
          val local_acc = SRAM[T](OUT_CHANS_MAX)
          val data_raw = in_fifos.map{f => f.deq()}
          // While we have input data, run it against each output channel's kernel
          Foreach(OUT_CHANS by 1 par P2){oc => 
            val filter = List.tabulate(window){lane => kernel_sram(lane, ic, oc)}
            val data = List.tabulate(window){lane => mux(C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1, data_raw(lane), 0.to[T])}
            val acc = filter.zip(data).map{case (a,b) => a*b}.reduceTree{_+_}
            local_acc(oc) = acc
          }
          local_acc
        }{_+_}

        // Store data out
        RESULT(pt, 0::OUT_CHANS par SP) store line_out_sram
        
      }

    }

    // Compute gold
    val gold = (0::n_points_out, 0::n_chans_out){(pt, oc) => 
      val lane_prods = List.tabulate(window){lane => 
        val C = pt * stride
        Array.tabulate(n_chans_in){ic => 
          val data = if (C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1) in_data(C + lane - (window/2), ic) else 0.to[T]
          data * kernel(lane, ic, oc)
        }.reduce{_+_}
      }
      lane_prods.reduce{_+_}
    }
    val got = getMatrix(RESULT)
    printMatrix(got.t, "Got")
    printMatrix(gold.t, "Gold")

    val cksum = gold == got
    println("PASS: " + cksum + " (Convolution)")
    assert(cksum)
  }
}

@spatial class ConvolutionFlat extends SpatialTest {
  override def runtimeArgs: Args = "128 64 32 16 2"
  type T = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    // Set properties to be known at compile-time
    val window = 9
    val IN_CHANS_MAX = 256
    val OUT_CHANS_MAX = 256

    // Set parallelizations
    val P1 = 1
    val P2 = 1
    val P3 = 1
    val LP = 1
    val SP = 1

    // Create ArgIns/Outs
    val IN_POINTS = ArgIn[Int]
    val OUT_POINTS = ArgIn[Int]
    val IN_CHANS = ArgIn[Int]
    val OUT_CHANS = ArgIn[Int]
    val STRIDE = ArgIn[Int]
    val n_points_in = args(0).to[Int]
    val n_points_out = args(1).to[Int]
    val n_chans_in = args(2).to[Int]
    val n_chans_out = args(3).to[Int]
    val stride = args(4).to[Int]
    setArg(IN_POINTS, n_points_in)
    setArg(OUT_POINTS, n_points_out)
    setArg(IN_CHANS, n_chans_in)
    setArg(OUT_CHANS, n_chans_out)
    setArg(STRIDE, stride)

    // Create data structures
    val in_data = (0::n_points_in,0::n_chans_in){(i,j) => (i+j).to[T]}
    val kernel = (0::window, 0::n_chans_in, 0::n_chans_out){(i,j,k) => ((i+j+k)%3).to[T]}
    val DATA = DRAM[T](IN_POINTS,IN_CHANS)
    val KERNEL = DRAM[T](window, IN_CHANS, OUT_CHANS)
    val RESULT = DRAM[T](OUT_POINTS,OUT_CHANS)
    setMem(DATA, in_data)
    setMem(KERNEL, kernel)
    printMatrix(in_data.t, "Data: (n_points is leading dimension)")
    printTensor3(kernel, "Kernel: (each matrix is in_chans x out_chans)")

    Accel {
      // Create local memory for kernel values
      val kernel_sram = SRAM[T](window, IN_CHANS_MAX, OUT_CHANS_MAX)
      kernel_sram load KERNEL

      // Create stream controller to run once per output point (which includes all output channels per point)
      Foreach(OUT_POINTS by 1){ pt =>

        // Create FIFOs to hold input data (declare here so parallelizing stream results in duplication of these)
        val in_fifos = List.tabulate(window){_ => FIFO[T](IN_CHANS_MAX)}
        // Create FIFO to buffer output data
        val line_out = FIFO[T](OUT_CHANS_MAX)
        // Create signalling FIFO to mediate control
        val store_ready = FIFO[Bit](8)
        
        val C = pt * STRIDE

        // Fetch data
        // Parallel{
        in_fifos.zipWithIndex.map{case (f, i) => 
          Pipe{f load DATA(max(0,min(C - (window/2) + i, IN_POINTS-1)), 0::IN_CHANS par LP)}
        }
        // }

        // Allocate temp accumulator
        val line_out_sram = SRAM[T](OUT_CHANS_MAX)
        // Compute partial result for each IN_CHAN
        Foreach(IN_CHANS by 1 par P1, OUT_CHANS by 1 par P2){(ic, oc) => 
          val holders = List.tabulate(window){lane => Reg[T]}
          val data_raw = in_fifos.zip(holders).map{case (f,r) => if (oc == 0) {val x = f.deq(oc == 0); r := x; x} else r.value}
          val filter = List.tabulate(window){lane => kernel_sram(lane, ic, oc)}
          val data = List.tabulate(window){lane => mux(C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1, data_raw(lane), 0.to[T])}
          val acc = filter.zip(data).map{case (a,b) => a*b}.reduceTree{_+_}
          line_out_sram(oc) = mux(ic == 0, acc, line_out_sram(oc) + acc)
        }

        // Store data out
        RESULT(pt, 0::OUT_CHANS par SP) store line_out_sram
        
      }

    }

    // Compute gold
    val gold = (0::n_points_out, 0::n_chans_out){(pt, oc) => 
      val lane_prods = List.tabulate(window){lane => 
        val C = pt * stride
        Array.tabulate(n_chans_in){ic => 
          val data = if (C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1) in_data(C + lane - (window/2), ic) else 0.to[T]
          data * kernel(lane, ic, oc)
        }.reduce{_+_}
      }
      lane_prods.reduce{_+_}
    }
    val got = getMatrix(RESULT)
    printMatrix(got.t, "Got")
    printMatrix(gold.t, "Gold")

    val cksum = gold == got
    println("PASS: " + cksum + " (ConvolutionFlat)")
    assert(cksum)
  }
}

@spatial class ConvolutionStream extends SpatialTest {
  override def runtimeArgs: Args = "128 64 32 16 2"
  type T = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    // Set properties to be known at compile-time
    val window = 9
    val IN_CHANS_MAX = 256
    val OUT_CHANS_MAX = 256

    // Set parallelizations
    val P1 = 1
    val P2 = 1
    val P3 = 1
    val LP = 1
    val SP = 1

    // Create ArgIns/Outs
    val IN_POINTS = ArgIn[Int]
    val OUT_POINTS = ArgIn[Int]
    val IN_CHANS = ArgIn[Int]
    val OUT_CHANS = ArgIn[Int]
    val STRIDE = ArgIn[Int]
    val donttouch = ArgOut[Bit]
    val n_points_in = args(0).to[Int]
    val n_points_out = args(1).to[Int]
    val n_chans_in = args(2).to[Int]
    val n_chans_out = args(3).to[Int]
    val stride = args(4).to[Int]
    setArg(IN_POINTS, n_points_in)
    setArg(OUT_POINTS, n_points_out)
    setArg(IN_CHANS, n_chans_in)
    setArg(OUT_CHANS, n_chans_out)
    setArg(STRIDE, stride)

    // Create data structures
    val in_data = (0::n_points_in,0::n_chans_in){(i,j) => (i+j).to[T]}
    val kernel = (0::window, 0::n_chans_in, 0::n_chans_out){(i,j,k) => ((i+j+k)%3).to[T]}
    val DATA = DRAM[T](IN_POINTS,IN_CHANS)
    val KERNEL = DRAM[T](window, IN_CHANS, OUT_CHANS)
    val RESULT = DRAM[T](OUT_POINTS,OUT_CHANS)
    setMem(DATA, in_data)
    setMem(KERNEL, kernel)
    printMatrix(in_data.t, "Data: (n_points is leading dimension)")
    printTensor3(kernel, "Kernel: (each matrix is in_chans x out_chans)")

    Accel {
      // Create local memory for kernel values
      val kernel_sram = SRAM[T](window, IN_CHANS_MAX, OUT_CHANS_MAX)
      kernel_sram load KERNEL

      // Create stream controller to run once per output point (which includes all output channels per point)
      Stream.Foreach(OUT_POINTS by 1){ pt =>

        // Create FIFOs to hold input data (declare here so parallelizing stream results in duplication of these)
        val in_fifos = List.tabulate(window){_ => FIFO[T](IN_CHANS_MAX)}
        // Create FIFO to buffer output data
        val line_out = FIFO[T](OUT_CHANS_MAX)
        // Create signalling FIFO to mediate control
        val store_ready = FIFO[Bit](8)

        // Fetch data
        in_fifos.zipWithIndex.map{case (f, i) => 
          // Quirk of compiler's Pipe insertion, guarantee that pt * STRIDE is computed locally
          'FETCH.Pipe{ 
            val C = pt * STRIDE
            f load DATA(max(0,min(C - (window/2) + i, IN_POINTS-1)), 0::IN_CHANS par LP)
          }
        }

        // Greedily consume data
        // Quirk of compiler's Pipe insertion, guarantee that pt * STRIDE is computed locally
        'COMPUTE.Pipe{ 
          val C = pt * STRIDE
          // Allocate temp accumulator
          val line_out_sram = SRAM[T](OUT_CHANS_MAX)
          // Compute partial result for each IN_CHAN
          MemReduce(line_out_sram(0::OUT_CHANS par P3))(IN_CHANS by 1 par P1){ic => 
            val local_acc = SRAM[T](OUT_CHANS_MAX)
            val data_raw = in_fifos.map{f => f.deq()}
            // While we have input data, run it against each output channel's kernel
            Foreach(OUT_CHANS by 1 par P2){oc => 
              val filter = List.tabulate(window){lane => kernel_sram(lane, ic, oc)}
              val data = List.tabulate(window){lane => mux(C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1, data_raw(lane), 0.to[T])}
              val acc = filter.zip(data).map{case (a,b) => a*b}.reduceTree{_+_}
              local_acc(oc) = acc
            }
            local_acc
          }{_+_}
          // Quickly copy results to output FIFO and indicate data is ready
          Foreach(OUT_CHANS by 1){oc => line_out.enq(line_out_sram(oc)); if (oc == 0) {store_ready.enq(true)}}
        }

        // Store data out
        'STORE.Pipe{
          donttouch := store_ready.deq() // Do not want to begin issuing store commands too soon
          RESULT(pt, 0::OUT_CHANS par SP) store line_out  
        }
        
      }

    }

    println(r"donttouch: $donttouch") // Guarantee compiler will not DCE

    // Compute gold
    val gold = (0::n_points_out, 0::n_chans_out){(pt, oc) => 
      val lane_prods = List.tabulate(window){lane => 
        val C = pt * stride
        Array.tabulate(n_chans_in){ic => 
          val data = if (C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1) in_data(C + lane - (window/2), ic) else 0.to[T]
          data * kernel(lane, ic, oc)
        }.reduce{_+_}
      }
      lane_prods.reduce{_+_}
    }
    val got = getMatrix(RESULT)
    printMatrix(got.t, "Got")
    printMatrix(gold.t, "Gold")

    val cksum = gold == got
    println("PASS: " + cksum + " (ConvolutionStream)")
    assert(cksum)
  }
}

@spatial class ConvolutionStreamFlat extends SpatialTest {
  override def runtimeArgs: Args = "128 64 32 16 2"
  type T = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    // Set properties to be known at compile-time
    val window = 9
    val IN_CHANS_MAX = 256
    val OUT_CHANS_MAX = 256

    // Set parallelizations
    val P1 = 1
    val P2 = 1
    val P3 = 1
    val LP = 1
    val SP = 1

    // Create ArgIns/Outs
    val IN_POINTS = ArgIn[Int]
    val OUT_POINTS = ArgIn[Int]
    val IN_CHANS = ArgIn[Int]
    val OUT_CHANS = ArgIn[Int]
    val STRIDE = ArgIn[Int]
    val donttouch = ArgOut[Bit]
    val n_points_in = args(0).to[Int]
    val n_points_out = args(1).to[Int]
    val n_chans_in = args(2).to[Int]
    val n_chans_out = args(3).to[Int]
    val stride = args(4).to[Int]
    setArg(IN_POINTS, n_points_in)
    setArg(OUT_POINTS, n_points_out)
    setArg(IN_CHANS, n_chans_in)
    setArg(OUT_CHANS, n_chans_out)
    setArg(STRIDE, stride)

    // Create data structures
    val in_data = (0::n_points_in,0::n_chans_in){(i,j) => (i+j).to[T]}
    val kernel = (0::window, 0::n_chans_in, 0::n_chans_out){(i,j,k) => ((i+j+k)%3).to[T]}
    val DATA = DRAM[T](IN_POINTS,IN_CHANS)
    val KERNEL = DRAM[T](window, IN_CHANS, OUT_CHANS)
    val RESULT = DRAM[T](OUT_POINTS,OUT_CHANS)
    setMem(DATA, in_data)
    setMem(KERNEL, kernel)
    printMatrix(in_data.t, "Data: (n_points is leading dimension)")
    printTensor3(kernel, "Kernel: (each matrix is in_chans x out_chans)")

    Accel {
      // Create local memory for kernel values
      val kernel_sram = SRAM[T](window, IN_CHANS_MAX, OUT_CHANS_MAX)
      kernel_sram load KERNEL

      // Create stream controller to run once per output point (which includes all output channels per point)
      Stream.Foreach(OUT_POINTS by 1){ pt =>

        // Create FIFOs to hold input data (declare here so parallelizing stream results in duplication of these)
        val in_fifos = List.tabulate(window){_ => FIFO[T](IN_CHANS_MAX)}
        // Create FIFO to buffer output data
        val line_out = FIFO[T](OUT_CHANS_MAX)
        // Create signalling FIFO to mediate control
        val store_ready = FIFO[Bit](8)

        // Fetch data
        in_fifos.zipWithIndex.map{case (f, i) => 
          // Quirk of compiler's Pipe insertion, guarantee that pt * STRIDE is computed locally
          'FETCH.Pipe{ 
            val C = pt * STRIDE
            f load DATA(max(0,min(C - (window/2) + i, IN_POINTS-1)), 0::IN_CHANS par LP)
          }
        }

        // Greedily consume data
        // Quirk of compiler's Pipe insertion, guarantee that pt * STRIDE is computed locally
        'COMPUTE.Pipe{ 
          val C = pt * STRIDE
          // Allocate temp accumulator
          val line_out_sram = SRAM[T](OUT_CHANS_MAX)
          // Compute partial result for each IN_CHAN
          Foreach(IN_CHANS by 1 par P1, OUT_CHANS by 1 par P2){(ic, oc) => 
            val holders = List.tabulate(window){lane => Reg[T]}
            val data_raw = in_fifos.zip(holders).map{case (f,r) => if (oc == 0) {val x = f.deq(oc == 0); r := x; x} else r.value}
            val filter = List.tabulate(window){lane => kernel_sram(lane, ic, oc)}
            val data = List.tabulate(window){lane => mux(C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1, data_raw(lane), 0.to[T])}
            val acc = filter.zip(data).map{case (a,b) => a*b}.reduceTree{_+_}
            line_out_sram(oc) = mux(ic == 0, acc, line_out_sram(oc) + acc)
          }
          // Quickly copy results to output FIFO and indicate data is ready
          Foreach(OUT_CHANS by 1){oc => line_out.enq(line_out_sram(oc)); if (oc == 0) {store_ready.enq(true)}}
        }

        // Store data out
        'STORE.Pipe{
          donttouch := store_ready.deq() // Do not want to begin issuing store commands too soon
          RESULT(pt, 0::OUT_CHANS par SP) store line_out  
        }
        
      }

    }

    println(r"donttouch: $donttouch") // Guarantee compiler will not DCE

    // Compute gold
    val gold = (0::n_points_out, 0::n_chans_out){(pt, oc) => 
      val lane_prods = List.tabulate(window){lane => 
        val C = pt * stride
        Array.tabulate(n_chans_in){ic => 
          val data = if (C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1) in_data(C + lane - (window/2), ic) else 0.to[T]
          data * kernel(lane, ic, oc)
        }.reduce{_+_}
      }
      lane_prods.reduce{_+_}
    }
    val got = getMatrix(RESULT)
    printMatrix(got.t, "Got")
    printMatrix(gold.t, "Gold")

    val cksum = gold == got
    println("PASS: " + cksum + " (ConvolutionStreamFlat)")
    assert(cksum)
  }
}


@spatial class ConvolutionStreamReclaim extends SpatialTest {
  override def runtimeArgs: Args = "128 64 32 16 2"
  type T = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    // Set properties to be known at compile-time
    val window = 9
    val IN_CHANS_MAX = 256
    val OUT_CHANS_MAX = 256

    // Set parallelizations
    val P1 = 1
    val P2 = 1
    val P3 = 1
    val LP = 1
    val SP = 1

    // Create ArgIns/Outs
    val IN_POINTS = ArgIn[Int]
    val OUT_POINTS = ArgIn[Int]
    val IN_CHANS = ArgIn[Int]
    val OUT_CHANS = ArgIn[Int]
    val STRIDE = ArgIn[Int]
    val donttouch = ArgOut[Bit]
    val n_points_in = args(0).to[Int]
    val n_points_out = args(1).to[Int]
    val n_chans_in = args(2).to[Int]
    val n_chans_out = args(3).to[Int]
    val stride = args(4).to[Int]
    setArg(IN_POINTS, n_points_in)
    setArg(OUT_POINTS, n_points_out)
    setArg(IN_CHANS, n_chans_in)
    setArg(OUT_CHANS, n_chans_out)
    setArg(STRIDE, stride)

    // Create data structures
    val in_data = (0::n_points_in,0::n_chans_in){(i,j) => (i+j).to[T]}
    val kernel = (0::window, 0::n_chans_in, 0::n_chans_out){(i,j,k) => ((i+j+k)%3).to[T]}
    val DATA = DRAM[T](IN_POINTS,IN_CHANS)
    val KERNEL = DRAM[T](window, IN_CHANS, OUT_CHANS)
    val RESULT = DRAM[T](OUT_POINTS,OUT_CHANS)
    setMem(DATA, in_data)
    setMem(KERNEL, kernel)
    printMatrix(in_data.t, "Data: (n_points is leading dimension)")
    printTensor3(kernel, "Kernel: (each matrix is in_chans x out_chans)")

    Accel {
      // Create local memory for kernel values
      val kernel_sram = SRAM[T](window, IN_CHANS_MAX, OUT_CHANS_MAX).hierarchical
      kernel_sram load KERNEL

      // Create stream controller to run once per output point (which includes all output channels per point)
      Stream.Foreach(OUT_POINTS by 1){ pt =>

        // Create FIFOs to hold input data (declare here so parallelizing stream results in duplication of these)
        val in_fifos = List.tabulate(window){_ => FIFO[T](IN_CHANS_MAX).conflictable}
        // Create FIFO to buffer output data
        val line_out = FIFO[T](OUT_CHANS_MAX)
        // Create signalling FIFO to mediate control
        val store_ready = FIFO[Bit](8)

        // Fetch data
        in_fifos.zipWithIndex.map{case (f, i) => 
          // Quirk of compiler's Pipe insertion, guarantee that pt * STRIDE is computed locally
          'FETCH.Pipe{ 
            if (pt == 0 || (i >= (window-STRIDE.value))) {
              val C = pt * STRIDE
              f load DATA(max(0,min(C - (window/2) + i, IN_POINTS-1)), 0::IN_CHANS par LP)
            }
          }
        }

        // Greedily consume data
        // Quirk of compiler's Pipe insertion, guarantee that pt * STRIDE is computed locally
        'COMPUTE.Pipe{ 
          val C = pt * STRIDE
          // Allocate temp accumulator
          val line_out_sram = SRAM[T](OUT_CHANS_MAX)
          // Compute partial result for each IN_CHAN
          MemReduce(line_out_sram(0::OUT_CHANS par P3))(IN_CHANS by 1 par P1){ic => 
            val local_acc = SRAM[T](OUT_CHANS_MAX)
            val data_raw = in_fifos.map{f => f.deq()}
            // While we have input data, run it against each output channel's kernel
            Foreach(OUT_CHANS by 1 par P2){oc => 
              val filter = List.tabulate(window){lane => kernel_sram(lane, ic, oc)}
              val data = List.tabulate(window){lane => mux(C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1, data_raw(lane), 0.to[T])}
              val acc = filter.zip(data).map{case (a,b) => a*b}.reduceTree{_+_}
              local_acc(oc) = acc
            }
            // Reclaim data
            in_fifos.dropRight(2).zipWithIndex.foreach{case (f, i) => 
              val s2data = data_raw(i+2)
              val s1data = data_raw(i+1)
              val data = mux(STRIDE.value == 1, s1data, s2data)
              f.enq(data)
            }
            in_fifos.dropRight(1).last.enq(data_raw.last, STRIDE.value == 1)
            local_acc
          }{_+_}
          // Quickly copy results to output FIFO and indicate data is ready
          Foreach(OUT_CHANS by 1){oc => line_out.enq(line_out_sram(oc)); if (oc == 0) {store_ready.enq(true)}}
        }

        // Store data out
        'STORE.Pipe{
          donttouch := store_ready.deq() // Do not want to begin issuing store commands too soon
          RESULT(pt, 0::OUT_CHANS par SP) store line_out  
        }
        
      }

    }

    println(r"donttouch: $donttouch") // Guarantee compiler will not DCE

    // Compute gold
    val gold = (0::n_points_out, 0::n_chans_out){(pt, oc) => 
      val lane_prods = List.tabulate(window){lane => 
        val C = pt * stride
        Array.tabulate(n_chans_in){ic => 
          val data = if (C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1) in_data(C + lane - (window/2), ic) else 0.to[T]
          data * kernel(lane, ic, oc)
        }.reduce{_+_}
      }
      lane_prods.reduce{_+_}
    }
    val got = getMatrix(RESULT)
    printMatrix(got.t, "Got")
    printMatrix(gold.t, "Gold")

    val cksum = gold == got
    println("PASS: " + cksum + " (ConvolutionStreamReclaim)")
    assert(cksum)
  }
}


@spatial class ConvolutionStreamReclaimFlat extends SpatialTest {
  override def runtimeArgs: Args = "128 64 32 16 2"
  type T = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    // Set properties to be known at compile-time
    val window = 9
    val IN_CHANS_MAX = 256
    val OUT_CHANS_MAX = 256

    // Set parallelizations
    val P1 = 1
    val P2 = 1
    val P3 = 1
    val LP = 16
    val SP = 16

    // Create ArgIns/Outs
    val IN_POINTS = ArgIn[Int]
    val OUT_POINTS = ArgIn[Int]
    val IN_CHANS = ArgIn[Int]
    val OUT_CHANS = ArgIn[Int]
    val STRIDE = ArgIn[Int]
    val donttouch = ArgOut[Bit]
    val n_points_in = args(0).to[Int]
    val n_points_out = args(1).to[Int]
    val n_chans_in = args(2).to[Int]
    val n_chans_out = args(3).to[Int]
    val stride = args(4).to[Int]
    setArg(IN_POINTS, n_points_in)
    setArg(OUT_POINTS, n_points_out)
    setArg(IN_CHANS, n_chans_in)
    setArg(OUT_CHANS, n_chans_out)
    setArg(STRIDE, stride)

    // Create data structures
    val in_data = (0::n_points_in,0::n_chans_in){(i,j) => (i+j).to[T]}
    val kernel = (0::window, 0::n_chans_in, 0::n_chans_out){(i,j,k) => ((i+j+k)%3).to[T]}
    val DATA = DRAM[T](IN_POINTS,IN_CHANS)
    val KERNEL = DRAM[T](window, IN_CHANS, OUT_CHANS)
    val RESULT = DRAM[T](OUT_POINTS,OUT_CHANS)
    setMem(DATA, in_data)
    setMem(KERNEL, kernel)
    printMatrix(in_data.t, "Data: (n_points is leading dimension)")
    printTensor3(kernel, "Kernel: (each matrix is in_chans x out_chans)")

    Accel {
      // Create local memory for kernel values
      val kernel_sram = SRAM[T](window, IN_CHANS_MAX, OUT_CHANS_MAX)
      kernel_sram load KERNEL

      // Create stream controller to run once per output point (which includes all output channels per point)
      Stream.Foreach(OUT_POINTS by 1){ pt =>

        // Create FIFOs to hold input data (declare here so parallelizing stream results in duplication of these)
        val in_fifos = List.tabulate(window){_ => FIFO[T](IN_CHANS_MAX).conflictable}
        // Create FIFO to buffer output data
        val line_out = FIFO[T](OUT_CHANS_MAX)
        // Create signalling FIFO to mediate control
        val store_ready = FIFO[Bit](8)

        // Fetch data
        in_fifos.zipWithIndex.map{case (f, i) => 
          // Quirk of compiler's Pipe insertion, guarantee that pt * STRIDE is computed locally
          'FETCH.Pipe{ 
            if (pt == 0 || (i >= (window-STRIDE.value))) {
              val C = pt * STRIDE
              f load DATA(max(0,min(C - (window/2) + i, IN_POINTS-1)), 0::IN_CHANS par LP)
            }
          }
        }

        // Greedily consume data
        // Quirk of compiler's Pipe insertion, guarantee that pt * STRIDE is computed locally
        'COMPUTE.Pipe{ 
          val C = pt * STRIDE
          // Allocate temp accumulator
          val line_out_sram = SRAM[T](OUT_CHANS_MAX)
          // Compute partial result for each IN_CHAN
          Foreach(IN_CHANS by 1 par P1, OUT_CHANS by 1 par P2){(ic, oc) => 
            val holders = List.tabulate(window){lane => Reg[T]}
            val data_raw = in_fifos.zip(holders).map{case (f,r) => if (oc == 0) {val x = f.deq(oc == 0); r := x; x} else r.value}
            val filter = List.tabulate(window){lane => kernel_sram(lane, ic, oc)}
            val data = List.tabulate(window){lane => mux(C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1, data_raw(lane), 0.to[T])}
            val acc = filter.zip(data).map{case (a,b) => a*b}.reduceTree{_+_}
            line_out_sram(oc) = mux(ic == 0, acc, line_out_sram(oc) + acc)
            // Reclaim data
            if (oc == 0) {
              in_fifos.dropRight(2).zipWithIndex.foreach{case (f, i) => 
                val s2data = data_raw(i+2)
                val s1data = data_raw(i+1)
                val data = mux(STRIDE.value == 1, s1data, s2data)
                f.enq(data)
              }
              in_fifos.dropRight(1).last.enq(data_raw.last, STRIDE.value == 1)
            }
          }

          // Quickly copy results to output FIFO and indicate data is ready
          Foreach(OUT_CHANS by 1){oc => line_out.enq(line_out_sram(oc)); if (oc == 0) {store_ready.enq(true)}}
        }

        // Store data out
        'STORE.Pipe{
          donttouch := store_ready.deq() // Do not want to begin issuing store commands too soon
          RESULT(pt, 0::OUT_CHANS par SP) store line_out  
        }
        
      }

    }

    println(r"donttouch: $donttouch") // Guarantee compiler will not DCE

    // Compute gold
    val gold = (0::n_points_out, 0::n_chans_out){(pt, oc) => 
      val lane_prods = List.tabulate(window){lane => 
        val C = pt * stride
        Array.tabulate(n_chans_in){ic => 
          val data = if (C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1) in_data(C + lane - (window/2), ic) else 0.to[T]
          data * kernel(lane, ic, oc)
        }.reduce{_+_}
      }
      lane_prods.reduce{_+_}
    }
    val got = getMatrix(RESULT)
    printMatrix(got.t, "Got")
    printMatrix(gold.t, "Gold")

    val cksum = gold == got
    println("PASS: " + cksum + " (ConvolutionStreamReclaimFlat)")
    assert(cksum)
  }
}
@spatial class ConvolutionFlatSRAM extends SpatialTest {
   override def dseModelArgs: Args = "32 16 64 128 128 128 128 128 128 128 128 128 32 16 64"
   override def finalModelArgs: Args = "32 16 64 128 128 128 128 128 128 128 128 128 32 16 64"
   override def runtimeArgs: Args = "128 64 32 16 2"
  type T = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    // Set properties to be known at compile-time
    val window = 9
    val IN_CHANS_MAX = 256
    val OUT_CHANS_MAX = 256

    // Set parallelizations
    val P1 = 1
    val P2 = 1
    val P3 = 1
    val LP = 1
    val SP = 1

    // Create ArgIns/Outs
    val IN_POINTS = ArgIn[Int]
    val OUT_POINTS = ArgIn[Int]
    val IN_CHANS = ArgIn[Int]
    val OUT_CHANS = ArgIn[Int]
    val STRIDE = ArgIn[Int]
    val n_points_in = args(0).to[Int]
    val n_points_out = args(1).to[Int]
    val n_chans_in = args(2).to[Int]
    val n_chans_out = args(3).to[Int]
    val stride = args(4).to[Int]
    setArg(IN_POINTS, n_points_in)
    setArg(OUT_POINTS, n_points_out)
    setArg(IN_CHANS, n_chans_in)
    setArg(OUT_CHANS, n_chans_out)
    setArg(STRIDE, stride)

    // Create data structures
    val in_data = (0::n_points_in,0::n_chans_in){(i,j) => (i+j).to[T]}
    val kernel = (0::window, 0::n_chans_in, 0::n_chans_out){(i,j,k) => ((i+j+k)%3).to[T]}
    val DATA = DRAM[T](IN_POINTS,IN_CHANS)
    val KERNEL = DRAM[T](window, IN_CHANS, OUT_CHANS)
    val RESULT = DRAM[T](OUT_POINTS,OUT_CHANS)
    setMem(DATA, in_data)
    setMem(KERNEL, kernel)
    printMatrix(in_data.t, "Data: (n_points is leading dimension)")
    printTensor3(kernel, "Kernel: (each matrix is in_chans x out_chans)")

    Accel {
      // Create local memory for kernel values
      val kernel_sram = SRAM[T](window, IN_CHANS_MAX, OUT_CHANS_MAX)
      kernel_sram load KERNEL

      // Create stream controller to run once per output point (which includes all output channels per point)
      Foreach(OUT_POINTS by 1){ pt =>

        // Create FIFOs to hold input data (declare here so parallelizing stream results in duplication of these)
        val in_fifos = List.tabulate(window){_ => FIFO[T](IN_CHANS_MAX)}
        // Create FIFO to buffer output data
        val line_out = FIFO[T](OUT_CHANS_MAX)
        // Create signalling FIFO to mediate control
        val store_ready = FIFO[Bit](8)
        
        val C = pt * STRIDE

        // Fetch data
        // Parallel{
        in_fifos.zipWithIndex.map{case (f, i) => 
          Pipe{f load DATA(max(0,min(C - (window/2) + i, IN_POINTS-1)), 0::IN_CHANS par LP)}
        }
        // }

        // Allocate temp accumulator
        val line_out_sram = SRAM[T](OUT_CHANS_MAX)
        // Compute partial result for each IN_CHAN
        Foreach(IN_CHANS by 1 par P1, OUT_CHANS by 1 par P2){(ic, oc) => 
          val holders = List.tabulate(window){lane => Reg[T]}
          val data_raw = in_fifos.zip(holders).map{case (f,r) => if (oc == 0) {val x = f.deq(oc == 0); r := x; x} else r.value}
          val filter = List.tabulate(window){lane => kernel_sram(lane, ic, oc)}
          val data = List.tabulate(window){lane => mux(C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1, data_raw(lane), 0.to[T])}
          val acc = filter.zip(data).map{case (a,b) => a*b}.reduceTree{_+_}
          line_out_sram(oc) = mux(ic == 0, acc, line_out_sram(oc) + acc)
        }

        // Store data out
        RESULT(pt, 0::OUT_CHANS par SP) store line_out_sram
        
      }

    }

    // Compute gold
    val gold = (0::n_points_out, 0::n_chans_out){(pt, oc) => 
      val lane_prods = List.tabulate(window){lane => 
        val C = pt * stride
        Array.tabulate(n_chans_in){ic => 
          val data = if (C - (window/2) + lane >= 0 && C - (window/2) + lane < IN_POINTS-1) in_data(C + lane - (window/2), ic) else 0.to[T]
          data * kernel(lane, ic, oc)
        }.reduce{_+_}
      }
      lane_prods.reduce{_+_}
    }
    val got = getMatrix(RESULT)
    printMatrix(got.t, "Got")
    printMatrix(gold.t, "Gold")

    val cksum = gold == got
    println("PASS: " + cksum + " (ConvolutionFlat)")
    assert(cksum)
  }
}
