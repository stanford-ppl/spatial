// package spatial.tests.feature.dense

// import spatial.dsl._

// @test class Convolution_FPGA extends SpatialTest { // ReviveMe
//   override def runtimeArgs: Args = NoArgs

//   val Kh = 3
//   val Kw = 3
//   val Cmax = 16


//   def convolve[T:Num](image: Matrix[T]): Matrix[T] = {
//     val B = 16 (1 -> 1 -> 16)

//     val R = ArgIn[Int]
//     val C = ArgIn[Int]
//     setArg(R, image.rows)
//     setArg(C, image.cols)
//     val lb_par = 8

//     val img = DRAM[T](R, C)
//     val imgOut = DRAM[T](R, C)

//     setMem(img, image)

//     Accel {
//       val lb = LineBuffer[T](Kh, Cmax)

//       val kh = LUT[T](3,3)(1.to[T], 0.to[T], -1.to[T],
//         2.to[T], 0.to[T], -2.to[T],
//         1.to[T], 0.to[T], -1.to[T])
//       val kv = LUT[T](3,3)(1.to[T],  2.to[T],  1.to[T],
//         0.to[T],  0.to[T],  0.to[T],
//         -1.to[T], -2.to[T], -1.to[T])

//       val sr = RegFile[T](Kh, Kw)
//       val lineOut = SRAM[T](Cmax)

//       Foreach(0 until R) { r =>
//         lb load img(r, 0::C par lb_par)

//         /*println("Row " + r)
//         Foreach(0 until Kh) { i =>
//           Foreach(0 until C) { c => print("" + lb(i,c) + "\t") }
//           println("")
//         }*/

//         Foreach(0 until C) { c =>
//           Pipe{sr.reset(c == 0)}

//           Foreach(0 until Kh par Kh){i => sr(i, *) <<= lb(i, c) }

//           val horz = Reduce(Reg[T])(Kh by 1, Kw by 1){ (i,j) =>
//             // val number = mux((r < 2) || (c < 2) , 0.to[T], sr(i,j))
//             // number * kh(i,j)
//             sr(i,j) * kh(i,j)
//           }{_+_}
//           val vert = Reduce(Reg[T])(Kh by 1, Kw by 1){ (i,j) =>
//             // val number = mux((r < 2) || (c < 2) , 0.to[T], sr(i,j))
//             // number * kv(i,j)
//             sr(i,j) * kv(i,j)
//           }{_+_}

//           lineOut(c) = mux( r < 2, 0.to[T], abs(horz.value) + abs(vert.value)) // Technically should be sqrt(horz**2 + vert**2)
//         }

//         imgOut(r, 0::C par 16) store lineOut
//       }

//     }

//     getMatrix(imgOut)

//   }


//   def main(args: Array[String]): Unit = {
//     val R = 16
//     val C = 16
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
//       // Shift result down by 2 and over by 2 because of the way accel is written
//       val px00 = if ((j-2) > border && (j-2) < C-border && (i-2) > border && (i-2) < C - border) (i-2)*16 else 0
//       val px01 = if ((j-1) > border && (j-1) < C-border && (i-2) > border && (i-2) < C - border) (i-2)*16 else 0
//       val px02 = if ((j+0) > border && (j+0) < C-border && (i-2) > border && (i-2) < C - border) (i-2)*16 else 0
//       val px10 = if ((j-2) > border && (j-2) < C-border && (i-1) > border && (i-1) < C - border) (i-1)*16 else 0
//       val px11 = if ((j-1) > border && (j-1) < C-border && (i-1) > border && (i-1) < C - border) (i-1)*16 else 0
//       val px12 = if ((j+0) > border && (j+0) < C-border && (i-1) > border && (i-1) < C - border) (i-1)*16 else 0
//       val px20 = if ((j-2) > border && (j-2) < C-border && (i+0) > border && (i+0) < C - border) (i+0)*16 else 0
//       val px21 = if ((j-1) > border && (j-1) < C-border && (i+0) > border && (i+0) < C - border) (i+0)*16 else 0
//       val px22 = if ((j+0) > border && (j+0) < C-border && (i+0) > border && (i+0) < C - border) (i+0)*16 else 0
//       abs(px00 * 1 + px01 * 2 + px02 * 1 - px20 * 1 - px21 * 2 - px22 * 1) + abs(px00 * 1 - px02 * 1 + px10 * 2 - px12 * 2 + px20 * 1 - px22 * 1)
//     };

//     // // This contains the "weird scheduling bug"
//     printMatrix(image, "Image")
//     printMatrix(gold, "Gold")
//     printMatrix(output, "Output")

//     val gold_sum = gold.map{g => g}.reduce{_+_}
//     val output_sum = output.zip(ids){case (o,i) => i * o}.reduce{_+_}
//     println("gold " + gold_sum + " =?= output " + output_sum)
//     val cksum = gold_sum == output_sum
//     println("PASS: " + cksum + " (Convolution_FPGA)")
//     assert(cksum)
//   }
// }
