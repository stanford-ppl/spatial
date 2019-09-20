package spatial.tests.apps

import spatial.dsl._
import utils.implicits._


@spatial class ppoppconv extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "64 32 32 16 8 16 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 (1 -> 4) //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 (1 -> 4) //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 (1,2,8,16,32) //2
    val P4 = 1 (1,2,8,16,32) //2
    val P5 = 1 (1,2,8,16,32) //4
    val P6 = 1 (1,2,8,16,32) //16
    val LP = 1 (1,4,8,16,32)
    val SP = 1 (1,4,8,16,32)
    // Scalar params
    val COLS_IN = ArgIn[Int]
    val ROWS_IN = ArgIn[Int]
    val COLS_OUT = ArgIn[Int]
    val ROWS_OUT = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match

    // Shadow params (input args)
    val imax_0 = args(0).to[Int] + 1; bound(imax_0) = 479
    val imax_1 = args(1).to[Int] + 1; bound(imax_1) = 580
    val omax_0 = args(2).to[Int] + 1; bound(omax_0) = 240
    val omax_1 = args(3).to[Int] + 1; bound(omax_1) = 290
    val inchans_max = args(4).to[Int] + 1; bound(inchans_max) = 3
    val outchans_max = args(5).to[Int] + 1; bound(outchans_max) = 16
    val stride = args(6).to[Int]; bound(stride) = 2
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(COLS_IN, imax_0)
    setArg(ROWS_IN, imax_1)
    setArg(COLS_OUT, omax_0)
    setArg(ROWS_OUT, omax_1)
    setArg(INCHANS_MAX, inchans_max)
    setArg(OUTCHANS_MAX, outchans_max)
    setArg(STRIDE, stride)

    // HW Design properties
    val max_chans_in = 192
    val max_chans_out = 192
    val kernel_cols = 3
    val kernel_rows = 3

    // Memories
    val IN_PTR = DRAM[T](COLS_IN, ROWS_IN, INCHANS_MAX)
    val OUT_PTR = DRAM[T](COLS_OUT, ROWS_OUT, OUTCHANS_MAX)
    val KRNL_PTR = DRAM[T](OUTCHANS_MAX, kernel_cols, kernel_rows, INCHANS_MAX)
    val BIAS_PTR =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::COLS_IN, 0::ROWS_IN, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::COLS_OUT, 0::ROWS_OUT, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::kernel_cols, 0::kernel_rows, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(IN_PTR, input)
    setMem(OUT_PTR, output)
    setMem(KRNL_PTR, kernel)
    setMem(BIAS_PTR, bias)

    Accel{

      val kernel_sram = SRAM[T](max_chans_out, kernel_cols, kernel_rows, max_chans_in)
      val bias_sram = SRAM[T](max_chans_out)
      kernel_sram load KRNL_PTR(0::OUTCHANS_MAX, 0::kernel_cols, 0::kernel_rows, 0::INCHANS_MAX)
      bias_sram load BIAS_PTR(0::OUTCHANS_MAX)        
      def inside_image(row: I32, col: I32): Bit = row >= 0 && row < ROWS_IN && col >= 0 && col < COLS_IN
      Foreach(COLS_OUT par P1, ROWS_OUT par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.tabulate(3,3){(_,_) => SRAM[T](1,1,max_chans_in).hierarchical}
        val accum_line_upcast = SRAM[T2](max_chans_out)
        val accum_line = SRAM[T](max_chans_out)
        List.tabulate(3,3){(i,j) => Pipe{if (inside_image(idx0 - 1 + i, idx1 - 1 + j)) local_data(i)(j)(0::1, 0::1,0::INCHANS_MAX) load IN_PTR(idx0-1+i::idx0+i,idx1-1+j::idx1+j,0::INCHANS_MAX par LP)}}
        
        Foreach(OUTCHANS_MAX by 1 par P3){oc =>
          val accum = Reduce(Reg[T2])(INCHANS_MAX by 1 par P4){ ic => 
            val filter_elements = List.tabulate(3,3){(i,j) => kernel_sram(oc,i,j,ic).to[T2]}.flatten
            val data_elements = List.tabulate(3,3){(i,j) => mux(inside_image(idx0-1+i, idx1-1+j), local_data(i)(j)(0,0,ic).to[T2], 0.to[T2])}.flatten
            data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          }{_+_}
          Pipe{
            val bitshifted = if (accum < (-536870912).to[T2]) -32768.to[T]
                             else if (accum > (536870911).to[T2]) 32767.to[T]
                             else accum.bits(29::14).as[T]
            val satadd = bitshifted +! bias_sram(oc)
            accum_line(oc) = max(0.to[T], satadd)
          }
        }

        OUT_PTR(C,R,0::OUTCHANS_MAX par SP) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUT_PTR)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}


@spatial class ppoppconvbetter extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
  override def runtimeArgs: Args = "64 32 32 16 8 16 2 1"
  type T = FixPt[TRUE,_16,_0]
  type T2 = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false

    val PX = 1 //1
    val P1 = 1 (1 -> 4) //2 // Fails in sim but passes on board because conflicting dram writes
    val P2 = 1 (1 -> 4) //2 // Fails in sim but passes on board because conflicting dram writes
    val P3 = 1 (1,2,8,16,32) //2
    val P4 = 1 (1,2,8,16,32) //2
    val P5 = 1 (1,2,8,16,32) //4
    val P6 = 1 (1,2,8,16,32) //16
    val LP = 1 (1,4,8,16,32)
    val SP = 1 (1,4,8,16,32)
    // Scalar params
    val COLS_IN = ArgIn[Int]
    val ROWS_IN = ArgIn[Int]
    val COLS_OUT = ArgIn[Int]
    val ROWS_OUT = ArgIn[Int]
    val INCHANS_MAX = ArgIn[Int]
    val OUTCHANS_MAX = ArgIn[Int]

    val STRIDE = ArgIn[Int] // Assume horiz and vert stride match

    // Shadow params (input args)
    val imax_0 = args(0).to[Int] + 1; bound(imax_0) = 479
    val imax_1 = args(1).to[Int] + 1; bound(imax_1) = 580
    val omax_0 = args(2).to[Int] + 1; bound(omax_0) = 240
    val omax_1 = args(3).to[Int] + 1; bound(omax_1) = 290
    val inchans_max = args(4).to[Int] + 1; bound(inchans_max) = 3
    val outchans_max = args(5).to[Int] + 1; bound(outchans_max) = 16
    val stride = args(6).to[Int]; bound(stride) = 2
    val print_data = args(7).to[Int]

    if (P1 > 1) assert(outchans_max > 16, r"Need at least 1 burst (oc=16) if P1 = $P1")

    // Set args
    setArg(COLS_IN, imax_0)
    setArg(ROWS_IN, imax_1)
    setArg(COLS_OUT, omax_0)
    setArg(ROWS_OUT, omax_1)
    setArg(INCHANS_MAX, inchans_max)
    setArg(OUTCHANS_MAX, outchans_max)
    setArg(STRIDE, stride)

    // HW Design properties
    val max_chans_in = 192
    val max_chans_out = 192
    val kernel_cols = 3
    val kernel_rows = 3

    // Memories
    val IN_PTR = DRAM[T](COLS_IN, ROWS_IN, INCHANS_MAX)
    val OUT_PTR = DRAM[T](COLS_OUT, ROWS_OUT, OUTCHANS_MAX)
    val KRNL_PTR = DRAM[T](OUTCHANS_MAX, kernel_cols, kernel_rows, INCHANS_MAX)
    val BIAS_PTR =   DRAM[T](OUTCHANS_MAX)

    // Load data (placeholder)
    val input = (0::COLS_IN, 0::ROWS_IN, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::COLS_OUT, 0::ROWS_OUT, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::kernel_cols, 0::kernel_rows, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // Set data
    setMem(IN_PTR, input)
    setMem(OUT_PTR, output)
    setMem(KRNL_PTR, kernel)
    setMem(BIAS_PTR, bias)

    Accel{

      val kernel_sram = SRAM[T](max_chans_out, kernel_cols, kernel_rows, max_chans_in)
      val bias_sram = SRAM[T](max_chans_out)
      kernel_sram load KRNL_PTR(0::OUTCHANS_MAX, 0::kernel_cols, 0::kernel_rows, 0::INCHANS_MAX)
      bias_sram load BIAS_PTR(0::OUTCHANS_MAX)        
      def inside_image(row: I32, col: I32): Bit = row >= 0 && row < ROWS_IN && col >= 0 && col < COLS_IN
      Foreach(COLS_OUT par P1, ROWS_OUT par P2){ (C,R) =>
        val idx0 = C*STRIDE
        val idx1 = R*STRIDE
        val local_data = List.tabulate(3,3){(_,_) => SRAM[T](1,1,max_chans_in).hierarchical}
        val accum_line_upcast = SRAM[T2](max_chans_out)
        val accum_line = SRAM[T](max_chans_out)
        List.tabulate(3,3){(i,j) => Pipe{if (inside_image(idx0 - 1 + i, idx1 - 1 + j)) local_data(i)(j)(0::1, 0::1,0::INCHANS_MAX) load IN_PTR(idx0-1+i::idx0+i,idx1-1+j::idx1+j,0::INCHANS_MAX par LP)}}
        
        Pipe.II(1).Foreach(INCHANS_MAX by 1 par P3, OUTCHANS_MAX by 1 par P4){ (ic, oc) =>
          val filter_elements = List.tabulate(3,3){(i,j) => kernel_sram(oc,i,j,ic).to[T2]}.flatten
          val data_elements = List.tabulate(3,3){(i,j) => mux(inside_image(idx0-1+i, idx1-1+j), local_data(i)(j)(0,0,ic).to[T2], 0.to[T2])}.flatten
          val accum = data_elements.zip(filter_elements).map{case(a,b) => a*b}.reduceTree{_+_}
          val total = accum_line_upcast(oc) + accum
          accum_line_upcast(oc) = mux(ic == 0, accum, total)
          val bitshifted = if (total < (-536870912).to[T2]) -32768.to[T]
                           else if (total > (536870911).to[T2]) 32767.to[T]
                           else total.bits(29::14).as[T]
          val satadd = bitshifted +! bias_sram(oc)
          if (ic == INCHANS_MAX - 1) accum_line(oc) = max(0.to[T], satadd)
        }

        OUT_PTR(C,R,0::OUTCHANS_MAX par SP) store accum_line
      }
    }

    // Get results
    val results = getTensor3(OUT_PTR)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)
    printTensor3(results, "Results")

  }
}
