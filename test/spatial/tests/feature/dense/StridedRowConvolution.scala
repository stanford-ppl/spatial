package spatial.tests.feature.dense


import spatial.dsl._


// @test class StridedRowConvolution extends SpatialTest { // ReviveMe
//   override def runtimeArgs: Args = NoArgs


//   def main(args: Array[String]): Unit = {
//     val R = 20
//     val C = 16

//     val mat = (0::R,0::C){(i,j) => i }

//     val img = DRAM[Int](R, C)
//     val out = DRAM[Int](R/2, C)
//     val out2 = DRAM[Int](R/2-2, C)
//     setMem(img, mat)

//     Accel {
//       // Test regular row strided lb
//       val lb = LineBuffer.strided[Int](3, C, 2)
//       Foreach(R/2 by 1){row =>
//         val line = SRAM[Int](C)
//         lb load img(row*2::row*2+2, 0::C)
//         Foreach(C by 1){col =>
//           val conv = Reduce(0)(3 by 1, 3 by 1){(r,c) => if (row - 1 + r < 0) 0 else lb(r, (col + c)%C)}{_+_} / 9
//           line(col) = conv
//         }
//         out(row,0::C) store line
//       }

//       // Test lb with transient load
//       val lb2 = LineBuffer.strided[Int](5, C, 2)
//       lb2 load img(0::3, 0::C)
//       Foreach(R/2-2 by 1){row =>
//         val line = SRAM[Int](C)
//         val rowstart = 3 + row*2
//         lb2 load img(rowstart::rowstart+2, 0::C)
//         Foreach(C by 1){col =>
//           val conv = Reduce(0)(5 by 1, 3 by 1){(r,c) => lb2(r, (col + c)%C)}{_+_} / 15
//           line(col) = conv
//         }
//         out2(row,0::C) store line
//       }

//     }

//     val result = getMatrix(out)
//     val result2 = getMatrix(out2)

//     printMatrix(mat, "Input")
//     printMatrix(result, "Result")
//     printMatrix(result2, "Result2")
//     val gold = (0::R/2, 0::C){(i,j) => 2*i}
//     val gold2 = (0::R/2-2, 0::C){(i,j) => 2*i+2}

//     val cksum = result.zip(gold){_==_}.reduce{_&&_}
//     val cksum2 = result2.zip(gold2){_==_}.reduce{_&&_}
//     println("PASS: " + {cksum && cksum2} + " (SimpleRowStridedConv)")
//   }
// }

