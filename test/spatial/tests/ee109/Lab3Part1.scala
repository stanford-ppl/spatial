// package spatial.tests.ee109
// import spatial.dsl._


// @spatial class DiffAndSobel extends SpatialTest {
    
//     def Conv1D[T:Num](output: DRAM1[T],
//                         input: DRAM1[T],
//                         filter: DRAM1[T],
//                         window: scala.Int, vec_tile: scala.Int): Unit = {

//         val filter_data = RegFile[T](window)
//         filter_data load filter
//         Foreach(input.size by vec_tile){i =>
//           val numel = min(vec_tile.to[Int], input.size-i)
//           val x_tile = SRAM[T](vec_tile)
//           val y_tile = SRAM[T](vec_tile)
//           x_tile load input(i::i+numel)

//           val sr1D = RegFile[T](1,window)
//           Foreach(numel by 1){j =>
//             sr1D(0,*) <<= x_tile(j) // Shift new point into sr1D
//             y_tile(j) = Reduce(Reg[T])(window by 1){k =>
//               val data = mux(i + j - k < 0, 0.to[T], sr1D(0,k)) // Handle edge case
//               data * filter_data(k)
//             }{_+_}
//           }

//           output(i::i+numel) store y_tile
//         }
//     }



//     def Sobel2D[T:Num](output: DRAM2[T],
//                             input: DRAM2[T], maxcols: scala.Int): Unit = {

//         val lb = LineBuffer[T](3, maxcols)
//         val sr = RegFile[T](3, 3)
//         val kernelH = LUT[T](3,3)(1.to[T], 2.to[T], 1.to[T],
//                                   0.to[T], 0.to[T], 0.to[T],
//                                  -1.to[T],-2.to[T],-1.to[T])
//         val kernelV = LUT[T](3,3)(1.to[T], 0.to[T], -1.to[T],
//                                   2.to[T], 0.to[T], -2.to[T],
//                                   1.to[T], 0.to[T], -1.to[T])
//         val lineout = SRAM[T](maxcols)
//         Foreach(input.rows by 1){row =>
//           lb load input(row, 0::input.cols)
//           Foreach(input.cols by 1){j =>
//             Foreach(3 by 1 par 3){i => sr(i,*) <<= lb(i,j)}
//             val accumH = Reduce(Reg[T](0.to[T]))(3 by 1, 3 by 1){(ii,jj) =>
//               val img = if (row - 2 + ii.to[Int] < 0 || j.to[Int] - 2 + jj.to[Int] < 0) 0.to[T] else sr(ii, 2 - jj)
//               img * kernelH(ii,jj)
//             }{_+_}
//             val accumV = Reduce(Reg[T](0.to[T]))(3 by 1, 3 by 1){(ii,jj) =>
//               val img = if (row - 2 + ii.to[Int] < 0 || j.to[Int] - 2 + jj.to[Int] < 0) 0.to[T] else sr(ii, 2 - jj)
//               img * kernelV(ii,jj)
//             }{_+_}
//             lineout(j) = abs(accumV.value) + abs(accumH.value)
//           }
//           output(row, 0::input.cols) store lineout
//         }
//     }


//     override def runtimeArgs = "64 64 8"
    
    
//     def main() {

//       type T = FixPt[TRUE,_16,_16]

//       val R = args(0).to[Int]
//       val C = args(1).to[Int]
//       val vec_len = args(2).to[Int]
//       val vec_tile = 64
//       val maxcols = 128 // Required for LineBuffer
//       val ROWS = ArgIn[Int]
//       val COLS = ArgIn[Int]
//       val LEN = ArgIn[Int]
//       setArg(ROWS,R)
//       setArg(COLS,C)
//       setArg(LEN, vec_len)

//       val window = 16
//       val x_t = Array.tabulate(vec_len){i =>
//         val x = i.to[T] * (4.to[T] / vec_len.to[T]).to[T] - 2
//         println(" x " + x)
//         -0.18.to[T] * pow(x, 4) + 0.5.to[T] * pow(x, 2) + 0.8.to[T]
//       }
//       val h_t = Array.tabulate(16){i => if (i < window/2) 1.to[T] else -1.to[T]}
//       printArray(x_t, "x_t data:")

//       val X_1D = DRAM[T](LEN)
//       val H_1D = DRAM[T](window)
//       val Y_1D = DRAM[T](LEN)
//       setMem(X_1D, x_t)
//       setMem(H_1D, h_t)

//       val border = 3
//       val image = (0::R, 0::C){(i,j) => if (j > border && j < C-border && i > border && i < C - border) (i*16).to[T] else 0.to[T]}
//       printMatrix(image, "image: ")
//       val kernelv = Array[T](1,2,1,0,0,0,-1,-2,-1)
//       val kernelh = Array[T](1,0,-1,2,0,-2,1,0,-1)
//       val X_2D = DRAM[T](ROWS, COLS)
//       val Y_2D = DRAM[T](ROWS, COLS)
//       setMem(X_2D, image)

//       Accel{
//         Conv1D(Y_1D, X_1D, H_1D)
//         Sobel2D(Y_2D, X_2D)
//       }

//       val Y_1D_result = getMem(Y_1D)
//       val Y_2D_result = getMatrix(Y_2D)

//       val Y_1D_gold = Array.tabulate(vec_len){i =>
//         Array.tabulate(window){j =>
//           val data = if (i - j < 0) 0 else x_t(i-j)
//           data * h_t(j)
//         }.reduce{_+_}
//       }
//       val Y_2D_gold = (0::R, 0::C){(i,j) =>
//         val h = Array.tabulate(3){ii => Array.tabulate(3){jj =>
//           val img = if (i-ii < 0 || j-jj < 0) 0 else image(i-ii,j-jj)
//           img * kernelh((2-ii)*3+(2-jj))
//         }}.flatten.reduce{_+_}
//         val v = Array.tabulate(3){ii => Array.tabulate(3){jj =>
//           val img = if (i-ii < 0 || j-jj < 0) 0 else image(i-ii,j-jj)
//           img * kernelv((2-ii)*3+(2-jj))
//         }}.flatten.reduce{_+_}
//         abs(v) + abs(h)
//       }

//       printArray(Y_1D_result, "1D Result:")
//       printArray(Y_1D_gold, "1D Gold:")
//       printMatrix(Y_2D_result, "2D Result:")
//       printMatrix(Y_2D_gold, "2D Gold:")

//       val margin = 0.25.to[T]
//       val cksum_1D = Y_1D_result.zip(Y_1D_gold){(a,b) => abs(a - b) < margin}.reduce{_&&_}
//       val cksum_2D = Y_2D_result.zip(Y_2D_gold){(a,b) => abs(a - b) < margin}.reduce{_&&_}
//       println("1D Pass? " + cksum_1D + ", 2D Pass? " + cksum_2D)
//   }
// }