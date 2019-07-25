package spatial.tests.apps
import scala.language.postfixOp

/* This app is a fancier version of Differentiator and EdgeDetector.  It takes a 1D stream of data that has roughly this shape with lots of noise and shifts:
                                                       
                                                       
                             /.-.-..'..-'..-.'-'.\                          
                          /./                      \/\              
                        /-                            \
-,--.-..-'-'-...-''''-./                               \.-.-.'''-.-..-..-'.--'

It convolves this with a low-pass, differentating filter.

It reports five numbers: the position and strength of the rising edge, of the falling edge, and the area under the curve between these two edges
*/

object Helpers {
  /** h(t) = low pass = gaussian
    * y(t) = real data lineout
    * d/dt (h * y) = d/dt (h) * y
    *
    * d/dt (h) = d/dt 1/(s*sqrt(2pi)) * e^(-t^2/[2s^2]) = -t/(s^3*sqrt(2pi)) * e^(-t^2/[2s^2])
    *
    */
  def build_derivkernel(sig: Double, window: Int): Seq[Double] = {
    import scala.math._

    val data = Seq.tabulate(window){i => 
      val t = i - (window-1)/2
      val exp = -pow(t,2)/2/pow(sig,2)
      val coef = -t/pow(sig,3)/sqrt(2*Pi)
      coef * pow(E,exp)
    }
    val factor = 1.0/data.map(abs).sum
    // offsets_p = [i for i in range(0,2*offset,1)]
    // offsets_n = [offset+i for i in range(0,2*offset,1)]
    // sp_mat_p = sparse.dia_matrix((data,offsets_p),shape=(npixels,npixels)) 
    // sp_mat_n = sparse.dia_matrix((data,offsets_n),shape=(npixels,npixels))
    // norm = np.sum(sp_mat_p,axis=0)
    // return sparse.csr_matrix((sp_mat_p-sp_mat_n)/norm).T
    println(s"factor is $factor")
    val d2 = data.map(_*factor)
    println(data.map(abs).sum)
    println(d2.map(abs).sum)
    d2
  }

  def build_reference(filename: String): Unit = {
    import sys.process._
    val filename = "cat ../src/findedges.py | grep \"calibfile =\" | sed \"s/calibfile = //g\"" !!

    println(s"Calib file is $filename")
    "python3 ../src/findedges.py" !
  }

  def parse_reference(filename: String): Seq[Seq[Int]] = {
    import sys.process._
    s"echo EXAMPLE SCALA METAPROGRAMMING" !

    Seq(Seq())
  }

}

import spatial.dsl._

@spatial class FilterStream1D extends SpatialTest {

  override def runtimeArgs: Args = "50"

  type T = FixPt[TRUE, _16, _16]
  val colTileSize = 64
  val rowTileSize = 16
  val deriv_window = 40
  @struct case class score(idx: Int, v: I32)
  type P = FixPt[TRUE,_112,_0]
  @struct case class composite(rising_idx: Int, rising_v: I32, falling_idx: Int, falling_v: I32, volume: U16, p16: U16, p32: U32, p64: U64)
  def main(args: Array[String]): Unit = {

    // // Get reference edges
    // val ref_file = "../data_fs/reference/chirp-2000_interferedelay1650_photonen9.5_ncalibdelays8192_netalon0_interference.calibration"
    // // Helpers.build_reference(ref_file)
    // Helpers.parse_reference(ref_file)

    // Get hard/soft derivative kernels
    val sharp_kernel = Helpers.build_derivkernel(deriv_window/8, deriv_window)
    println(r"""Kernel: ${sharp_kernel.mkString("\t")}""")

    // Get input data
    val input_data = loadCSV2D[I16](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.csv"," ","\n")

    // Set up dram
    val COLS = ArgIn[Int]
    val ROWS = ArgIn[Int]
    val LINES_TODO = ArgIn[Int]
    setArg(COLS, input_data.cols)
    setArg(ROWS, input_data.rows)
    setArg(LINES_TODO, args(0).to[Int])
    val input_dram = DRAM[I16](ROWS, COLS)
    setMem(input_dram, input_data)
    val output_composite_dram = DRAM[composite](LINES_TODO)

    // DEBUG
    val deriv = DRAM[T](COLS)

    Accel{
      /*
                                                acc after last rising update
                                                             _                     
                                           /------------>   |_|  ------------\        
                                          /      acc after last rising update ---\ 
                                         /                   _               /    \
                                        /--------------->   |_|  -----------/      \
                                       /                                            \                          
                       input_fifo     /     sr                                       \                           
                        ________     /     _____                min idx/val           \  result_fifo
       DRAM  ----->    |_|_|_|_|    --->  |_|_|_|     deriv       _                    \   ________                                          
                                                  \     _     /  |_|  ------------------> |_|_|_|_|   ----->    DRAM
                                          kernel   >   |_|  <   max idx/val            /
                                           _____  /           \   _                   /                         
                                          |_|_|_|                |_|  ---------------/                  
                                                                                                      
       \__________________/ \________________________________________________________________/ \____________________/                                                                                           
             Stage 1                            Stage 2                                                Stage 3    
                                                                                                     
                                                                                                    
                                                                                                    
                                                                                                    
                                                                                                    
      */
      Stream.Foreach(LINES_TODO by 1 par 1){r => 
        val input_fifo = FIFO[I16](colTileSize)
        val issue = FIFO[Int](2*rowTileSize)
        val result = FIFO[composite](2*rowTileSize)

        // // DEBUG
        // val deriv_fifo = FIFO[T](32)

        // Stage 1: Load
        input_fifo load input_dram(r, 0::COLS)

        // Stage 2: Process (Force II = 1 to squeeze sr write and sr read into one cycle)
        Pipe.II(1).Foreach(COLS by 1){c => 
          val best_rising = Reg[score](score(0, -999.to[I32]))
          val best_falling = Reg[score](score(0, -999.to[I32]))
          val acc_after_rising = Reg[U16](0)
          val acc_after_falling = Reg[U16](0)
          val sr = RegFile[I16](deriv_window)
          val next = input_fifo.deq()
          sr <<= next
          acc_after_rising :+= next.as[U16]
          acc_after_falling :+= next.as[U16]
          val t = List.tabulate(deriv_window){i => sharp_kernel(i).to[T] * sr(i).to[T]}.reduceTree{_+_}
          if (c == deriv_window || (c > deriv_window && t.to[I32] > best_rising.value.v)) {
            acc_after_rising.reset()
            best_rising := score(c,t.to[I32])
          }
          if (c == deriv_window || (c > deriv_window && t.to[I32] < best_falling.value.v)) {
            acc_after_falling.reset()
            best_falling := score(c,t.to[I32])
          }
          if (c == COLS-1) {
            result.enq(composite(best_rising.value.idx, best_rising.value.v, best_falling.value.idx, best_falling.value.v, acc_after_rising - acc_after_falling, 0, 0, 0))
            issue.enq(mux(best_rising.value == score(-1,-1) || r == LINES_TODO-1 || r % rowTileSize == rowTileSize-1, mux((r+1) % rowTileSize == 0, rowTileSize, r % rowTileSize + 1), 0)) // Random math to make sure retiming puts it later
          } 

          // // DEBUG
          // if (r == LINES_TODO-1) deriv_fifo.enq(t)
        }

        // Stage 3: Store
        Pipe{
          val numel = issue.deq()
          if (numel > 0) {
            // // DEBUG
            // deriv store deriv_fifo
            // Store results
            output_composite_dram(r-(r%rowTileSize)::r-(r%rowTileSize) + numel) store result
          }
        }
      }
    }

    // // DEBUG
    // println("Debug info:")
    // printArray(Array.tabulate(input_data.cols){i => input_data(args(0).to[Int]-1, i)}, r"Row ${args(0)}")
    // printArray(getMem(deriv), r"Deriv ${args(0)}")

    val result_composite_dram = getMem(output_composite_dram)
    println("Results:")
    println("|  Rising Idx   |  Falling Idx  |     Volume      |   Rising V   |   Falling V   |")
    for (i <- 0 until LINES_TODO) {
      println(r"|      ${result_composite_dram(i).rising_idx}      |" +
              r"      ${result_composite_dram(i).falling_idx}      |" +
              r"      ${result_composite_dram(i).volume}      |"      +
              r"      ${result_composite_dram(i).rising_v}      |"    +
              r"      ${result_composite_dram(i).falling_v}      |")
    }

    val gold_rising_idx = loadCSV1D[Int](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.rising_idx",",")
    val gold_rising_v = loadCSV1D[I32](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.rising_v",",")
    val gold_falling_idx = loadCSV1D[Int](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.falling_idx",",")
    val gold_falling_v = loadCSV1D[I32](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.falling_v",",")
    val gold_volume = loadCSV1D[U16](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.volume",",")

    if (LINES_TODO == 50) { // Only have regression for 50 lines...
      val got_rising_idx = Array.tabulate(LINES_TODO){i => result_composite_dram(i).rising_idx}
      val got_rising_v = Array.tabulate(LINES_TODO){i => result_composite_dram(i).rising_v}
      val got_falling_idx = Array.tabulate(LINES_TODO){i => result_composite_dram(i).falling_idx}
      val got_falling_v = Array.tabulate(LINES_TODO){i => result_composite_dram(i).falling_v}
      val got_volume = Array.tabulate(LINES_TODO){i => result_composite_dram(i).volume}

      println(r"Correct rising_idx:  ${gold_rising_idx == got_rising_idx}")
      println(r"Correct rising_v:    ${gold_rising_v == got_rising_v}")
      println(r"Correct falling_idx: ${gold_falling_idx == got_falling_idx}")
      println(r"Correct falling_v:   ${gold_falling_v == got_falling_v}")
      println(r"Correct volume:      ${gold_volume == got_volume}")

      assert(gold_rising_idx == got_rising_idx)
      assert(gold_rising_v == got_rising_v)
      assert(gold_falling_idx == got_falling_idx)
      assert(gold_falling_v == got_falling_v)
      assert(gold_volume == got_volume)
    }

  }
}


