package spatial.tests.feature.dense


import spatial.dsl._


/*
Implementation based on http://cs.stanford.edu/people/karpathy/visml/ising_example.html
 pi(x) = exp(J* 𝚺x_j*x_i + J_b * 𝚺b_i*x_i)
 let x' = x with one entry flipped
 Prob(accept x') ~ min(1, pi(x')/pi(x)) = exp(-2*J*𝚺x_j*x_i)*exp(-2*J_b*𝚺b_i*x_i)
Use args 100 0.4 0 to get a nice looking lava lamp pattern, or 0.8 for scala


          _________________________________________
         |                                         |
         | update --->                             |
x_par=4  |       --->       X                XX    |
         |      --->                       XXXX    |
         |     --->     .------------.X   X XXX    |
         |          X   .BIAS REGION .  XX   X     |
         |            XX.            .     XX      |
         |              .     X XX   .             |
         |X             .      XXXXX .             |
         |              .      XXXX  .             |
         |      X XXX   .------------.             |
         |     X XXX        XX         X           |
         |                                         |
         |                                  X      |
         |_________________________________________|

*/
@spatial class Gibbs_Ising2D extends SpatialTest {
  override def dseModelArgs: Args = "25 -2 99"
  override def finalModelArgs: Args = "25 0 -1 -2 -3"
  override def runtimeArgs: Args = "25 0.3 1"


  type T = FixPt[TRUE,_16,_16] // FixPt[TRUE,_32,_32]
  type PROB = FixPt[FALSE, _0, _8]


  def main(args: Array[String]): Unit = {

    val COLS = 64
    val ROWS = 32
    val lut_size = 9
    val border = -1

    val I = args(0).to[Int] // Number of iterations to run
    val J = args(1).to[T] // Energy scalar for edge
    val J_b = args(2).to[T] // Energy scalar for external field

    // Args
    val iters = ArgIn[Int]
    val exp_negbias = ArgIn[T]
    val exp_posbias = ArgIn[T]

    // Set up lut for edge energy ratio
    // 𝚺 x_j * x_i can be from -4 to +4
    val exp_data = Array.tabulate[T](lut_size){i =>
      val x = i - 4
      exp(x.to[Float]*J.to[Float] * -2.to[Float]).to[T]
    }
    // Set up args for bias energy ratio
    val exp_neg = exp(-J_b.to[Float]*2.to[Float]).to[T]
    val exp_pos = exp(J_b.to[Float]*2.to[Float]).to[T]

    // Debugging
    printArray(exp_data, "exp data")
    println("neg: " + exp_neg)
    println("pos: " + exp_pos)

    // Set initial and bias patterns:
    // Checkerboard
    val grid_init = (0::ROWS, 0::COLS){(i,j) => if ((i+j)%2 == 0) -1.to[Int] else 1.to[Int]}
    // // Square
    // val grid_init = (0::ROWS, 0::COLS){(i,j) => if (i > ROWS/4 && i < 3*ROWS/4 && j > COLS/4 && j < 3*COLS/4) -1.to[Int] else 1.to[Int]}

    val par_load = 16
    val par_store = 16
    val x_par = 4 (1 -> 1 -> 16)

    // Square
    val bias_matrix = (0::ROWS, 0::COLS){(i,j) => if (i > ROWS/4 && i < 3*ROWS/4 && j > COLS/4 && j < 3*COLS/4) -1.to[Int] else 1.to[Int]}

    val exp_lut = DRAM[T](lut_size)
    val grid_dram = DRAM[Int](ROWS,COLS)
    val bias_dram = DRAM[Int](ROWS,COLS)

    setMem(grid_dram, grid_init)
    setMem(bias_dram, bias_matrix)
    setMem(exp_lut, exp_data)
    setArg(exp_negbias, exp_neg)
    setArg(exp_posbias, exp_pos)
    setArg(iters, I)

    Accel{
      val exp_sram = SRAM[T](lut_size)
      // val grid_sram = SRAM[Int](ROWS,COLS).flat
      val grid_sram = SRAM[Int](ROWS,COLS).noflat.noduplicate.effort(0)
      exp_sram load exp_lut
      grid_sram load grid_dram(0::ROWS, 0::COLS par par_load)
      // Issue #187
      val bias_sram = SRAM[Int](ROWS,COLS).effort(1)
      bias_sram load bias_dram(0::ROWS, 0::COLS par par_load)


      Foreach(iters by 1) { iter =>
        Foreach(ROWS by 1 par x_par) { i =>
          // Update each point in active row
          val this_body = i % x_par
          Sequential.Foreach(-this_body until COLS by 1) { j =>
            // val col = j - this_body
            val N = grid_sram((i+1)%ROWS, j)
            val E = grid_sram(i, (j+1)%COLS)
            val S = grid_sram((i-1)%ROWS, j)
            val W = grid_sram(i, (j-1)%COLS)
            val self = grid_sram(i,j)
            val sum = (N+E+S+W)*self
            val p_flip = exp_sram(-sum+lut_size/2)
            val pi_x = exp_sram(sum+4) * mux((bias_sram(i,j) * self) < 0, exp_posbias, exp_negbias)
            val threshold = min(1.to[T], pi_x)
            val rng = random[PROB]
            val flip = mux(pi_x > 1, 1.to[T], mux(rng < threshold.bits(15::8).as[PROB], 1.to[T], 0.to[T]))
            if (j >= 0 && j < COLS) {
              grid_sram(i,j) = mux(flip == 1.to[T], -self, self)
            }
          }
        }
      }
      grid_dram(0::ROWS, 0::COLS par par_store) store grid_sram
    }

    val result = getMatrix(grid_dram)
    println("Ran for " + I + " iters.")
    // printMatrix(result, "Result matrix")

    print(" ")
    for( j <- 0 until COLS) { print("-")}
    for( i <- 0 until ROWS) {
      println("")
      print("|")
      for( j <- 0 until COLS) {
        if (result(i,j) == -1) {print("X")} else {print(" ")}
      }
      print("|")
    }
    println(""); print(" ")
    for( j <- 0 until COLS) { print("-")}
    println("")

    val blips_inside = (0::ROWS, 0::COLS){(i,j) =>
      if (i > ROWS/4 && i < 3*ROWS/4 && j > COLS/4 && j < 3*COLS/4) {
        if (result(i,j) != -1) 1 else 0
      } else { 0 }
    }.reduce{_+_}
    val blips_outside = (0::ROWS, 0::COLS){(i,j) =>
      if (i > ROWS/4 && i < 3*ROWS/4 && j > COLS/4 && j < 3*COLS/4) {
        0
      } else {
        if (result(i,j) != 1) 1 else 0
      }
    }.reduce{_+_}
    println("Found " + blips_inside + " blips inside the bias region and " + blips_outside + " blips outside the bias region")
    val cksum = (blips_inside + blips_outside) < (ROWS*COLS/8)
    println("PASS: " + cksum + " (Gibbs_Ising2D)")
    assert(cksum)
  }
}

