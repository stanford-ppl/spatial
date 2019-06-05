package spatial.tests.feature.banking

import argon._
import spatial.dsl._

@spatial class ScanLines extends SpatialTest {
  override def dseModelArgs: Args = "4 -1 99"
  override def finalModelArgs: Args = "4 -2 -1 0"
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {

    val COLS = 64
    val ROWS = 32
    val lut_size = 9
    val border = -1
    val P = 3

    // Set initial and bias patterns:
    // Checkerboard
    val grid_init = (0::ROWS, 0::COLS){(i,j) => i*COLS + j}
    // // Square
    // val grid_init = (0::ROWS, 0::COLS){(i,j) => if (i > ROWS/4 && i < 3*ROWS/4 && j > COLS/4 && j < 3*COLS/4) -1.to[Int] else 1.to[Int]}

    val par_load = 16
    val par_store = 16

    val grid_dram = DRAM[Int](ROWS,COLS)

    setMem(grid_dram, grid_init)
    val iters = ArgIn[Int]
    setArg(iters, 4)

    Accel{
      val grid_sram = SRAM[Int](ROWS,COLS).hierarchical.noduplicate
      grid_sram load grid_dram(0::ROWS, 0::COLS par par_load)

      Foreach(iters by 1) { iter =>
        Foreach(ROWS by 1 par P) { i =>
          val this_body = i % P
          Sequential.Foreach(-this_body until COLS by 1) { j =>
            val N = grid_sram((i+1)%ROWS, j)
            val E = grid_sram(i, (j+1)%COLS)
            val S = grid_sram((i-1)%ROWS, j)
            val W = grid_sram(i, (j-1)%COLS)
            val self = grid_sram(i,j)
            val sum = (N+E+S+W+self)/5
            if (j >= 0 && j < COLS) {
              grid_sram(i,j) = sum
            }
          }
        }
      }
      grid_dram(0::ROWS, 0::COLS par par_store) store grid_sram
    }

    val result = getMatrix(grid_dram)
    printMatrix(result, "Result matrix")
    val gold = loadCSV2D[Int](s"$DATA/unit/scanlines.csv", ",", "\n")
    printMatrix(gold, "Gold matrix")


    val cksum = gold == result
    println("PASS: " + cksum + " (ScanLines)")
    assert(cksum)
    // println("Found " + blips_inside + " blips inside the bias region and " + blips_outside + " blips outside the bias region")
    // val cksum = (blips_inside + blips_outside) < (ROWS*COLS/8)
    // println("PASS: " + cksum + " (Gibbs_Ising2D)")
    // assert(cksum)
  }
}
