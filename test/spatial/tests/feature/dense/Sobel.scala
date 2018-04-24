package spatial.tests.feature.dense


import spatial.dsl._


// @test class Sobel extends SpatialTest { // ReviveMe
//   override def runtimeArgs: Args = "200 160"

//   val Kh = 3
//   val Kw = 3
//   val Cmax = 160


//   def convolve[T:Num](image: Matrix[T]): Matrix[T] = {
//     val B = 16 (1 -> 1 -> 16)

//     val R = ArgIn[Int]
//     val C = ArgIn[Int]
//     setArg(R, image.rows)
//     setArg(C, image.cols)


//     val lb_par = 16 (1 -> 1 -> 16)
//     val par_store = 16
//     val row_stride = 10 (100 -> 100 -> 500)
//     val row_par = 2 (1 -> 1 -> 16)
//     val par_Kh = 3 (1 -> 1 -> 3)
//     val par_Kw = 3 (1 -> 1 -> 3)

//     val img = DRAM[T](R, C)
//     val imgOut = DRAM[T](R, C)

//     setMem(img, image)

//     Accel {
//       Foreach(R by row_stride par row_par){ rr =>
//         val rows_todo = min(row_stride, R - rr)
//         val lb = LineBuffer[T](Kh, Cmax)
//         val sr = RegFile[T](Kh, Kw)
//         val lineOut = SRAM[T](Cmax)
//         val kh = LUT[T](3,3)(1.to[T], 0.to[T], -1.to[T],
//           2.to[T], 0.to[T], -2.to[T],
//           1.to[T], 0.to[T], -1.to[T])
//         val kv = LUT[T](3,3)(1.to[T],  2.to[T],  1.to[T],
//           0.to[T],  0.to[T],  0.to[T],
//           -1.to[T], -2.to[T], -1.to[T])

//         Foreach(-2 until rows_todo) { r =>
//           // println(" r is " + r)
//           val ldaddr = if ((r.to[I32]+rr.to[I32]) < 0.to[I32] || (r.to[I32]+rr.to[I32]) > R.value) 0.to[I32] else {r.to[I32]+rr.to[I32]}
//           lb load img(ldaddr, 0::C par lb_par)

//           Foreach(0 until C) { c =>
//             Pipe{sr.reset(c == 0)}

//             Foreach(0 until Kh par Kh){i => sr(i, *) <<= lb(i, c) }

//             val horz = Reduce(Reg[T])(Kh by 1 par par_Kh){i =>
//               Reduce(Reg[T])(Kw by 1 par par_Kw){j =>
//                 // val number = mux((r < 2) || (c < 2) , 0.to[T], sr(i,j))
//                 // number * kh(i,j)
//                 sr(i,j) * kh(i,j)
//               }{_+_}
//             }{_+_}
//             val vert = Reduce(Reg[T])(Kh by 1 par par_Kh){i =>
//               Reduce(Reg[T])(Kw by 1 par par_Kw){j =>
//                 // val number = mux((r < 2) || (c < 2) , 0.to[T], sr(i,j))
//                 // number * kv(i,j)
//                 sr(i,j) * kv(i,j)
//               }{_+_}
//             }{_+_}

//             lineOut(c) = mux(r.to[I32] + rr.to[I32] < 2.to[I32] || r.to[I32] + rr.to[I32] >= R-2, 0.to[T], abs(horz.value) + abs(vert.value))// Technically should be sqrt(horz**2 + vert**2)
//             // println("lineout c = " + mux(r.to[I32] + rr.to[I32] < 2.to[I32], 0.to[T], abs(horz.value) + abs(vert.value)))
//           }

//           if (r.to[I32]+rr.to[I32] < R && r.to[I32] >= 0.to[I32]) {
//             // println("storing to row " + {r+rr} + " from " + r + " " + rr)
//             // Foreach(0 until C){kk => print(" " + lineOut(kk))}
//             // println(" ")
//             imgOut(r.to[I32]+rr.to[I32], 0::C par par_store) store lineOut
//           }
//         }

//       }
//     }

//     getMatrix(imgOut)

//   }


//   def main(args: Array[String]): Unit = {
//     val R = args(0).to[Int] //1895
//     val C = args(1).to[Int] //1024
//     val border = 3
//     // val image = (0::R, 0::C){(i,j) => if (j > 3 && i > 3 && j < 11 && i < 11) 256 else 0 }
//     val image = (0::R, 0::C){(i,j) => if (j > border && j < C-border && i > border && i < C - border) i*16 else 0}
//     val ids = (0::R, 0::C){(i,j) => if (i < 2) 0 else 1}

//     val kh = List((List(1,2,1), List(0,0,0), List(-1,-2,-1)))
//     val kv = List((List(1,0,-1), List(2,0,-2), List(1,0,-1)))

//     val output = convolve(image)

//     /*
//       Filters:
//       1   2   1
//       0   0   0
//      -1  -2  -1

//       1   0  -1
//       2   0  -2
//       1   0  -1

//     */
//     val gold = (0::R, 0::C){(i,j) =>
//       if (i >= R-2) {
//         0
//       } else if (i >= 2 && j >= 2) {
//         val px00 = image(i,j)
//         val px01 = image(i,j-1)
//         val px02 = image(i,j-2)
//         val px10 = image(i-1,j)
//         val px11 = image(i-1,j-1)
//         val px12 = image(i-1,j-2)
//         val px20 = image(i-2,j)
//         val px21 = image(i-2,j-1)
//         val px22 = image(i-2,j-2)
//         abs(px00 * 1 + px01 * 2 + px02 * 1 - px20 * 1 - px21 * 2 - px22 * 1) + abs(px00 * 1 - px02 * 1 + px10 * 2 - px12 * 2 + px20 * 1 - px22 * 1)
//       } else {
//         0
//       }
//       // Shift result down by 2 and over by 2 because of the way accel is written
//     }

//     // // This contains the "weird scheduling bug"
//     printMatrix(image, "Image")
//     printMatrix(gold, "Gold")
//     printMatrix(output, "Output")

//     val gold_sum = gold.map{g => g}.reduce{_+_}
//     val output_sum = output.zip(ids){case (o,i) => i * o}.reduce{_+_}
//     println("gold " + gold_sum + " =?= output " + output_sum)
//     val cksum = gold_sum == output_sum
//     println("PASS: " + cksum + " (Sobel)")
//     assert(cksum)
//   }
// }