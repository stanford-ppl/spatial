package spatial.tests.feature.streaming

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
import forge.tags._

@struct case class score(idx: Int, v: I32)
@struct case class composite(rising_idx: Int, rising_v: I32, falling_idx: Int, falling_v: I32, volume: U16, row: U16, p32: U32, p64: U64)

object SpatialHelper {

  @virtualize
  @api def ComputeUnit[T:Num](
    COLS: I32,
    els_per_pack: scala.Int,
    sharp_kernel: Seq[scala.Double],
    input_fifo: FIFO[I16],
    issue: FIFO[Int],
    result: FIFO[composite],
    r: I32,
    rowTileSize: scala.Int,
    LINES_TODO: I32,
    last_unit: scala.Boolean
  ): Unit = {
    val deriv_window = sharp_kernel.size
    Console.println(s"kernel is ${sharp_kernel.mkString(",")}")
    Pipe.II(1).Foreach(COLS by 1, els_per_pack by 1){(oc, ic) =>
      val c = oc*els_per_pack + ic
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
      if (c == deriv_window.to[Int] || (c > deriv_window.to[Int] && t.to[I32] > best_rising.value.v)) {
        acc_after_rising.reset()
        best_rising := score(c,t.to[I32])
      }
      if (c == deriv_window.to[Int] || (c > deriv_window.to[Int] && t.to[I32] < best_falling.value.v)) {
        acc_after_falling.reset()
        best_falling := score(c,t.to[I32])
      }
      if (c == (COLS*els_per_pack)-1) {
        result.enq(composite(best_rising.value.idx, best_rising.value.v, best_falling.value.idx, best_falling.value.v, acc_after_rising - acc_after_falling, r.as[U16], 0, 0))
        if (last_unit) issue.enq(mux(best_rising.value == score(-1,-1) || r == LINES_TODO-1 || r % rowTileSize == rowTileSize-1, mux((r+1) % rowTileSize == 0, rowTileSize, r % rowTileSize + 1), 0)) // Random math to make sure retiming puts it later
      } 

      // // DEBUG
      // if (r == LINES_TODO-1) deriv_fifo.enq(t)
    }
  }
}

@spatial class FilterStream1D extends SpatialTest {

  override def runtimeArgs: Args = "50"

  type T = FixPt[TRUE, _16, _16]
  val colTileSize = 64
  val rowTileSize = 16
  val deriv_window = 40
  type P = FixPt[TRUE,_112,_0]
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
        SpatialHelper.ComputeUnit[T](COLS, 1, sharp_kernel, input_fifo, issue, result, r, rowTileSize, LINES_TODO, true)

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
    println("|  Row           |  Rising Idx   |  Falling Idx  |     Volume      |   Rising V   |   Falling V   |")
    for (i <- 0 until LINES_TODO) {
      println(r"|      ${result_composite_dram(i).row}         |" +
              r"     ${result_composite_dram(i).rising_idx}      |" +
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



@spatial class InterleavedFilterStream1D extends SpatialTest {

  /*    ___________________________________________________________________________________
       |___64 bits line 0___|___64 bits line 1___|___64 bits line 2___|___64 bits line 3___|
        <------------------------  one cycle ---------------------------------------------->
  */

  override def runtimeArgs: Args = "48"

  type T = FixPt[TRUE, _16, _16]
  val colTileSize = 64
  val rowTileSize = 16
  val deriv_window = 40
  val interleave_factor = 2 // if = 4 doesn't work because cpp backend doesn't handle 256 bit numbers well...
  type PACK = I128 // Should equal interleave_factor * 64
  val bus_bits = interleave_factor * 64
  val line_bits = 1024 * 16
  val pxl_bits = 16
  def main(args: Array[String]): Unit = {

    // // Get reference edges
    // val ref_file = "../data_fs/reference/chirp-2000_interferedelay1650_photonen9.5_ncalibdelays8192_netalon0_interference.calibration"
    // // Helpers.build_reference(ref_file)
    // Helpers.parse_reference(ref_file)

    // Get hard/soft derivative kernels
    val sharp_kernel = Helpers.build_derivkernel(deriv_window/8, deriv_window)
    println(r"""Kernel: ${sharp_kernel.mkString("\t")}""")

    // Get input data
    val raw_input_data: Matrix[I16] = loadCSV2D[I16](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.csv"," ","\n")
    assert(args(0).to[Int] % interleave_factor == 0, s"LINES_TODO (${args(0).to[Int]} must be divisible by interleave_factor $interleave_factor")
    val interleave_rows = args(0).to[Int]/interleave_factor // Rows of packed data structure
    val interleave_col_bits = bus_bits / interleave_factor // Bits per line per cycle
    val interleave_cols = line_bits / interleave_col_bits // Cols of packed data structure
    val interleave_line_els = interleave_col_bits / pxl_bits // # of elements of a line per cycle
    val input_data: Matrix[PACK] = (0::interleave_rows, 0::interleave_cols){(i,j) =>
      val pack = Seq.tabulate(interleave_factor){p =>
        val row = i * interleave_factor + p
        val col = j * interleave_line_els
        val els = Seq.tabulate(interleave_line_els){e => raw_input_data(row,col + e)}
        Seq.tabulate(interleave_line_els){e => println(r" row $row, col $col, el $e = ${raw_input_data(row,col + e)}")}
        els
      }.flatten
      Vec.ZeroFirst(pack:_*).asPacked[PACK]
    }

    println(r"Packed ${args(0).to[Int]} x ${raw_input_data.cols} matrix -> $interleave_rows x $interleave_cols matrix with interleave_factor $interleave_factor")
    // Set up dram
    val COLS = ArgIn[Int]
    val ROWS = ArgIn[Int]
    val LINES_TODO = ArgIn[Int]
    setArg(COLS, input_data.cols)
    setArg(ROWS, input_data.rows)
    setArg(LINES_TODO, args(0).to[Int])
    val input_dram = DRAM[PACK](ROWS, COLS)
    setMem(input_dram, input_data)
    val output_composite_dram = DRAM[composite](LINES_TODO)


    // DEBUG
    val deriv = DRAM[T](COLS)

    Accel{
      val done = Reg[Bit](false)
      Stream(breakWhen = done).Foreach(*){r =>
        val raw_input_fifo = FIFO[PACK](colTileSize)
        val input_fifo = List.tabulate(interleave_factor){_ => FIFO[I16](colTileSize)}
        val issue = FIFO[Int](2*rowTileSize).conflictable
        val results = List.tabulate(interleave_factor){_ => FIFO[composite](2*rowTileSize/interleave_factor)}
        val packed_results = FIFO[composite](2*rowTileSize)

        // // DEBUG
        // val deriv_fifo = FIFO[T](32)

        // Stage 0: Load
        Pipe{
          if (r < ROWS) raw_input_fifo load input_dram(r, 0::COLS)
        }

        // Stage 1: consume cxp-like stream
        Pipe {
          val raw: PACK = raw_input_fifo.deq()
          input_fifo.zipWithIndex.foreach{case (f,i) =>
            val raw_pack = raw.bits((i+1)*interleave_col_bits - 1 :: i*interleave_col_bits).as[I64] // U${128/interleave_col_bits}
            f.enqVec(raw_pack.asVec[I16])
          }
        }

        // Stage 2: Process (Force II = 1 to squeeze sr write and sr read into one cycle)
        input_fifo.zipWithIndex.foreach{case (f,i) =>
          Pipe{SpatialHelper.ComputeUnit[T](COLS, interleave_line_els, sharp_kernel, f, issue, results(i), r*interleave_factor + i, rowTileSize, LINES_TODO, i == (input_fifo.size-1))}
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
            output_composite_dram((r*interleave_factor)-((r*interleave_factor)%rowTileSize)::(r*interleave_factor)-((r*interleave_factor)%rowTileSize) + numel) store packed_results
            if (r*interleave_factor >= (LINES_TODO - interleave_factor)) {
//              FSM(0)(state => state != 1) { _ => }(state => mux(state == 0 && input_fifo.forall(_.isEmpty), 1, 0))
              done := true
            }
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
    println("|  Row           |  Rising Idx   |  Falling Idx  |     Volume      |   Rising V   |   Falling V   |")
    for (i <- 0 until LINES_TODO) {
      println(r"|      ${result_composite_dram(i).row}         |" +
              r"     ${result_composite_dram(i).rising_idx}      |" +
              r"      ${result_composite_dram(i).falling_idx}      |" +
              r"      ${result_composite_dram(i).volume}      |"      +
              r"      ${result_composite_dram(i).rising_v}      |"    +
              r"      ${result_composite_dram(i).falling_v}      |")
    }

    val gold_rising_idx_all = loadCSV1D[Int](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.rising_idx",",")
    val gold_rising_v_all = loadCSV1D[I32](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.rising_v",",")
    val gold_falling_idx_all = loadCSV1D[Int](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.falling_idx",",")
    val gold_falling_v_all = loadCSV1D[I32](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.falling_v",",")
    val gold_volume_all = loadCSV1D[U16](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.volume",",")

    if (LINES_TODO <= 48) { // Only have regression for 50 lines...
      val got_rising_idx = Array.tabulate(LINES_TODO){i => result_composite_dram(i).rising_idx}
      val got_rising_v = Array.tabulate(LINES_TODO){i => result_composite_dram(i).rising_v}
      val got_falling_idx = Array.tabulate(LINES_TODO){i => result_composite_dram(i).falling_idx}
      val got_falling_v = Array.tabulate(LINES_TODO){i => result_composite_dram(i).falling_v}
      val got_volume = Array.tabulate(LINES_TODO){i => result_composite_dram(i).volume}

      val gold_rising_idx = Array.tabulate(LINES_TODO){i => gold_rising_idx_all(i)}
      val gold_rising_v = Array.tabulate(LINES_TODO){i => gold_rising_v_all(i)}
      val gold_falling_idx = Array.tabulate(LINES_TODO){i => gold_falling_idx_all(i)}
      val gold_falling_v = Array.tabulate(LINES_TODO){i => gold_falling_v_all(i)}
      val gold_volume = Array.tabulate(LINES_TODO){i => gold_volume_all(i)}

      def withinPercent[X:Num](a: Array[X], b: Array[X], percent: scala.Double): Bit = {
        Array.tabulate(a.length){i =>
          val pe = 100.0 * abs(a(i).to[Float] - b(i).to[Float])/b(i).to[Float]
          if (pe > percent.to[Float]) println(r"el $i pe = $pe")
          pe < percent.to[Float]
        }.reduce{_&&_}
      }

      println(r"Correct rising_idx:  ${withinPercent(gold_rising_idx, got_rising_idx, 5.1)}")
      println(r"Correct rising_v:    ${withinPercent(gold_rising_v, got_rising_v, 5.1)}")
      println(r"Correct falling_idx: ${withinPercent(gold_falling_idx, got_falling_idx, 5.1)}")
      println(r"Correct falling_v:   ${withinPercent(gold_falling_v, got_falling_v, 5.1)}")
      println(r"Correct volume:      ${withinPercent(gold_volume, got_volume, 5.1)}")

      assert(withinPercent(gold_rising_idx, got_rising_idx, 5.1))
      assert(withinPercent(gold_rising_v, got_rising_v, 5.1))
      assert(withinPercent(gold_falling_idx, got_falling_idx, 5.1))
      assert(withinPercent(gold_falling_v, got_falling_v, 5.1))
      assert(withinPercent(gold_volume, got_volume, 5.1))
    }

  }
}



@spatial class InterleavedFilterStream1DBBox extends SpatialTest {

  /*    ___________________________________________________________________________________
       |___64 bits line 0___|___64 bits line 1___|___64 bits line 2___|___64 bits line 3___|
        <------------------------  one cycle ---------------------------------------------->
  */

  override def runtimeArgs: Args = "48"

  type T = FixPt[TRUE, _16, _16]
  val colTileSize = 64
  val rowTileSize = 16
  val deriv_window = 40
  val interleave_factor = 2 // if = 4 doesn't work because cpp backend doesn't handle 256 bit numbers well...
  type PACK = I128 // Should equal interleave_factor * 64
  val bus_bits = interleave_factor * 64
  val line_bits = 1024 * 16
  val pxl_bits = 16

  @streamstruct case class BBOX_IN(COLS: I32, input: I16, r: I32, LINES_TODO: I32, last_unit: Bit)
  @streamstruct case class BBOX_OUT(result: composite, issue: Int)

  def main(args: Array[String]): Unit = {

    // // Get reference edges
    // val ref_file = "../data_fs/reference/chirp-2000_interferedelay1650_photonen9.5_ncalibdelays8192_netalon0_interference.calibration"
    // // Helpers.build_reference(ref_file)
    // Helpers.parse_reference(ref_file)

    // Get hard/soft derivative kernels
    val sharp_kernel = Helpers.build_derivkernel(deriv_window/8, deriv_window)
    println(r"""Kernel: ${sharp_kernel.mkString("\t")}""")

    // Get input data
    val raw_input_data: Matrix[I16] = loadCSV2D[I16](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.csv"," ","\n")
    assert(args(0).to[Int] % interleave_factor == 0, s"LINES_TODO (${args(0).to[Int]} must be divisible by interleave_factor $interleave_factor")
    val interleave_rows = args(0).to[Int]/interleave_factor // Rows of packed data structure
    val interleave_col_bits = bus_bits / interleave_factor // Bits per line per cycle
    val interleave_cols = line_bits / interleave_col_bits // Cols of packed data structure
    val interleave_line_els = interleave_col_bits / pxl_bits // # of elements of a line per cycle
    val input_data: Matrix[PACK] = (0::interleave_rows, 0::interleave_cols){(i,j) =>
      val pack = Seq.tabulate(interleave_factor){p =>
        val row = i * interleave_factor + p
        val col = j * interleave_line_els
        val els = Seq.tabulate(interleave_line_els){e => raw_input_data(row,col + e)}
        els
      }.flatten
      Vec.ZeroFirst(pack:_*).asPacked[PACK]
    }

    println(r"Packed ${args(0).to[Int]} x ${raw_input_data.cols} matrix -> $interleave_rows x $interleave_cols matrix with interleave_factor $interleave_factor")
    // Set up dram
    val COLS = ArgIn[Int]
    val ROWS = ArgIn[Int]
    val LINES_TODO = ArgIn[Int]
    setArg(COLS, input_data.cols)
    setArg(ROWS, input_data.rows)
    setArg(LINES_TODO, args(0).to[Int])
    val input_dram = DRAM[PACK](ROWS, COLS)
    setMem(input_dram, input_data)
    val output_composite_dram = DRAM[composite](LINES_TODO)


    // Define bbox outside of Accel
    val computeUnitImpl = Blackbox.SpatialController[BBOX_IN, BBOX_OUT] { in: BBOX_IN =>
      val deriv_window = sharp_kernel.size
      val COLS = in.COLS
      val r = in.r
      val LINES_TODO = in.LINES_TODO
      val last_unit = in.last_unit
      val issue = FIFO[Int](2*rowTileSize/interleave_factor).conflictable
      val result = FIFO[composite](2*rowTileSize/interleave_factor)

      Pipe.II(1).Foreach(COLS by 1, interleave_line_els by 1){(oc, ic) =>
        val c = oc*interleave_line_els + ic
        val best_rising = Reg[score](score(0, -999.to[I32]))
        val best_falling = Reg[score](score(0, -999.to[I32]))
        val acc_after_rising = Reg[U16](0)
        val acc_after_falling = Reg[U16](0)
        val sr = RegFile[I16](deriv_window)
        val next = in.input
        sr <<= next
        acc_after_rising :+= next.as[U16]
        acc_after_falling :+= next.as[U16]
        val t = List.tabulate(deriv_window){i => sharp_kernel(i).to[T] * sr(i).to[T]}.reduceTree{_+_}
        if (c == deriv_window.to[Int] || (c > deriv_window.to[Int] && t.to[I32] > best_rising.value.v)) {
          acc_after_rising.reset()
          best_rising := score(c,t.to[I32])
        }
        if (c == deriv_window.to[Int] || (c > deriv_window.to[Int] && t.to[I32] < best_falling.value.v)) {
          acc_after_falling.reset()
          best_falling := score(c,t.to[I32])
        }
        if (c == (COLS*interleave_line_els)-1) {
          result.enq(composite(best_rising.value.idx, best_rising.value.v, best_falling.value.idx, best_falling.value.v, acc_after_rising - acc_after_falling, r.as[U16], 0, 0))
          if (last_unit) issue.enq(mux(best_rising.value == score(-1,-1) || r == LINES_TODO-1 || r % rowTileSize == rowTileSize-1, mux((r+1) % rowTileSize == 0, rowTileSize, r % rowTileSize + 1), 0)) // Random math to make sure retiming puts it later
        }

        // // DEBUG
        // if (r == LINES_TODO-1) deriv_fifo.enq(t)
      }
      BBOX_OUT(result.deqInterface(), issue.deqInterface())
    }


    // DEBUG
    val deriv = DRAM[T](COLS)

    Accel{
      val done = Reg[Bit](false)
      Stream(breakWhen = done).Foreach(*){r =>
        val raw_input_fifo = FIFO[PACK](colTileSize)
        val input_fifo = List.tabulate(interleave_factor){_ => FIFO[I16](colTileSize)}
        val COLS_FIFO = List.tabulate(interleave_factor){_ => FIFO[Int](rowTileSize)}
        val r_FIFO = List.tabulate(interleave_factor){_ => FIFO[Int](rowTileSize)}
        val LINES_TODO_FIFO = List.tabulate(interleave_factor){_ => FIFO[Int](rowTileSize)}
        val LAST_FIFO = List.tabulate(interleave_factor){_ => FIFO[Bit](rowTileSize)}
        val packed_results = FIFO[composite](2*rowTileSize)

        // // DEBUG
        // val deriv_fifo = FIFO[T](32)

        // Stage 0: Load
        Pipe{
          if (r < ROWS) {
            Parallel {
              List.tabulate(interleave_factor) { i =>
                COLS_FIFO(i).enq(COLS)
                r_FIFO(i).enq(r * interleave_factor + i)
                LINES_TODO_FIFO(i).enq(LINES_TODO)
                LAST_FIFO(i).enq(i == (input_fifo.size - 1))
              }
              raw_input_fifo load input_dram(r, 0 :: COLS)
            }
          }
        }

        // Stage 1: consume cxp-like stream
          // BBoxes automatically turn off stream ctr optimization, but we want this particular loop to run as fast as possible without bubbles so we make it a Foreach(*) instead of a Pipe
        Foreach(*) { _ =>
//        Pipe{
          val raw: PACK = raw_input_fifo.deq()
          input_fifo.zipWithIndex.foreach{case (f,i) =>
            val raw_pack = raw.bits((i+1)*interleave_col_bits - 1 :: i*interleave_col_bits).as[I64] // U${128/interleave_col_bits}
            f.enqVec(raw_pack.asVec[I16])
          }
        }

        // Stage 2: Process (Force II = 1 to squeeze sr write and sr read into one cycle)
        val bbox = input_fifo.zipWithIndex.map{case (f,i) =>
          computeUnitImpl(BBOX_IN(COLS_FIFO(i).deqInterface(), f.deqInterface(), r_FIFO(i).deqInterface(), LINES_TODO_FIFO(i).deqInterface(), LAST_FIFO(i).deqInterface()))
        }

        // Stage 3: Store
        Pipe{
          val numel = bbox.last.issue
          if (numel > 0) {
            Foreach(numel / interleave_factor by 1){e =>
              packed_results.enqVec(Vec.ZeroFirst(bbox.map(_.result):_*))
            }
            // // DEBUG
            // deriv store deriv_fifo
            // Store results
            output_composite_dram((r*interleave_factor)-((r*interleave_factor)%rowTileSize)::(r*interleave_factor)-((r*interleave_factor)%rowTileSize) + numel) store packed_results
            if (r*interleave_factor >= (LINES_TODO - interleave_factor)) {
//              FSM(0)(state => state != 1) { _ => }(state => mux(state == 0 && input_fifo.forall(_.isEmpty), 1, 0))
              done := true
            }
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
    println("|  Row           |  Rising Idx   |  Falling Idx  |     Volume      |   Rising V   |   Falling V   |")
    for (i <- 0 until LINES_TODO) {
      println(r"|      ${result_composite_dram(i).row}         |" +
              r"     ${result_composite_dram(i).rising_idx}      |" +
              r"      ${result_composite_dram(i).falling_idx}      |" +
              r"      ${result_composite_dram(i).volume}      |"      +
              r"      ${result_composite_dram(i).rising_v}      |"    +
              r"      ${result_composite_dram(i).falling_v}      |")
    }

    val gold_rising_idx_all = loadCSV1D[Int](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.rising_idx",",")
    val gold_rising_v_all = loadCSV1D[I32](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.rising_v",",")
    val gold_falling_idx_all = loadCSV1D[Int](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.falling_idx",",")
    val gold_falling_v_all = loadCSV1D[I32](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.falling_v",",")
    val gold_volume_all = loadCSV1D[U16](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.volume",",")

    if (LINES_TODO <= 48) { // Only have regression for 50 lines...
      val got_rising_idx = Array.tabulate(LINES_TODO){i => result_composite_dram(i).rising_idx}
      val got_rising_v = Array.tabulate(LINES_TODO){i => result_composite_dram(i).rising_v}
      val got_falling_idx = Array.tabulate(LINES_TODO){i => result_composite_dram(i).falling_idx}
      val got_falling_v = Array.tabulate(LINES_TODO){i => result_composite_dram(i).falling_v}
      val got_volume = Array.tabulate(LINES_TODO){i => result_composite_dram(i).volume}

      val gold_rising_idx = Array.tabulate(LINES_TODO){i => gold_rising_idx_all(i)}
      val gold_rising_v = Array.tabulate(LINES_TODO){i => gold_rising_v_all(i)}
      val gold_falling_idx = Array.tabulate(LINES_TODO){i => gold_falling_idx_all(i)}
      val gold_falling_v = Array.tabulate(LINES_TODO){i => gold_falling_v_all(i)}
      val gold_volume = Array.tabulate(LINES_TODO){i => gold_volume_all(i)}

      def withinPercent[X:Num](a: Array[X], b: Array[X], percent: scala.Double): Bit = {
        Array.tabulate(a.length){i =>
          val pe = 100.0 * abs(a(i).to[Float] - b(i).to[Float])/b(i).to[Float]
          if (pe > percent.to[Float]) println(r"el $i pe = $pe")
          pe < percent.to[Float]
        }.reduce{_&&_}
      }

      println(r"Correct rising_idx:  ${withinPercent(gold_rising_idx, got_rising_idx, 5.1)}")
      println(r"Correct rising_v:    ${withinPercent(gold_rising_v, got_rising_v, 5.1)}")
      println(r"Correct falling_idx: ${withinPercent(gold_falling_idx, got_falling_idx, 5.1)}")
      println(r"Correct falling_v:   ${withinPercent(gold_falling_v, got_falling_v, 5.1)}")
      println(r"Correct volume:      ${withinPercent(gold_volume, got_volume, 5.1)}")

      assert(withinPercent(gold_rising_idx, got_rising_idx, 5.1))
      assert(withinPercent(gold_rising_v, got_rising_v, 5.1))
      assert(withinPercent(gold_falling_idx, got_falling_idx, 5.1))
      assert(withinPercent(gold_falling_v, got_falling_v, 5.1))
      assert(withinPercent(gold_volume, got_volume, 5.1))
    }

  }
}


