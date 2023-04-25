package spatial.tests.ee109

import spatial.dsl._

@spatial class Lab3Part1Convolution extends SpatialTest {

  val Kh = 3
  val Kw = 3
  val Cmax = 16

  def convolve[T:Num](image: Matrix[T]): Matrix[T] = {
    val B = 16

    val R = ArgIn[Int]
    val C = ArgIn[Int]
    setArg(R, image.rows)
    setArg(C, image.cols)
    val lb_par = 8

    val img = DRAM[T](R, C)
    val imgOut = DRAM[T](R, C)

    setMem(img, image)

    Accel {
      val lb = LineBuffer[T](Kh, Cmax)

      val kh = LUT[T](3,3)(1.to[T], 0.to[T], -1.to[T],
        2.to[T], 0.to[T], -2.to[T],
        1.to[T], 0.to[T], -1.to[T])
      val kv = LUT[T](3,3)(1.to[T],  2.to[T],  1.to[T],
        0.to[T],  0.to[T],  0.to[T],
        -1.to[T], -2.to[T], -1.to[T])

      val sr = RegFile[T](Kh, Kw)
      val lineOut = SRAM[T](Cmax)

      Foreach(0 until R) { r =>
        lb load img(r, 0::C par lb_par)

        Sequential.Foreach(0 until C) { c =>
          Pipe{sr.reset(c == 0)}
          // Shifting in data from the line buffer
          Foreach(0 until Kh par Kh){i => sr(i, *) <<= lb(i, c) }
          Foreach(0 until Kh, 0 until Kw) {
            (i, j) =>
              println(r"sr($i, $j) = ${sr(i, j)}")
          }

          // Implement the computation part for a 2-D convolution.
          // Use horz and vert to store your convolution results.
          // Your code here:
          val horz = Reg[T](0)
          val vert = Reg[T](0)

          Reduce(horz)(0 until Kh par Kh) { xh =>
            Reduce(0.to[T])(0 until Kw par Kw) { yh =>
//              println(r"[XH, YH = $xh, $yh] ${sr(xh, yh)} * ${kh(xh, yh)}")
              sr(xh, yh) * kh(xh, yh)
            }{_+_}
          }{_+_}

          Reduce(vert)(0 until Kh par Kh) { xv =>
            Reduce(0.to[T])(0 until Kw par Kw) { yv =>
//              println(r"[XV, YV = $xv, $yv] ${sr(xv, yv)} * ${kh(xv, yv)}")
              sr(xv, yv) * kv(xv, yv)
            }{_+_}
          }{_+_}

          println(r"$r,$c Horiz = ${horz.value}, Vert = ${vert.value}")

          lineOut(c) = mux( (r < 2 || c < 2), 0.to[T], abs(horz.value) + abs(vert.value)) // Technically should be sqrt(horz**2 + vert**2)
        }
        imgOut(r, 0::C par 16) store lineOut
      }
    }

    getMatrix(imgOut)
  }

  def main(args: Array[String]): Unit = {
    val R = 16
    val C = 16
    val border = 3
    val image = (0::R, 0::C){(i,j) => if (j > border && j < C-border && i > border && i < C - border) i*16 else 0}
    val ids = (0::R, 0::C){(i,j) => if (i < 2) 0 else 1}

    val kh = List((List(1,2,1), List(0,0,0), List(-1,-2,-1)))
    val kv = List((List(1,0,-1), List(2,0,-2), List(1,0,-1)))

    val output = convolve(image)

    /*
      Filters:
      1   2   1
      0   0   0
     -1  -2  -1

      1   0  -1
      2   0  -2
      1   0  -1

    */
    val gold = (0::R, 0::C){(i,j) =>
      val px00 = if ((j-2) > border && (j-2) < C-border && (i-2) > border && (i-2) < C - border) (i-2)*16 else 0
      val px01 = if ((j-1) > border && (j-1) < C-border && (i-2) > border && (i-2) < C - border) (i-2)*16 else 0
      val px02 = if ((j+0) > border && (j+0) < C-border && (i-2) > border && (i-2) < C - border) (i-2)*16 else 0
      val px10 = if ((j-2) > border && (j-2) < C-border && (i-1) > border && (i-1) < C - border) (i-1)*16 else 0
      val px11 = if ((j-1) > border && (j-1) < C-border && (i-1) > border && (i-1) < C - border) (i-1)*16 else 0
      val px12 = if ((j+0) > border && (j+0) < C-border && (i-1) > border && (i-1) < C - border) (i-1)*16 else 0
      val px20 = if ((j-2) > border && (j-2) < C-border && (i+0) > border && (i+0) < C - border) (i+0)*16 else 0
      val px21 = if ((j-1) > border && (j-1) < C-border && (i+0) > border && (i+0) < C - border) (i+0)*16 else 0
      val px22 = if ((j+0) > border && (j+0) < C-border && (i+0) > border && (i+0) < C - border) (i+0)*16 else 0
      abs(px00 * 1 + px01 * 2 + px02 * 1 - px20 * 1 - px21 * 2 - px22 * 1) + abs(px00 * 1 - px02 * 1 + px10 * 2 - px12 * 2 + px20 * 1 - px22 * 1)
    };

    printMatrix(image, "Image")
    printMatrix(gold, "Gold")
    printMatrix(output, "Output")

    val gold_sum = gold.map{g => g}.reduce{_+_}
    val output_sum = output.zip(ids){case (o,i) => i * o}.reduce{_+_}
    println("gold " + gold_sum + " =?= output " + output_sum)
    val cksum = gold_sum == output_sum
    println("PASS: " + cksum + " (Convolution_FPGA)")
    assert(cksum)
  }
}

