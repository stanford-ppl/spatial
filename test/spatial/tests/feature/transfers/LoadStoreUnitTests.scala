package spatial.tests.feature.transfers


import argon.static.Sym
import spatial.dsl._

@spatial class FrameLoadTest extends SpatialTest {
  def main(args: Array[String]): Unit = {

    val in = FrameIn[U64](8)
    val data = Array[U64](Seq.tabulate(8){i => List.tabulate(4){j => ((i*4+j)%256) << (j*8)}.sum}.map(_.to[U64]):_*)

    setFrame(in,data)
    val out = List.tabulate(32){_ => ArgOut[U8]}
    Accel {
      val a = SRAM[U64](8)
      a load in
      out.grouped(4).zipWithIndex.foreach { case (os, i) =>
        val all = a(i).asVec[U8]
        os.zipWithIndex.foreach { case (o, j) => o := all(j) }
      }
    }
    out.zipWithIndex.foreach{case (o,i) =>
      println(r"got ${getArg(o)}, wanted ${i}")
      assert(getArg(o) == i)
    }
  }
}



@spatial class FrameStoreTest extends SpatialTest {
  def main(args: Array[String]): Unit = {

    val out = FrameOut[U64](8)
    Accel {
      val a = SRAM[U64](8)
      Foreach(32 by 4) {i => a(i/4) = Vec.ZeroFirst(i.to[U8], (i+1).to[U8], (i+2).to[U8], (i+3).to[U8], 0.to[U8], 0.to[U8], 0.to[U8], 0.to[U8]).asPacked[U64]}
      out store a
    }
    val got = getFrame(out)
    val gold = Array[U64](Seq.tabulate(8){i => List.tabulate(4){j => ((i*4+j)%256) << (j*8)}.sum}.map(_.to[U64]):_*)
    printArray(gold, "Wanted:")
    printArray(got, "Got:")
    assert(got == gold)
  }
}


@spatial class FrameLoadStoreTest extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val in = FrameIn[U32](64)
    val data = Array.tabulate[U32](64){i => i.to[U32]}
    setFrame(in, data)
    val out = FrameOut[U32](64)
    Accel {
      Stream {
        val a = FIFO[U32](8)
        a load in
        val b = FIFO[U32](depth=8)
        Foreach(64 by 1){_ => b.enq(a.deq() + 5)}
        out store b
      }
//      Stream.Foreach(64 by 1) { _ =>
//        import spatial.lang._
//        import spatial.metadata.memory._
//        val a = FIFO[U32](8)
//        val strmIn = in.asInstanceOf[Sym[_]].interfaceStream
//        a.enq(strmIn.value)
//        val b = FIFO[U32](depth=8)
//        b.enq(a.deq() + 5)
//        val strmOut = out.asInstanceOf[Sym[_]].interfaceStream
//        strmOut := b.deq()
//      }
    }
    val got = getFrame(out)
    val gold = data.map(_+5)
    printArray(gold, "Wanted:")
    printArray(got, "Got:")
    assert(got == gold)
  }
}

//
//@spatial class FrameFilter1D extends SpatialTest {
//
//  override def runtimeArgs: Args = "50"
//
//  type T = FixPt[TRUE, _16, _16]
//  val colTileSize = 64
//  val rowTileSize = 16
//  val deriv_window = 40
//  type P = FixPt[TRUE,_112,_0]
//  def main(args: Array[String]): Unit = {
//    import spatial.tests.feature.streaming.{Helpers, composite, SpatialHelper}
//    // // Get reference edges
//    // val ref_file = "../data_fs/reference/chirp-2000_interferedelay1650_photonen9.5_ncalibdelays8192_netalon0_interference.calibration"
//    // // Helpers.build_reference(ref_file)
//    // Helpers.parse_reference(ref_file)
//
//    // Get hard/soft derivative kernels
//    val sharp_kernel = Helpers.build_derivkernel(deriv_window/8, deriv_window)
//    println(r"""Kernel: ${sharp_kernel.mkString("\t")}""")
//
//    // Get input data
//    val input_data = loadCSV2D[I16](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.csv"," ","\n")
//
//    // Set up dram
//    val COLS = ArgIn[Int]
//    val ROWS = ArgIn[Int]
//    val LINES_TODO = ArgIn[Int]
//    setArg(COLS, input_data.cols)
//    setArg(ROWS, input_data.rows)
//    setArg(LINES_TODO, args(0).to[Int])
//    val input_dram = FrameIn[I16](ROWS * COLS)
//    setFrame(input_dram, input_data.flatten)
//    val output_composite_dram = FrameOut[composite](LINES_TODO)
//
////    // DEBUG
////    val deriv = DRAM[T](COLS)
//
//    Accel{
//      /*
//                                                acc after last rising update
//                                                             _
//                                           /------------>   |_|  ------------\
//                                          /      acc after last rising update ---\
//                                         /                   _               /    \
//                                        /--------------->   |_|  -----------/      \
//                                       /                                            \
//                       input_fifo     /     sr                                       \
//                        ________     /     _____                min idx/val           \  result_fifo
//       DRAM  ----->    |_|_|_|_|    --->  |_|_|_|     deriv       _                    \   ________
//                                                  \     _     /  |_|  ------------------> |_|_|_|_|   ----->    DRAM
//                                          kernel   >   |_|  <   max idx/val            /
//                                           _____  /           \   _                   /
//                                          |_|_|_|                |_|  ---------------/
//
//       \__________________/ \________________________________________________________________/ \____________________/
//             Stage 1                            Stage 2                                                Stage 3
//
//
//
//
//
//      */
//      Stream.Foreach(LINES_TODO by 1 par 1){r =>
//        val input_fifo = FIFO[I16](colTileSize)
//        val issue = FIFO[Int](2*rowTileSize)
//        val result = FIFO[composite](2*rowTileSize)
//
//        // // DEBUG
//        // val deriv_fifo = FIFO[T](32)
//
//        // Stage 1: Load
//        input_fifo load input_dram
//
//        // Stage 2: Process (Force II = 1 to squeeze sr write and sr read into one cycle)
//        SpatialHelper.ComputeUnit[T](COLS, 1, sharp_kernel, input_fifo, issue, result, r, rowTileSize, LINES_TODO, true)
//
//        // Stage 3: Store
//        output_composite_dram store result
//      }
//    }
//
//    // // DEBUG
//    // println("Debug info:")
//    // printArray(Array.tabulate(input_data.cols){i => input_data(args(0).to[Int]-1, i)}, r"Row ${args(0)}")
//    // printArray(getMem(deriv), r"Deriv ${args(0)}")
//
//    val result_composite_dram = getFrame(output_composite_dram)
//    println("Results:")
//    println("|  Row           |  Rising Idx   |  Falling Idx  |     Volume      |   Rising V   |   Falling V   |")
//    for (i <- 0 until LINES_TODO) {
//      println(r"|      ${result_composite_dram(i).row}         |" +
//              r"     ${result_composite_dram(i).rising_idx}      |" +
//              r"      ${result_composite_dram(i).falling_idx}      |" +
//              r"      ${result_composite_dram(i).volume}      |"      +
//              r"      ${result_composite_dram(i).rising_v}      |"    +
//              r"      ${result_composite_dram(i).falling_v}      |")
//    }
//
//    val gold_rising_idx = loadCSV1D[Int](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.rising_idx",",")
//    val gold_rising_v = loadCSV1D[I32](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.rising_v",",")
//    val gold_falling_idx = loadCSV1D[Int](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.falling_idx",",")
//    val gold_falling_v = loadCSV1D[I32](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.falling_v",",")
//    val gold_volume = loadCSV1D[U16](s"$DATA/slac/xppc00117_r136_refsub_ipm4_del3.volume",",")
//
//    if (LINES_TODO == 50) { // Only have regression for 50 lines...
//      val got_rising_idx = Array.tabulate(LINES_TODO){i => result_composite_dram(i).rising_idx}
//      val got_rising_v = Array.tabulate(LINES_TODO){i => result_composite_dram(i).rising_v}
//      val got_falling_idx = Array.tabulate(LINES_TODO){i => result_composite_dram(i).falling_idx}
//      val got_falling_v = Array.tabulate(LINES_TODO){i => result_composite_dram(i).falling_v}
//      val got_volume = Array.tabulate(LINES_TODO){i => result_composite_dram(i).volume}
//
//      println(r"Correct rising_idx:  ${gold_rising_idx == got_rising_idx}")
//      println(r"Correct rising_v:    ${gold_rising_v == got_rising_v}")
//      println(r"Correct falling_idx: ${gold_falling_idx == got_falling_idx}")
//      println(r"Correct falling_v:   ${gold_falling_v == got_falling_v}")
//      println(r"Correct volume:      ${gold_volume == got_volume}")
//
//      assert(gold_rising_idx == got_rising_idx)
//      assert(gold_rising_v == got_rising_v)
//      assert(gold_falling_idx == got_falling_idx)
//      assert(gold_falling_v == got_falling_v)
//      assert(gold_volume == got_volume)
//    }
//
//  }
//}