package spatial.tests.feature.streaming

import spatial.dsl._
import spatial.lang.CXPPixelBus
import spatial.tests.apps._

@spatial class CXP1DFilter extends SpatialTest {
  override def backends = DISABLED



  type T = FixPt[TRUE, _16, _16]
  val colTileSize = 512
  val rowTileSize = 64
  val deriv_window = 40
  def main(args: Array[String]): Unit = {

    val deriv_window = 40

    // Get hard/soft derivative kernels
    val sharp_kernel = Helpers.build_derivkernel(deriv_window/8, deriv_window)
    println(r"""Kernel: ${sharp_kernel.mkString("\t")}""")


    val rows = 99999
    val colTileSize = 64
    val rowTileSize = 16
    val interleave_factor = 4
    val bus_bits = 256
    val line_bits = 1024*16
    val pxl_bits = 16
    val interleave_rows = rows/interleave_factor // Rows of packed data structure
    val interleave_col_bits = bus_bits / interleave_factor // Bits per line per cycle
    val interleave_cols = line_bits / interleave_col_bits // Cols of packed data structure
    val interleave_line_els = interleave_col_bits / pxl_bits // # of elements of a line per cycle
    // /** DRAM TESTING */
    // // Get input data
    // val input_data = loadCSV2D[I16](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.csv"," ","\n")
    // // Set up dram
    // val COLS = ArgIn[Int]
    // val ROWS = ArgIn[Int]
    // val LINES_TODO = ArgIn[Int]
    // setArg(COLS, input_data.cols)
    // setArg(ROWS, input_data.rows)
    // setArg(LINES_TODO, args(0).to[Int])
    // val input_dram = DRAM[I16](ROWS, COLS)
    // setMem(input_dram, input_data)
    // val output_composite_dram = DRAM[composite](LINES_TODO)

    /** BOARD TESTING */
    val in = StreamIn[U256](CXPPixelBus)
    val LINES_TODO = ArgIn[Int]
    setArg(LINES_TODO, rows)
    val COLS = interleave_cols;
//    setArg(COLS, 1024)
    val out = StreamOut[U256](CXPPixelBus)

    // Create HW accelerator
    Accel {
      Stream.Foreach(*){r => 
        val input_fifo = List.tabulate(interleave_factor){_ => FIFO[I16](colTileSize)}
        val issue = FIFO[Int](2*rowTileSize).conflictable
        val results = List.tabulate(interleave_factor){_ => FIFO[composite](2*rowTileSize/interleave_factor)}
        val packed_results = FIFO[composite](2*rowTileSize)

        // Stage 1: consume cxp-like stream
        Pipe {
          val raw: U256 = in.value
          input_fifo.zipWithIndex.foreach{case (f,i) =>
            val raw_pack: I64 = raw.bits((i+1)*interleave_col_bits - 1 :: i*interleave_col_bits).as[I64] // U${256/interleave_col_bits}
            f.enqVec(raw_pack.asVec[I16])
          }
        }

        // Stage 2: Process (Force II = 1 to squeeze sr write and sr read into one cycle)
        input_fifo.zipWithIndex.foreach{case (f,i) =>
          SpatialHelper.ComputeUnit[T](COLS, interleave_line_els, sharp_kernel, f, issue, results(i), r*interleave_factor + i, rowTileSize, LINES_TODO, i == (input_fifo.size-1))
        }

        // Stage 3: Store
        Pipe{
          val numel = issue.deq()
          if (numel > 0) {
            Foreach(numel / interleave_factor by 1){e =>
              packed_results.enqVec(Vec.ZeroFirst(results.map(_.deq()):_*))
            }
            // // DEBUG
            // deriv store deriv_fifo
            // Store results
            Foreach(numel by 1){e =>
              out := packed_results.deq().as[U256];
            }
          }
        }
      }
    }

  }
}
