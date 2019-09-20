package spatial.tests.experiment
import spatial.dsl._

//class cgo_conv0 extends cgo_conv(0)
//class cgo_conv1 extends cgo_conv(1)
class cgo_conv2 extends cgo_conv(2)
//class cgo_conv3 extends cgo_conv(3)
class cgo_conv4 extends cgo_conv(4)
//class cgo_conv5 extends cgo_conv(5)
//class cgo_conv6 extends cgo_conv(6)
@spatial abstract class cgo_conv(region: scala.Int) extends SpatialTest {
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
    val P4 = 3 //2
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

    val probe = ArgOut[T2]
    Accel{
      val kernel_sram =
        if (region == 0) SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 4) SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
        else if (region == 5) SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.axesfission(List(List(0)))
        else if (region == 6) SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX).effort(0).flat.nBest.alphaBest.noblockcyclic.axesfission(List(List(0)))
        else SRAM[T](OUTPUT_CHANS_MAX, KERNEL_COLS, KERNEL_ROWS, INPUT_CHANS_MAX).effort(0).hierarchical
      kernel_sram load KERNEL_DATA(0::OUTPUT_CHANS, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INPUT_CHANS)
      val reg = Reg[T2](0).conflictable
      val rowspan = if (STRIDE.value == 1) INPUT_ROWS-(KERNEL_ROWS-STRIDE) else (INPUT_ROWS-(KERNEL_ROWS-STRIDE) >> 1)
      val colspan = if (STRIDE.value == 1) INPUT_COLS-(KERNEL_COLS-STRIDE) else (INPUT_COLS-(KERNEL_COLS-STRIDE) >> 1)
      Foreach(rowspan par P1){ R =>
        val row = R*STRIDE.value
        Foreach(colspan par P2){ C =>
          val col = C*STRIDE.value
          Foreach(INPUT_CHANS by 1 par P3){ ic =>
            Foreach(OUTPUT_CHANS by 1 par P4){ oc =>
              val filter_elements = List.tabulate(3){i => List.tabulate(3){j =>
                kernel_sram(oc,j,i,ic).to[T2]
              }}.flatten
              val accum = filter_elements.reduce{_+_}
              reg := accum
            }
          }
          // RELU
          Foreach(OUTPUT_CHANS by 1 par P6){i =>
            println("hi")
          }
          val dummy = SRAM[T](OUTPUT_CHANS_MAX)
          OUTPUT_DATA(col/STRIDE,row/STRIDE,0::OUTPUT_CHANS par storePar) store dummy
          probe := reg.value
        }
      }
    }

    println(probe)
    assert(getArg(probe).to[Int] == 5)
//    // Get results
//    val results = getTensor3(OUTPUT_DATA)
//    printTensor3(results)

  }
}

//class cgo_fft0 extends cgo_fft(0)
//class cgo_fft1 extends cgo_fft(1)
//class cgo_fft2 extends cgo_fft(2)
//class cgo_fft3 extends cgo_fft(3)
//class cgo_fft4 extends cgo_fft(4)
//class cgo_fft5 extends cgo_fft(5)
//class cgo_fft6 extends cgo_fft(6)
@spatial abstract class cgo_fft(region: scala.Int) extends SpatialTest {
 /*
    Concerns: Not sure why machsuite makes a data_x and DATA_x when they only dump values from one row of DATA_x to data_x and back
              Also, is their algorithm even correct?!  It's very suspicion and I can even comment out some of their code and it still passes....
 */

  type T = FixPt[TRUE,_16,_16]

  def main(args: Array[String]): Unit = {

    val dim = 512
    val THREADS = 64
    val stride = THREADS
    val M_SQRT1_2 = 0.70710678118654752440.to[T]
    val TWOPI = 6.28318530717959.to[T]

    val data_x = loadCSV1D[T](s"$DATA/fft/fft_transpose_x.csv", "\n").reshape(8,stride)
    val data_y = loadCSV1D[T](s"$DATA/fft/fft_transpose_y.csv", "\n").reshape(8,stride)

    val work_x_dram = DRAM[T](8,stride)
    val work_y_dram = DRAM[T](8,stride)
    val result_x_dram = DRAM[T](8,stride)
    val result_y_dram = DRAM[T](8,stride)

    val probe = ArgOut[T]
    setMem(work_x_dram, data_x)
    setMem(work_y_dram, data_y)

    Accel{
      val work_x_sram =
        if (region == 0) SRAM[T](8,stride).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[T](8,stride).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[T](8,stride).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[T](8,stride).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 4) SRAM[T](8,stride).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
        else if (region == 5) SRAM[T](8,stride).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.axesfission(List(List(0)))
        else if (region == 6) SRAM[T](8,stride).effort(0).flat.nBest.alphaBest.noblockcyclic.axesfission(List(List(0)))
        else SRAM[T](8,stride).effort(0).hierarchical
      val work_y_sram =
        if (region == 0) SRAM[T](8,stride).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[T](8,stride).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[T](8,stride).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[T](8,stride).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 4) SRAM[T](8,stride).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
        else if (region == 5) SRAM[T](8,stride).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.axesfission(List(List(0)))
        else if (region == 6) SRAM[T](8,stride).effort(0).flat.nBest.alphaBest.noblockcyclic.axesfission(List(List(0)))
        else SRAM[T](8,stride).effort(0).hierarchical
      val smem = SRAM[T](8*8*9)

      work_x_sram load work_x_dram
      work_y_sram load work_y_dram

      val reversed_LUT = LUT[Int](8)(0,4,2,6,1,5,3,7)

      def twiddles8(tid: I32, i: Int, N: Int): Unit = {
        Foreach(1 until 8 by 1) { j =>
          val phi = -TWOPI*(i.to[T]*reversed_LUT(j).to[T] / N.to[T])
          val phi_shifted = phi + TWOPI/2
          val beyond_left = phi_shifted < -TWOPI.to[T]/4
          val beyond_right = phi_shifted > TWOPI.to[T]/4
          val phi_bounded = mux(beyond_left, phi_shifted + TWOPI.to[T]/2, mux(beyond_right, phi_shifted - TWOPI.to[T]/2, phi_shifted))
          val phi_x = cos_taylor(phi_bounded) * mux(beyond_left || beyond_right, 1.to[T], -1.to[T]) // cos(real phi)
          val phi_y = sin_taylor(phi_bounded) * mux(beyond_left || beyond_right, 1.to[T], -1.to[T]) // sin(real phi)
          val temp_x = work_x_sram(j, tid)
          work_x_sram(j, tid) = temp_x * phi_x - work_y_sram(j, tid) * phi_y
          work_y_sram(j, tid) = temp_x * phi_y + work_y_sram(j, tid) * phi_x
        }
      }

      def FFT2(tid: I32, id0: Int, id1: Int):Unit = {
        val temp_x = work_x_sram(id0, tid)
        val temp_y = work_y_sram(id0, tid)
        work_x_sram(id0, tid) = temp_x + work_x_sram(id1, tid)
        work_y_sram(id0, tid) = temp_y + work_y_sram(id1, tid)
        work_x_sram(id1, tid) = temp_x - work_x_sram(id1, tid)
        work_y_sram(id1, tid) = temp_y - work_y_sram(id1, tid)
      }

      def FFT4(tid: I32, base: Int):Unit = {
        val exp_LUT = LUT[T](2)(0, -1)
        Foreach(0 until 2 by 1) { j =>
          FFT2(tid, base+j.to[I32], 2+base+j.to[I32])
        }
        val temp_x = work_x_sram(base+3,tid)
        work_x_sram(base+3,tid) = temp_x * exp_LUT(0) - work_y_sram(base+3,tid)*exp_LUT(1)
        work_y_sram(base+3,tid) = temp_x * exp_LUT(1) - work_y_sram(base+3,tid)*exp_LUT(0)
        Foreach(0 until 2 by 1) { j =>
          FFT2(tid, base+2*j.to[I32], 1+base+2*j.to[I32])
        }
      }

      def FFT8(tid: I32):Unit = {
        Foreach(0 until 4 by 1) { i =>
          FFT2(tid, i, 4+i.to[I32])
        }
        Foreach(0 until 3 by 1) { i =>
          val exp_LUT = LUT[T](2,3)( 1,  0, -1,
                                      -1, -1, -1)
          val temp_x = work_x_sram(5+i.to[I32], tid)
          val mul_factor = mux(i.to[I32] == 1, 1.to[T], M_SQRT1_2)
          work_x_sram(5+i.to[I32], tid) = (temp_x * exp_LUT(0,i) - work_y_sram(5+i.to[I32],tid) * exp_LUT(1,i))*mul_factor
          work_y_sram(5+i.to[I32], tid) = (temp_x * exp_LUT(1,i) + work_y_sram(5+i.to[I32],tid) * exp_LUT(0,i))*mul_factor
        }
        // FFT4
        Sequential.Foreach(0 until 2 by 1) { ii =>
          val base = 4*ii.to[I32]
          FFT4(tid, base)
        }

      }

      // Loop 1
      Sequential.Foreach(THREADS by 1) { tid =>
        FFT8(tid)
        twiddles8(tid, tid, dim)
      }

      val shuffle_lhs_LUT = LUT[Int](8)(0,4,1,5,2,6,3,7)
      val shuffle_rhs_LUT = LUT[Int](8)(0,1,4,5,2,3,6,7)

      // Loop 2
      Foreach(THREADS by 1) { tid =>
        val sx = 66
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = hi*8+lo // * here but >> above????
        Foreach(8 by 1) { i =>
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_rhs_LUT(i)
          smem(lhs_factor*sx + offset) = work_x_sram(rhs_factor, tid)
        }
      }

      // Loop 3
      Foreach(THREADS by 1) { tid =>
        val sx = 8
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = lo*66 + hi
        Foreach(8 by 1) { i =>
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_lhs_LUT(i) // [sic]
          work_x_sram(lhs_factor, tid) = smem(rhs_factor*sx+offset)
        }
      }

      // Loop 4
      Foreach(THREADS by 1) { tid =>
        val sx = 66
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = hi*8+lo // * here but >> above????
        Foreach(8 by 1) { i =>
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_rhs_LUT(i)
          smem(lhs_factor*sx + offset) = work_y_sram(rhs_factor, tid)
        }
      }

      // Loop 5
      Foreach(THREADS by 1) { tid =>
        val sx = 8
        val hi = tid.to[I32] >> 3;
        val lo = tid.to[I32] & 7;
        val offset = lo*66+hi
        Foreach(8 by 1) { i =>
          work_y_sram(i, tid) = smem(i.to[I32]*sx+offset)
        }
      }

      // Loop 6
      Sequential.Foreach(THREADS by 1) { tid =>
        FFT8(tid)
        val hi = tid.to[I32] >> 3
        twiddles8(tid, hi, 64)
      }

      // Loop 7
      Foreach(THREADS by 1) { tid =>
        val sx = 72
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = hi*8 + lo
        Foreach(8 by 1) { i =>
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_rhs_LUT(i)
          smem(lhs_factor * sx + offset) = work_x_sram(rhs_factor, tid)
        }
      }

      // Loop 8
      Foreach(THREADS by 1) { tid =>
        val sx = 8
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = hi*72 + lo
        Foreach(8 by 1) { i =>
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_lhs_LUT(i) // [sic]
          work_x_sram(lhs_factor, tid) = smem(rhs_factor * sx + offset)
        }
      }

      // Loop 9
      Foreach(THREADS by 1) { tid =>
        val sx = 72
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = hi*8 + lo
        Foreach(8 by 1) { i =>
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_rhs_LUT(i)
          smem(lhs_factor * sx + offset) = work_y_sram(rhs_factor, tid)
        }
      }

      // Loop 10
      Foreach(THREADS by 1) { tid =>
        val sx = 8
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = hi*72 + lo
        Foreach(8 by 1) { i =>
          work_y_sram(i, tid) = smem(i.to[I32] * sx + offset)
        }
      }

      // Loop 11
      Sequential.Foreach(THREADS by 1) { tid =>
        FFT8(tid)
        // Do the indirect "reversing" (LUT = 0,4,2,6,1,5,3,7)
        val tmem_x = SRAM[T](8)
        val tmem_y = SRAM[T](8)
        Foreach(8 by 1) { i =>
          tmem_x(reversed_LUT(i)) = work_x_sram(i, tid)
          tmem_y(reversed_LUT(i)) = work_y_sram(i, tid)
        }
        Foreach(8 by 1) { i =>
          work_x_sram(i, tid) = tmem_x(i)
          work_y_sram(i, tid) = tmem_y(i)
        }
      }

      result_x_dram store work_x_sram
      result_y_dram store work_y_sram
    }

    val result_x = getMatrix(result_x_dram)
    val result_y = getMatrix(result_y_dram)
    // val gold_x = loadCSV1D[T](s"$DATA/fft/fft_transpose_x_gold.csv", "\n").reshape(8,stride)
    // val gold_y = loadCSV1D[T](s"$DATA/fft/fft_transpose_y_gold.csv", "\n").reshape(8,stride)
    val gold_x = loadCSV1D[T](s"$DATA/fft/fft_transpose_x_gold.csv", "\n").reshape(8,stride)
    val gold_y = loadCSV1D[T](s"$DATA/fft/fft_transpose_y_gold.csv", "\n").reshape(8,stride)

    printMatrix(gold_x, "Gold x: ")
    println("")
    printMatrix(result_x, "Result x: ")
    println("")
    printMatrix(gold_y, "Gold y: ")
    println("")
    printMatrix(result_y, "Result y: ")
    println("")

    // printMatrix(gold_x.zip(result_x){(a,b) => abs(a-b)}, "X Diff")

    val margin = 0.5.to[T]
    val cksumX = gold_x.zip(result_x){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    val cksumY = gold_y.zip(result_y){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    val cksum = cksumX && cksumY
//    println("X cksum: " + cksumX + ", Y cksum: " + cksumY)
//    println("PASS: " + cksum + " (FFT_Transpose)")
    assert(cksum)
  }
}
//
//class cgo_gibbs0 extends cgo_gibbs(0)
//class cgo_gibbs1 extends cgo_gibbs(1)
//class cgo_gibbs2 extends cgo_gibbs(2)
//class cgo_gibbs3 extends cgo_gibbs(3)
//class cgo_gibbs4 extends cgo_gibbs(4)
//class cgo_gibbs5 extends cgo_gibbs(5)
//class cgo_gibbs6 extends cgo_gibbs(6)
@spatial abstract class cgo_gibbs(region: scala.Int) extends SpatialTest {
  override def dseModelArgs: Args = "25 -2 99"
  override def finalModelArgs: Args = "25 0 -1 -2 -3"
  override def runtimeArgs: Args = "25 0.3 1"


  type T = FixPt[TRUE,_16,_16] // FixPt[TRUE,_32,_32]
  type PROB = FixPt[FALSE, _0, _8]


  def main(args: Array[String]): Unit = {

    val COLS = 64
    val ROWS = 32
    val lut_size = 9
    val border = -1

    val I = args(0).to[Int] // Number of iterations to run
    val J = args(1).to[T] // Energy scalar for edge
    val J_b = args(2).to[T] // Energy scalar for external field

    // Args
    val iters = ArgIn[Int]
    val exp_negbias = ArgIn[T]
    val exp_posbias = ArgIn[T]

    // Set up lut for edge energy ratio
    // 𝚺 x_j * x_i can be from -4 to +4
    val exp_data = Array.tabulate[T](lut_size){i =>
      val x = i - 4
      exp(x.to[Float]*J.to[Float] * -2.to[Float]).to[T]
    }
    // Set up args for bias energy ratio
    val exp_neg = exp(-J_b.to[Float]*2.to[Float]).to[T]
    val exp_pos = exp(J_b.to[Float]*2.to[Float]).to[T]

    // Debugging
    printArray(exp_data, "exp data")
    println("neg: " + exp_neg)
    println("pos: " + exp_pos)

    // Set initial and bias patterns:
    // Checkerboard
    val grid_init = (0::ROWS, 0::COLS){(i,j) => if ((i+j)%2 == 0) -1.to[Int] else 1.to[Int]}
    // // Square
    // val grid_init = (0::ROWS, 0::COLS){(i,j) => if (i > ROWS/4 && i < 3*ROWS/4 && j > COLS/4 && j < 3*COLS/4) -1.to[Int] else 1.to[Int]}

    val par_load = 1
    val par_store = 1
    val x_par = 3

    // Square
    val bias_matrix = (0::ROWS, 0::COLS){(i,j) => if (i > ROWS/4 && i < 3*ROWS/4 && j > COLS/4 && j < 3*COLS/4) -1.to[Int] else 1.to[Int]}

    val exp_lut = DRAM[T](lut_size)
    val grid_dram = DRAM[Int](ROWS,COLS)
    val bias_dram = DRAM[Int](ROWS,COLS)

    setMem(grid_dram, grid_init)
    setMem(bias_dram, bias_matrix)
    setMem(exp_lut, exp_data)
    setArg(exp_negbias, exp_neg)
    setArg(exp_posbias, exp_pos)
    setArg(iters, I)

    Accel{
//      val exp_sram = SRAM[T](lut_size)
      // val grid_sram = SRAM[Int](ROWS,COLS).flat
      val grid_sram =
        if (region == 0) SRAM[Int](ROWS,COLS).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[Int](ROWS,COLS).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[Int](ROWS,COLS).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[Int](ROWS,COLS).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 4) SRAM[Int](ROWS,COLS).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
        else if (region == 5) SRAM[Int](ROWS,COLS).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.axesfission(List(List(0)))
        else if (region == 6) SRAM[Int](ROWS,COLS).effort(0).flat.nBest.alphaBest.noblockcyclic.axesfission(List(List(0)))
        else SRAM[Int](ROWS,COLS).effort(0).hierarchical
//      exp_sram load exp_lut
      grid_sram load grid_dram(0::ROWS, 0::COLS par par_load)
      // Issue #187
//      val bias_sram = SRAM[Int](ROWS,COLS).effort(1)
//      bias_sram load bias_dram(0::ROWS, 0::COLS par par_load)


      Foreach(iters by 1) { iter =>
        def rowv(r: I32): Bit = {r >= 0 && r < ROWS}
        def colv(c: I32): Bit = {c >= 0 && c < COLS}
        // Foreach(ROWS by 1 by x_par) { i =>
        //   // Update each point in active row
        //   Parallel{
        //     List.tabulate(x_par){this_body =>
        Foreach(ROWS by 1 par x_par) { i =>
          // Update each point in active row
          val this_body = (i % x_par)*2
          Foreach(-this_body until COLS by 1) { j =>
            // val col = j - this_body
            val N = mux(rowv(i+1), grid_sram(i+1, j), -1)
            val E = mux(colv(j+1), grid_sram(i, j+1), -1)
            val S = mux(rowv(i-1), grid_sram(i-1, j), -1)
            val W = mux(colv(j-1), grid_sram(i, j-1), -1)
            val self = grid_sram(i,j)
            val sum = (N+E+S+W)*self
            if (j >= 0 && j < COLS) {
              grid_sram(i,j) = sum
            }
          }
            // }
          // }
        }
      }
      grid_dram(0::ROWS, 0::COLS par par_store) store grid_sram
    }

    val result = getMatrix(grid_dram)
    println("Ran for " + I + " iters.")
    // printMatrix(result, "Result matrix")

    print(" ")
    for( j <- 0 until COLS) { print("-")}
    for( i <- 0 until ROWS) {
      println("")
      print("|")
      for( j <- 0 until COLS) {
        if (result(i,j) == -1) {print("X")} else {print(" ")}
      }
      print("|")
    }
    println(""); print(" ")
    for( j <- 0 until COLS) { print("-")}
    println("")

    val blips_inside = (0::ROWS, 0::COLS){(i,j) =>
      if (i > ROWS/4 && i < 3*ROWS/4 && j > COLS/4 && j < 3*COLS/4) {
        if (result(i,j) != -1) 1 else 0
      } else { 0 }
    }.reduce{_+_}
    val blips_outside = (0::ROWS, 0::COLS){(i,j) =>
      if (i > ROWS/4 && i < 3*ROWS/4 && j > COLS/4 && j < 3*COLS/4) {
        0
      } else {
        if (result(i,j) != 1) 1 else 0
      }
    }.reduce{_+_}
    println("Found " + blips_inside + " blips inside the bias region and " + blips_outside + " blips outside the bias region")
    val cksum = (blips_inside + blips_outside) < (ROWS*COLS/8)
    println("PASS: " + cksum + " (Gibbs_Ising2D)")
    assert(cksum)
  }
}

class cgo_grid0 extends cgo_grid(0)
class cgo_grid1 extends cgo_grid(1)
class cgo_grid2 extends cgo_grid(2)
class cgo_grid3 extends cgo_grid(3)
class cgo_grid4 extends cgo_grid(4)
class cgo_grid5 extends cgo_grid(5)
class cgo_grid6 extends cgo_grid(6)
@spatial abstract class cgo_grid(region: scala.Int) extends SpatialTest {
  override def dseModelArgs: Args = "0 4 0 4 0 4 4 4 1"
  override def finalModelArgs: Args = "0 4 0 4 0 4 4 4 4 4 4 4 4 4"

 /*

  Moleckaler Dynamics via the grid, a digital frontier





                            ←      BLOCK_SIDE     →
                ↗
                          __________________________________
       BLOCK_SIDE        /                                  /|
                        /                                  / |
        ↙              /                                  /  |
                      /_________________________________ /   |
                     |           b1                     |    |
           ↑         |        ..  ..  ..                |    |
                     |       - - - - - -                |    |
                     |      :``::``::``:                |    |
           B         |    b1:..::__::..:b1              |    |
           L         |      - - /_/| - -                |    |
           O         |     :``:|b0||:``:                |    |
           C         |   b1:..:|__|/:..:b1              |    |
           K         |      - - - -  - -                |    |
           |         |     :``::``: :``:                |    |
           S         |   b1:..::..: :..:b1              |    |
           I         |          b1                      |    |
           D         |                                  |   /
           E         |                                  |  /
                     |                                  | /
           ↓         |                                  |/
                      ``````````````````````````````````       * Each b0 contains up to "density" number of atoms
                                                               * For each b0, and then for each atom in b0, compute this atom's
                                                                    interactions with all atoms in the adjacent (27) b1's
                                                               * One of the b1's will actually be b0, so skip this contribution

 */

  // Max pos seems to be about 19
  type T = FixPt[TRUE, _12, _20]
  @struct case class XYZ(x: T, y: T, z: T)


  def main(args: Array[String]): Unit = {

    val N_ATOMS = 256
    val DOMAIN_EDGE = 20
    val BLOCK_SIDE = 4
    val density = 10
    val density_aligned = density + (8 - (density % 8))
    val lj1 = 1.5.to[T]
    val lj2 = 2.to[T]

    val par_load = 1 // Wider data type
    val par_store = 1 // Wider data type
    val loop_grid0_x = 1 (1 -> 1 -> 16)
    val loop_grid0_y = 1 (1 -> 1 -> 16)
    val loop_grid0_z = 1 (1 -> 1 -> 16)
    val loop_grid1_x = 2 (1 -> 1 -> 16)
    val loop_grid1_y = 1 (1 -> 1 -> 16)
    val loop_grid1_z = 2 (1 -> 1 -> 16)
    val loop_p =       1 (1 -> 1 -> 16)
    val loop_q =       3 (1 -> 1 -> 16)

    val raw_npoints = Array[Int](4,4,3,4,5,5,2,1,1,8,4,8,3,3,7,5,4,5,6,2,2,4,4,3,3,4,7,2,3,2,
                                 2,1,7,1,3,7,6,3,3,4,3,4,5,5,6,4,2,5,7,6,5,4,3,3,5,4,4,4,3,2,3,2,7,5)
    val npoints_data = raw_npoints.reshape(BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE)

    val raw_dvec = loadCSV1D[T](s"$DATA/MD/grid_dvec.csv", "\n")
    // Strip x,y,z vectors from raw_dvec
    val dvec_x_data = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => raw_dvec(i*BLOCK_SIDE*BLOCK_SIDE*density*3 + j*BLOCK_SIDE*density*3 + k*density*3 + 3*l)}
    val dvec_y_data = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => raw_dvec(i*BLOCK_SIDE*BLOCK_SIDE*density*3 + j*BLOCK_SIDE*density*3 + k*density*3 + 3*l+1)}
    val dvec_z_data = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => raw_dvec(i*BLOCK_SIDE*BLOCK_SIDE*density*3 + j*BLOCK_SIDE*density*3 + k*density*3 + 3*l+2)}

    val dvec_x_dram = DRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
    val dvec_y_dram = DRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
    val dvec_z_dram = DRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
    val force_x_dram = DRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
    val force_y_dram = DRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
    val force_z_dram = DRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
    val npoints_dram = DRAM[Int](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE)

    setMem(dvec_x_dram, dvec_x_data)
    setMem(dvec_y_dram, dvec_y_data)
    setMem(dvec_z_dram, dvec_z_data)
    setMem(npoints_dram, npoints_data)
    val probe = ArgOut[T]

    Accel{
      val reg = Reg[T](0).conflictable
      val dvec_x_sram =
        if (region == 0) SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 4) SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
        else if (region == 5) SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.axesfission(List(List(0)))
        else if (region == 6) SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density).effort(0).flat.nBest.alphaBest.noblockcyclic.axesfission(List(List(0)))
        else SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density).effort(0).hierarchical
//      val dvec_y_sram = SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density).hierarchical
//      val dvec_z_sram = SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density).hierarchical
      val npoints_sram = SRAM[Int](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE).hierarchical
//      val force_x_sram = SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
//      val force_y_sram = SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
//      val force_z_sram = SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)

      dvec_x_sram load dvec_x_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density par par_load)
//      dvec_y_sram load dvec_y_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density par par_load)
//      dvec_z_sram load dvec_z_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density par par_load)
      npoints_sram load npoints_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE par par_load)

      // Iterate over each block
      'ATOM1LOOP.Foreach(BLOCK_SIDE by 1 par loop_grid0_x, BLOCK_SIDE by 1 par loop_grid0_y, BLOCK_SIDE by 1 par loop_grid0_z){(b0x,b0y,b0z) =>
        // Iterate over each point in this block, considering boundaries
        val b0_cube_forces = SRAM[XYZ](density)
        val b1x_start = max(0.to[Int],b0x-1.to[Int])
        val b1x_end = min(BLOCK_SIDE.to[Int], b0x+2.to[Int])
        val b1y_start = max(0.to[Int],b0y-1.to[Int])
        val b1y_end = min(BLOCK_SIDE.to[Int], b0y+2.to[Int])
        val b1z_start = max(0.to[Int],b0z-1.to[Int])
        val b1z_end = min(BLOCK_SIDE.to[Int], b0z+2.to[Int])
        // Iterate over points in b0
        val p_range = npoints_sram(b0x, b0y, b0z)
//        println(r"\npt $b0x $b0y $b0z, atom2bounds ${b1x_start}-${b1x_end}, ${b1y_start}-${b1y_end}, ${b1z_start}-${b1z_end}")
        'ATOM2LOOP.MemReduce(b0_cube_forces)(0 until b1x_end by 1 par loop_grid1_x, b1y_start until b1y_end by 1, 0 until b1z_end by 1 par loop_grid1_z) { (b1x, b1y, b1z) =>
          val b1_cube_contributions = SRAM[XYZ](density).buffer
          val q_range = npoints_sram(b1x, b1y, b1z)
//          println(r"  p_range/q_range for $b0x $b0y $b0z - $b1x $b1y $b1z = ${p_range} ${q_range}")
          'PLOOP.Foreach(0 until p_range par loop_p) { p_idx =>
            val px = random[T](10)//dvec_x_sram(b0x, b0y, b0z, p_idx)
//            val py = dvec_y_sram(b0x, b0y, b0z, p_idx)
//            val pz = dvec_z_sram(b0x, b0y, b0z, p_idx)
            val q_sum = Reg[XYZ](XYZ(0.to[T], 0.to[T], 0.to[T])).conflictable
            'QLOOP.Foreach(0 until q_range par loop_q) { q_idx =>
              val qx = dvec_x_sram(b1x, b1y, b1z, q_idx)
//              val qy = dvec_y_sram(b1x, b1y, b1z, q_idx)
//              val qz = dvec_z_sram(b1x, b1y, b1z, q_idx)
              val tmp = if ( !(b0x == b1x && b0y == b1y && b0z == b1z && p_idx == q_idx) ) { // Skip self
//                println(r"    $b1x $b1y $b1z ${q_idx} = $qx $qy $qz")
                XYZ(px - qx,0,0)
              } else {
                XYZ(0.to[T], 0.to[T], 0.to[T])
              }
              q_sum := tmp
            }
//            println(r"    $b0x $b0y $b0z ${p_idx}: contribution of $b1x $b1y $b1z = ${q_sum}")
            b1_cube_contributions(p_idx) = q_sum
          }
          Foreach(p_range until density) { i => println(i) } // Zero out untouched interactions
          b1_cube_contributions
        }{(a,b) => XYZ(a.x + b.x, a.y + b.y, a.z + b.z)}

        Foreach(0 until density) { i =>
          reg := b0_cube_forces(i).x + b0_cube_forces(i).y + b0_cube_forces(i).z
//          println(r"total contribution @ $b0x $b0y $b0z $i = ${b0_cube_forces(i)}")
//          force_x_sram(b0x,b0y,b0z,i) = b0_cube_forces(i).x
//          force_y_sram(b0x,b0y,b0z,i) = b0_cube_forces(i).y
//          force_z_sram(b0x,b0y,b0z,i) = b0_cube_forces(i).z
        }
      }
//      force_x_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density par par_load) store force_x_sram
//      force_y_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density par par_load) store force_y_sram
//      force_z_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density par par_load) store force_z_sram
  probe := reg.value
    }
  println(probe)
    // No need to align after bug #195 fixed
    val force_x_received = getTensor4(force_x_dram)
    val force_y_received = getTensor4(force_y_dram)
    val force_z_received = getTensor4(force_z_dram)
    val raw_force_gold = loadCSV1D[T](s"$DATA/MD/grid_gold.csv", "\n")
    val force_x_gold = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => raw_force_gold(i*BLOCK_SIDE*BLOCK_SIDE*density*3 + j*BLOCK_SIDE*density*3 + k*density*3 + 3*l)}
    val force_y_gold = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => raw_force_gold(i*BLOCK_SIDE*BLOCK_SIDE*density*3 + j*BLOCK_SIDE*density*3 + k*density*3 + 3*l+1)}
    val force_z_gold = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => raw_force_gold(i*BLOCK_SIDE*BLOCK_SIDE*density*3 + j*BLOCK_SIDE*density*3 + k*density*3 + 3*l+2)}


    printTensor4(force_x_gold, "Gold x:")
    printTensor4(force_x_received, "Received x:")
    printTensor4(force_y_gold, "Gold y:")
    printTensor4(force_y_received, "Received y:")
    printTensor4(force_z_gold, "Gold z:")
    printTensor4(force_z_received, "Received z:")


    val margin = 0.001.to[T]
    val validateZ = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => if (abs(force_z_gold(i,j,k,l) - force_z_received(i,j,k,l)) < margin) 1 else 0}
    printTensor4(validateZ, "Z matchup")
    val cksumx = force_x_gold.zip(force_x_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksumy = force_y_gold.zip(force_y_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksumz = force_z_gold.zip(force_z_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    println("X: " + cksumx + ", Y:" + cksumy + ", Z: " + cksumz)
    val cksum = cksumx && cksumy && cksumz
    println("PASS: " + cksum + " (MD_Grid)")
    assert(cksum)
  }
}

class cgo_sgd0 extends cgo_sgd(0)
class cgo_sgd1 extends cgo_sgd(1)
class cgo_sgd2 extends cgo_sgd(2)
class cgo_sgd3 extends cgo_sgd(3)
class cgo_sgd4 extends cgo_sgd(4)
class cgo_sgd5 extends cgo_sgd(5)
class cgo_sgd6 extends cgo_sgd(6)
@spatial abstract class cgo_sgd(region: scala.Int) extends SpatialTest {
  override def runtimeArgs: Args = "40 64 0.0001"

  type TM = FixPt[TRUE,_16,_16]
  type TX = FixPt[TRUE,_16,_16]
  val modelSize = 64
  val tileSize = 32
  val innerPar = 4
  val outerPar = 1 // Not used right now?
  val margin = 1


  def sgdminibatch(x_in: Array[TX], y_in: Array[TX], alpha: TM, epochs: Int, nn: Int): Array[TM] = {
    val E = ArgIn[Int]
    val N = ArgIn[Int]
    val A = ArgIn[TM]
    val D = modelSize

    val ip = innerPar (1 -> 1)
    val op = outerPar (1 -> 1)
    val P1 = 2
    val P2 = 3
    val P3 = 4
    val P4 = 5

    setArg(E, epochs)
    setArg(N, nn)
    setArg(A, alpha)

    val x = DRAM[TX](N,D)
    val y = DRAM[TX](N)
    val result = DRAM[TM](D)

    setMem(x, x_in)
    setMem(y, y_in)

    Accel {
//      val y_tile = SRAM[TX](tileSize)
      val sgdmodel = SRAM[TM](D)
      val x_tile =
        if (region == 0) SRAM[TX](tileSize,D).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[TX](tileSize,D).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[TX](tileSize,D).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[TX](tileSize,D).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 4) SRAM[TX](tileSize,D).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
        else if (region == 5) SRAM[TX](tileSize,D).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.axesfission(List(List(0)))
        else if (region == 6) SRAM[TX](tileSize,D).effort(0).flat.nBest.alphaBest.noblockcyclic.axesfission(List(List(0)))
        else SRAM[TX](tileSize,D).effort(0).hierarchical

//      Foreach(D by 1){i => sgdmodel(i) = 0.to[TM]}
      Sequential.Foreach(E by 1) { e =>

        Sequential.Foreach (N by tileSize) { b =>
          Foreach(random[Int](10) by 1){i => println(i)}
          x_tile load x(b::b+tileSize, 0::D)
          val y_err = SRAM[TX](tileSize)
          Foreach(tileSize by 1 par P1) {i =>
            val y_hat = Reg[TX]
            Reduce(y_hat)(D by 1 par P3){ j => x_tile(i,j)  &  random[TX](10).to[TX] }{_ & _}
            y_err(i) = random[TX](10) - y_hat.value
          }
          Foreach(D by 1 par P2) { i =>
            val raw_update = Reg[TX]
            Reduce(raw_update)(tileSize by 1 par P4){ j => x_tile(j,i)  &  y_err(j) }{_ & _}
            sgdmodel(i) = random[TM](10) + raw_update.value.to[TM] & A
          }
        }
      }
      result(0::D par ip) store sgdmodel

    }

    getMem(result)
  }


  def main(args: Array[String]): Unit = {
    val E = args(0).to[Int]
    val N = args(1).to[Int]
    val A = args(2).to[TM] // Should be somewhere around 0.0001 for point-wise sgd
    val D = modelSize

    val sX = Array.fill(N){ Array.fill(D){ random[TX](3.to[TX]) + 1.to[TX]} }
    val ideal_model = Array.tabulate(D){ i => 2.to[TM] }
    val sY = Array.tabulate(N){i => ideal_model.zip(sX.apply(i)){case (a,b) => a.to[TX]*b}.reduce{_+_}}
    val id = Array.tabulate(D){ i => i }
    val ep = Array.tabulate(E){ i => i }

    val result = sgdminibatch(sX.flatten, sY, A, E, N)

    val cksum = ideal_model.zip(result){ case (a,b) => abs(a - b) < margin }.reduce{_&&_}
    printArray(result, "result: ")
    printArray(ideal_model, "gold: ")
    println("PASS: " + cksum  + " (SGD_minibatch)")
    assert(cksum)
  }
}