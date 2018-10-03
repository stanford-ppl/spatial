package spatial.tests.apps

import spatial.dsl._
@spatial class convprobe extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED
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

    // debug
    val INPUT_CHUNK = DRAM[T](3,3,INCHANS_MAX_UPPERBOUND)
    val KERNEL_CHUNK = DRAM[T](3,3,INCHANS_MAX_UPPERBOUND)
    val OUTCHAN_PROBE = ArgIn[Int]
    val OUTROW_PROBE = ArgIn[Int]
    val OUTCOL_PROBE = ArgIn[Int]
    val OUTVAL = ArgOut[T]
    setArg(OUTROW_PROBE, args(8).to[Int])
    setArg(OUTCOL_PROBE, args(9).to[Int])
    setArg(OUTCHAN_PROBE, args(10).to[Int])

    // Load data (placeholder)
    val input = (0::IMAX_0, 0::IMAX_1, 0::INCHANS_MAX) {(i,j,k) => ((i + j + k) % 64 - 8).to[T]}
    val output = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 0.to[T]}
    val kernel = (0::OUTCHANS_MAX, 0::KERNEL_COLS, 0::KERNEL_ROWS, 0::INCHANS_MAX) {(i,j,k,l) => if (random[Int](10) > 7) 1.to[T] else 0.to[T]}
    val bias =   Array.tabulate(OUTCHANS_MAX){i => 1.to[T]}

    // printTensor3(input.reorder(Seq(2,1,0)), "Input")
    // printTensor4(kernel.reorder(Seq(0,3,2,1)), "Kernel")
    // printArray(bias, "Bias")

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
          Foreach(OUTCHANS_MAX by 1 par P6){oc => 
            val bitshifted = if (accum_line_upcast(oc) < (-134217728).to[T2]) -32768.to[T]
                             else if (accum_line_upcast(oc) > (134217727).to[T2]) 32767.to[T]
                             // else accum_line_upcast(oc).bits(27::12).as[T]
                             else accum_line_upcast(oc).bits(15::0).as[T]
            println(r"  bitshifted @ outchan $oc = max(0, ${bitshifted +! bias_sram(oc)}")
            accum_line(oc) = max(0.to[T], bitshifted +! bias_sram(oc))
            if (C == OUTCOL_PROBE.value && R == OUTROW_PROBE.value && oc == OUTCHAN_PROBE.value) {
              INPUT_CHUNK store local_data
              val kernel_debug = SRAM[T](3,3,INCHANS_MAX_UPPERBOUND)
              Foreach(3 by 1, 3 by 1, INCHANS_MAX_UPPERBOUND by 1){(i,j,k) => kernel_debug(i,j,k) = kernel_sram(oc,i,j,k)}
              KERNEL_CHUNK(0::3, 0::3, 0::INCHANS_MAX) store kernel_debug
              OUTVAL := bitshifted +! bias_sram(oc)
            }
          }
          OUTPUT_DATA(C,R,0::OUTCHANS_MAX par storePar) store accum_line
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    println("Results are " + results.dim0 + " x " + results.dim1 + " x " + results.dim2)

    // // Compute Checks
    // val gold = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) => 
    //   val el = Array.tabulate(INCHANS_MAX){page => 
    //     Array.tabulate(KERNEL_COLS){ii => Array.tabulate(KERNEL_ROWS){jj => 
    //       val idx0 = i*stride - 1 + ii
    //       val idx1 = j*stride - 1 + jj

    //       val pxl = if (idx0 >= 0 && idx0 < IMAX_0 && idx1 >= 0 && idx1 < IMAX_1) input(idx0,idx1, page) else 0.to[T]
    //       val f = kernel(k, ii, jj, page)
    //       // println(r"for $idx0 $idx1: $pxl * $f")
    //       pxl.to[T2] * f.to[T2]
    //     }}.flatten.reduce{_+_}
    //   }.reduce{_+_} //>> 12
    //   if (el.to[T] + bias(k) < 0.to[T]) 0.to[T] else {el.to[T] + bias(k)}
    // }
  
    if ((print_data == 1)) {
      printTensor3(input.reorder(Seq(2,1,0)), "Input")
      printTensor4(kernel.reorder(Seq(0,3,2,1)), "Kernel")
      // printTensor3(gold.reorder(Seq(2,1,0)), "Gold")
      printTensor3(results.reorder(Seq(2,1,0)), "Extracted")  

    //   val bitmask = (0::OMAX_0, 0::OMAX_1, 0::OUTCHANS_MAX){(i,j,k) =>
    //     if (results(i,j,k) == gold(i,j,k)) 1.to[Int] else 0.to[Int]
    //   }
    //   val num_wrong = bitmask.length - bitmask.reduce{_+_}
    //   printTensor3(bitmask.reorder(Seq(2,1,0)), "Matchup")
    //   println("Error rate: " + num_wrong + " / " + bitmask.length + " incorrect")

    }
    printTensor3(getTensor3(INPUT_CHUNK), "inchunk")
    printTensor3(getTensor3(KERNEL_CHUNK), "kernelchunk")
    println(r"value is ${getArg(OUTVAL)}")

    // val cksum = gold.zip(results){_==_}.reduce{_&&_}    

    // println("PASS: " + cksum + " (SingleLayerConv_RCIO)")
    // assert(cksum)

  }
}
