package spatial.tests.feature.dense

import spatial.dsl._

@spatial class Sobel extends SpatialTest {
  override def compileArgs: Args = super.compileArgs and "--forceBanking"
  override def runtimeArgs: Args = "200 160"


  def main(args: Array[String]): Unit = {
    // Set up kernel height and width
    val Kh = 3
    val Kw = 3

    // Set max number of columns and pad size
    val Cmax = 160
    val pad = 3
    
    // Set up data for kernels
    val kh_data = List(List(1,2,1), List(0,0,0), List(-1,-2,-1))
    val kv_data = List(List(1,0,-1), List(2,0,-2), List(1,0,-1))

    val B = 16

    // Get image size from command line
    val r = args(0).to[Int]
    val c = args(1).to[Int]

    // Generate some input data
    val image = (0::r, 0::c){(i,j) => if (j > pad && j < r-pad && i > pad && i < r - pad) i*16 else 0}

    // Set args
    val R = ArgIn[Int]
    val C = ArgIn[Int]
    setArg(R, image.rows)
    setArg(C, image.cols)

    // Set up parallelization factors
    val lb_par = 16 (1 -> 1 -> 16)
    val par_store = 16
    val row_stride = 10 (100 -> 100 -> 500)
    val row_par = 2 (1 -> 1 -> 16)
    val par_Kh = 3 (1 -> 1 -> 3)
    val par_Kw = 3 (1 -> 1 -> 3)

    // Set up input and output images
    val img = DRAM[Int](R, C)
    val imgOut = DRAM[Int](R, C)

    // Transfer data to memory
    setMem(img, image)

    Accel {
      // Iterate over row tiles
      Foreach(R by row_stride par row_par){ rr =>

        // Handle edge case with number of rows to do in this tile
        val rows_todo = min(row_stride, R - rr)

        // Create line buffer, shift reg, and result
        val lb = LineBuffer[Int](Kh, Cmax)
        val sr = RegFile[Int](Kh, Kw)
        val lineOut = SRAM[Int](Cmax)

        // Use Scala lists defined above for populating LUT
        val kh = LUT[Int](3,3)(kh_data.flatten.map(_.to[Int]):_*)
        val kv = LUT[Int](3,3)(kv_data.flatten.map(_.to[Int]):_*)

        // Iterate over each row in tile
        Foreach(-2 until rows_todo) { r =>

          // Compute load address in larger image and load
          val ldaddr = if ((r+rr) < 0 || (r+rr) > R.value) 0 else {r+rr}
          lb load img(ldaddr, 0::C par lb_par)

          // Iterate over each column
          Foreach(0 until C) { c =>

            // Reset shift register
            Pipe{sr.reset(c == 0)}

            // Shift into 2D window
            Foreach(0 until Kh par Kh){i => sr(i, *) <<= lb(i, c) }

            val horz = Reduce(Reg[Int])(Kh by 1 par par_Kh, Kw by 1 par par_Kw){(i,j) =>
              sr(i,j) * kh(i,j)
            }{_+_}
            val vert = Reduce(Reg[Int])(Kh by 1 par par_Kh, Kw by 1 par par_Kw){(i,j) =>
              sr(i,j) * kv(i,j)
            }{_+_}

            // Store abs sum into answer memory
            lineOut(c) = mux(r + rr < 2 || r + rr >= R-2, 0.to[Int], abs(horz.value) + abs(vert.value))
          }

          // Only if current row is in-bounds, store result to output DRAM
          if (r+rr < R && r >= 0) imgOut(r+rr, 0::C par par_store) store lineOut
        }

      }
    }
    val output = getMatrix(imgOut)

    /*
      Filters:
      1   2   1
      0   0   0
     -1  -2  -1

      1   0  -1
      2   0  -2
      1   0  -1

    */
    // Compute gold check
    val gold = (0::R, 0::C){(i,j) =>
      if (i >= R-2) {
        0
      } else if (i >= 2 && j >= 2) {
        val px00 = image(i,j)
        val px01 = image(i,j-1)
        val px02 = image(i,j-2)
        val px10 = image(i-1,j)
        val px11 = image(i-1,j-1)
        val px12 = image(i-1,j-2)
        val px20 = image(i-2,j)
        val px21 = image(i-2,j-1)
        val px22 = image(i-2,j-2)
        abs(px00 * 1 + px01 * 2 + px02 * 1 - px20 * 1 - px21 * 2 - px22 * 1) + abs(px00 * 1 - px02 * 1 + px10 * 2 - px12 * 2 + px20 * 1 - px22 * 1)
      } else {
        0
      }
      // Shift result down by 2 and over by 2 because of the way accel is written
    }

    printMatrix(image, "Image")
    printMatrix(gold, "Gold")
    printMatrix(output, "Output")

    val cksum = gold == output
    println("PASS: " + cksum + " (Sobel)")
    assert(cksum)
  }
}