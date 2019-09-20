import spatial.dsl._


@spatial class FFT_1 extends SpatialTest { /*                                                                                                  
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

    setMem(work_x_dram, data_x)
    setMem(work_y_dram, data_y)

    val signals = ArgIn[Int]
    setArg(signals, args(0).to[Int])

    Accel{
      val work_x_sram = SRAM[T](8,stride).effort(0).buffer
      val work_y_sram = SRAM[T](8,stride).effort(0).buffer
      val smem = SRAM[T](8*8*9).effort(0).buffer

      Sequential.Foreach(signals by 1){_ =>
        work_x_sram load work_x_dram
        work_y_sram load work_y_dram

        val reversed_LUT = LUT[Int](8)(0,4,2,6,1,5,3,7)

        def twiddles8(tid: I32, i: Int, N: Int): Unit = {
          Sequential.Foreach(1 until 8 by 1) { j => 
            val phi = -TWOPI*(i.to[T]*reversed_LUT(j).to[T] / N.to[T])
            val phi_shifted = phi + TWOPI/2
            val beyond_left = phi_shifted < -TWOPI.to[T]/4
            val beyond_right = phi_shifted > TWOPI.to[T]/4
            val phi_bounded = mux(beyond_left, phi_shifted + TWOPI.to[T]/2, mux(beyond_right, phi_shifted - TWOPI.to[T]/2, phi_shifted))
            val phi_x = cos_taylor(phi_bounded) * mux(beyond_left || beyond_right, 1.to[T], -1.to[T]) // cos(real phi)
            val phi_y = sin_taylor(phi_bounded) * mux(beyond_left || beyond_right, 1.to[T], -1.to[T]) // sin(real phi)
            val temp_x = work_x_sram(j, tid)
            Pipe{work_x_sram(j, tid) = temp_x * phi_x - work_y_sram(j, tid) * phi_y}
            Pipe{work_y_sram(j, tid) = temp_x * phi_y + work_y_sram(j, tid) * phi_x}
          }
        }

        def FFT2(tid: I32, id0: Int, id1: Int):Unit = {
          val temp_x = work_x_sram(id0, tid)
          val temp_y = work_y_sram(id0, tid)
          Pipe{work_x_sram(id0, tid) = temp_x + work_x_sram(id1, tid)}
          Pipe{work_y_sram(id0, tid) = temp_y + work_y_sram(id1, tid)}
          Pipe{work_x_sram(id1, tid) = temp_x - work_x_sram(id1, tid)}
          Pipe{work_y_sram(id1, tid) = temp_y - work_y_sram(id1, tid)}
        }

        def FFT4(tid: I32, base: Int):Unit = {
          val exp_LUT = LUT[T](2)(0, -1)
          Sequential.Foreach(0 until 2 by 1) { j => 
            Pipe{FFT2(tid, base+j.to[I32], 2+base+j.to[I32])}
          }
          val temp_x = work_x_sram(base+3,tid)
          Pipe{work_x_sram(base+3,tid) = temp_x * exp_LUT(0) - work_y_sram(base+3,tid)*exp_LUT(1)}
          Pipe{work_y_sram(base+3,tid) = temp_x * exp_LUT(1) - work_y_sram(base+3,tid)*exp_LUT(0)}
          Sequential.Foreach(0 until 2 by 1) { j => 
            Pipe{FFT2(tid, base+2*j.to[I32], 1+base+2*j.to[I32])}
          }
        }

        def FFT8(tid: I32):Unit = {
          Sequential.Foreach(0 until 4 by 1) { i => 
            Pipe{FFT2(tid, i, 4+i.to[I32])}
          }
          Sequential.Foreach(0 until 3 by 1) { i => 
            val exp_LUT = LUT[T](2,3)( 1,  0, -1,
                                        -1, -1, -1)
            val temp_x = work_x_sram(5+i.to[I32], tid)
            val mul_factor = mux(i.to[I32] == 1, 1.to[T], M_SQRT1_2)
            Pipe{work_x_sram(5+i.to[I32], tid) = (temp_x * exp_LUT(0,i) - work_y_sram(5+i.to[I32],tid) * exp_LUT(1,i))*mul_factor}
            Pipe{work_y_sram(5+i.to[I32], tid) = (temp_x * exp_LUT(1,i) + work_y_sram(5+i.to[I32],tid) * exp_LUT(0,i))*mul_factor}
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
        Sequential.Foreach(THREADS by 1) { tid => 
          val sx = 66
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = hi*8+lo // * here but >> above????
          Sequential.Foreach(8 by 1) { i => 
            val lhs_factor = shuffle_lhs_LUT(i)
            val rhs_factor = shuffle_rhs_LUT(i)
            smem(lhs_factor*sx + offset) = work_x_sram(rhs_factor, tid)
          }
        }

        // Loop 3
        Sequential.Foreach(THREADS by 1) { tid => 
          val sx = 8
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = lo*66 + hi
          Sequential.Foreach(8 by 1) { i => 
            val lhs_factor = shuffle_lhs_LUT(i)
            val rhs_factor = shuffle_lhs_LUT(i) // [sic]
            work_x_sram(lhs_factor, tid) = smem(rhs_factor*sx+offset)
          }
        }

        // Loop 4
        Sequential.Foreach(THREADS by 1) { tid => 
          val sx = 66
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = hi*8+lo // * here but >> above????
          Sequential.Foreach(8 by 1) { i => 
            val lhs_factor = shuffle_lhs_LUT(i)
            val rhs_factor = shuffle_rhs_LUT(i)
            smem(lhs_factor*sx + offset) = work_y_sram(rhs_factor, tid)
          }
        }

        // Loop 5
        Sequential.Foreach(THREADS by 1) { tid => 
          val sx = 8
          val hi = tid.to[I32] >> 3;
          val lo = tid.to[I32] & 7;
          val offset = lo*66+hi
          Sequential.Foreach(8 by 1) { i => 
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
        Sequential.Foreach(THREADS by 1) { tid => 
          val sx = 72
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = hi*8 + lo
          Sequential.Foreach(8 by 1) { i => 
            val lhs_factor = shuffle_lhs_LUT(i)
            val rhs_factor = shuffle_rhs_LUT(i)
            smem(lhs_factor * sx + offset) = work_x_sram(rhs_factor, tid)
          }
        }

        // Loop 8
        Sequential.Foreach(THREADS by 1) { tid => 
          val sx = 8
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = hi*72 + lo
          Sequential.Foreach(8 by 1) { i => 
            val lhs_factor = shuffle_lhs_LUT(i)
            val rhs_factor = shuffle_lhs_LUT(i) // [sic]
            work_x_sram(lhs_factor, tid) = smem(rhs_factor * sx + offset)
          }
        }

        // Loop 9
        Sequential.Foreach(THREADS by 1) { tid => 
          val sx = 72
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = hi*8 + lo
          Sequential.Foreach(8 by 1) { i => 
            val lhs_factor = shuffle_lhs_LUT(i)
            val rhs_factor = shuffle_rhs_LUT(i)
            smem(lhs_factor * sx + offset) = work_y_sram(rhs_factor, tid)
          }
        }

        // Loop 10
        Sequential.Foreach(THREADS by 1) { tid => 
          val sx = 8
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = hi*72 + lo
          Sequential.Foreach(8 by 1) { i => 
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
            Pipe{tmem_x(reversed_LUT(i)) = work_x_sram(i, tid)}
            Pipe{tmem_y(reversed_LUT(i)) = work_y_sram(i, tid)}
          }
          Foreach(8 by 1) { i => 
            Pipe{work_x_sram(i, tid) = tmem_x(i)}
            Pipe{work_y_sram(i, tid) = tmem_y(i)}
          }
        }

        result_x_dram store work_x_sram
        result_y_dram store work_y_sram
      }
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
    println("X cksum: " + cksumX + ", Y cksum: " + cksumY)
    println("PASS: " + cksum + " (FFT_Transpose)")
    assert(cksum)
  }
}


@spatial class FFT_2 extends SpatialTest { // Some squashing (IGNORE THIS ONE BECAUSE THE LOCATION I SQUASHED IS NOT VERY BENEFICIAL)
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

    setMem(work_x_dram, data_x)
    setMem(work_y_dram, data_y)

    val signals = ArgIn[Int]
    setArg(signals, args(0).to[Int])
    Accel{
      val work_x_sram = SRAM[T](8,stride).effort(0).buffer.conflictable
      val work_y_sram = SRAM[T](8,stride).effort(0).buffer.conflictable
      val smem = SRAM[T](8*8*9).effort(0).buffer.conflictable

      Sequential.Foreach(signals by 1){ _ => 
        work_x_sram load work_x_dram
        work_y_sram load work_y_dram

        val reversed_LUT = LUT[Int](8)(0,4,2,6,1,5,3,7)

        def twiddles8(tid: I32, i: Int, N: Int): Unit = {
          List.tabulate(7) { jj =>
            val j = jj + 1 
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
          ()
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
    println("X cksum: " + cksumX + ", Y cksum: " + cksumY)
    println("PASS: " + cksum + " (FFT_Transpose)")
    assert(cksum)
  }
}

@spatial class FFT_3 extends SpatialTest { // even more squashing
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

    setMem(work_x_dram, data_x)
    setMem(work_y_dram, data_y)

    val signals = ArgIn[Int]
    setArg(signals, args(0).to[Int])
    Accel{
      val work_x_sram = SRAM[T](8,stride).effort(0).buffer.conflictable
      val work_y_sram = SRAM[T](8,stride).effort(0).buffer.conflictable
      val smem = SRAM[T](8*8*9).effort(0).buffer.conflictable

      Sequential.Foreach(signals by 1){ _ => 
        work_x_sram load work_x_dram
        work_y_sram load work_y_dram

        val reversed_LUT = LUT[Int](8)(0,4,2,6,1,5,3,7)

        def twiddles8(tid: I32, i: Int, N: Int): Unit = {
          // List.tabulate(7) { jj =>
          //   val j = jj + 1 
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
          ()
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
        Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
          val sx = 66
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = hi*8+lo // * here but >> above????
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_rhs_LUT(i)
          smem(lhs_factor*sx + offset) = work_x_sram(rhs_factor, tid)
        }

        // Loop 3
        Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
          val sx = 8
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = lo*66 + hi
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_lhs_LUT(i) // [sic]
          work_x_sram(lhs_factor, tid) = smem(rhs_factor*sx+offset)
        }

        // Loop 4
        Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
          val sx = 66
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = hi*8+lo // * here but >> above????
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_rhs_LUT(i)
          smem(lhs_factor*sx + offset) = work_y_sram(rhs_factor, tid)
        }

        // Loop 5
        Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
          val sx = 8
          val hi = tid.to[I32] >> 3;
          val lo = tid.to[I32] & 7;
          val offset = lo*66+hi
          work_y_sram(i, tid) = smem(i.to[I32]*sx+offset)
        }

        // Loop 6 
        Sequential.Foreach(THREADS by 1) { tid => 
          FFT8(tid)
          val hi = tid.to[I32] >> 3
          twiddles8(tid, hi, 64)
        }

        // Loop 7
        Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
          val sx = 72
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = hi*8 + lo
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_rhs_LUT(i)
          smem(lhs_factor * sx + offset) = work_x_sram(rhs_factor, tid)
        }

        // Loop 8
        Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
          val sx = 8
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = hi*72 + lo
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_lhs_LUT(i) // [sic]
          work_x_sram(lhs_factor, tid) = smem(rhs_factor * sx + offset)
        }

        // Loop 9
        Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
          val sx = 72
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = hi*8 + lo
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_rhs_LUT(i)
          smem(lhs_factor * sx + offset) = work_y_sram(rhs_factor, tid)
        }

        // Loop 10
        Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
          val sx = 8
          val hi = tid.to[I32] >> 3
          val lo = tid.to[I32] & 7
          val offset = hi*72 + lo
          work_y_sram(i, tid) = smem(i.to[I32] * sx + offset)
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
    println("X cksum: " + cksumX + ", Y cksum: " + cksumY)
    println("PASS: " + cksum + " (FFT_Transpose)")
    assert(cksum)
  }
}


@spatial class FFT_4 extends SpatialTest { // with II forced to 1 in some places
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

    setMem(work_x_dram, data_x)
    setMem(work_y_dram, data_y)

    val signals = ArgIn[Int]
    setArg(signals, args(0).to[Int])
    Accel{
      val work_x_sram = SRAM[T](8,stride).effort(0).buffer.conflictable
      val work_y_sram = SRAM[T](8,stride).effort(0).buffer.conflictable
      val smem = SRAM[T](8*8*9).effort(0).buffer.conflictable

      Foreach(signals by 1){ _ => 
        work_x_sram load work_x_dram
        work_y_sram load work_y_dram

        val reversed_LUT = LUT[Int](8)(0,4,2,6,1,5,3,7)

        def twiddles8(tid: I32, i: Int, N: Int): Unit = {
          // List.tabulate(7) { jj =>
          //   val j = jj + 1 
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
          ()
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
          Pipe.II(1).Foreach(0 until 2 by 1) { j => 
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

        Pipe{
          // Loop 2
          Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
            val sx = 66
            val hi = tid.to[I32] >> 3
            val lo = tid.to[I32] & 7
            val offset = hi*8+lo // * here but >> above????
            val lhs_factor = shuffle_lhs_LUT(i)
            val rhs_factor = shuffle_rhs_LUT(i)
            smem(lhs_factor*sx + offset) = work_x_sram(rhs_factor, tid)
          }

          // Loop 3
          Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
            val sx = 8
            val hi = tid.to[I32] >> 3
            val lo = tid.to[I32] & 7
            val offset = lo*66 + hi
            val lhs_factor = shuffle_lhs_LUT(i)
            val rhs_factor = shuffle_lhs_LUT(i) // [sic]
            work_x_sram(lhs_factor, tid) = smem(rhs_factor*sx+offset)
          }

          // Loop 4
          Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
            val sx = 66
            val hi = tid.to[I32] >> 3
            val lo = tid.to[I32] & 7
            val offset = hi*8+lo // * here but >> above????
            val lhs_factor = shuffle_lhs_LUT(i)
            val rhs_factor = shuffle_rhs_LUT(i)
            smem(lhs_factor*sx + offset) = work_y_sram(rhs_factor, tid)
          }

          // Loop 5
          Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
            val sx = 8
            val hi = tid.to[I32] >> 3;
            val lo = tid.to[I32] & 7;
            val offset = lo*66+hi
            work_y_sram(i, tid) = smem(i.to[I32]*sx+offset)
          }
        }

        // Loop 6 
        Sequential.Foreach(THREADS by 1) { tid => 
          FFT8(tid)
          val hi = tid.to[I32] >> 3
          twiddles8(tid, hi, 64)
        }

        Pipe{
          // Loop 7
          Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
            val sx = 72
            val hi = tid.to[I32] >> 3
            val lo = tid.to[I32] & 7
            val offset = hi*8 + lo
            val lhs_factor = shuffle_lhs_LUT(i)
            val rhs_factor = shuffle_rhs_LUT(i)
            smem(lhs_factor * sx + offset) = work_x_sram(rhs_factor, tid)
          }

          // Loop 8
          Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
            val sx = 8
            val hi = tid.to[I32] >> 3
            val lo = tid.to[I32] & 7
            val offset = hi*72 + lo
            val lhs_factor = shuffle_lhs_LUT(i)
            val rhs_factor = shuffle_lhs_LUT(i) // [sic]
            work_x_sram(lhs_factor, tid) = smem(rhs_factor * sx + offset)
          }

          // Loop 9
          Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
            val sx = 72
            val hi = tid.to[I32] >> 3
            val lo = tid.to[I32] & 7
            val offset = hi*8 + lo
            val lhs_factor = shuffle_lhs_LUT(i)
            val rhs_factor = shuffle_rhs_LUT(i)
            smem(lhs_factor * sx + offset) = work_y_sram(rhs_factor, tid)
          }

          // Loop 10
          Foreach(THREADS by 1, 8 by 1) { (tid,i) => 
            val sx = 8
            val hi = tid.to[I32] >> 3
            val lo = tid.to[I32] & 7
            val offset = hi*72 + lo
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
    println("X cksum: " + cksumX + ", Y cksum: " + cksumY)
    println("PASS: " + cksum + " (FFT_Transpose)")
    assert(cksum)
  }
}



