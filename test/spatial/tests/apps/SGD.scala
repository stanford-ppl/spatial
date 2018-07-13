package spatial.tests.apps

import spatial.dsl._

/*
         Minibatch impelementation:
                             _
                            | |
                            |M|
                            | |
                            | |
                            | |
                            | |
                  D         |_|
             _____________   _       _                  _
            |             | |^|     | |                | |
          N |      X      | |Y|  -  |Y|  =>            |Y_err
            |_____________| |_|     |_|                |_|
                                                ____    _        _      _
                                               |    |  | |      | |    | |
                                               |    |  | |      |M|    |M|
                                               |    |  |?|  +   | | -> | |
                                               | X_T|  | |      | |    | |
                                               |    |  | |      | |    | |
                                               |    |  | |      | |    | |
                                               |____|  |_|      |_|    |_|
*/
@spatial class SGD extends SpatialTest { // Test Args: 


  type TM = FixPt[TRUE, _9, _23]
  type TX = FixPt[TRUE, _9, _7]

  val loadPar = 16 (1 -> 32)
  val storePar = 16 (1 -> 32)
  val max_history = 512
  val PX = 1
  val P10 = 4 (1 -> 8)
  val P13 = 4 (1 -> 8)

  val tileSize = 256 //192

  val debug = true

  def getValue(table: Matrix[String], key: String): Float = {
    val sum = Array.tabulate(table.rows){i => if (table(i,0) == key) table(i,1).to[Float] else 0.to[Float]}.reduce{_+_}
    if (sum == 0.to[Float]) println("WARN: Possibly could not find " + key)
    sum
  }

  def main(args: Array[String]): Void = {
    val config = loadCSV2D[String](s"$DATA/training/sgd.config", "\n")
    printMatrix(config, "Config")

    val epochs = getValue(config, "epochs").to[Int]
    val points = getValue(config, "points").to[Int] // Total Points
    val alpha1 = getValue(config, "alpha1").to[TM] // Step size
    val alpha2 = getValue(config, "alpha2").to[TM]
    val bump_epoch = getValue(config, "bump_epoch").to[Int]
    val track = getValue(config, "track").to[Int] // Track cost vs time
    val threshold = getValue(config, "threshold").to[TM] // Cost at which to quit (only quits if track is on)
    val variance = getValue(config, "variance").to[Int] // numerator for noise
    val D = 128
    val maxX = 6

    val noise_num = variance
    val noise_denom = 10
    
    // Generate some test data
    val sX = (0::points, 0::D){(i,j) => (random[TX](maxX) - (maxX/3).to[TX])}
    val W_gold = Array.tabulate(D) { i => (random[TM](3) / 2)}
    val Y_noise = Array.tabulate(points) { i => (random[Int](noise_num).to[TM] - (noise_num.to[TM]/2)) / noise_denom.to[TM] }
    val sY = Array.tabulate(points) { i => Array.tabulate(D){j => (W_gold(j) * sX(i,j).to[TM])}.reduce{_+_} + Y_noise(i) }

    val E = ArgIn[Int]
    val N = ArgIn[Int]
    val A1 = ArgIn[TM]
    val A2 = ArgIn[TM]
    val BUMP_EPOCH = ArgIn[Int]
    val TRACK = ArgIn[Int]
    val THRESH = ArgIn[TM]
    val E_ACTUAL = HostIO[Int]

    setArg(E, epochs)
    setArg(N, points)
    setArg(A1, alpha1)
    setArg(A2, alpha2)
    setArg(BUMP_EPOCH, bump_epoch)
    setArg(TRACK, track)
    setArg(THRESH, threshold)
    setArg(E_ACTUAL, epochs-1)

    val x = DRAM[TX](N, D)
    val y = DRAM[TM](N)
    val w = DRAM[TM](D)
    val true_w = DRAM[TM](D)
    val cost = DRAM[TM](max_history)

    setMem(x, sX)
    setMem(y, sY)
    setMem(w, Array.fill(D)(0.to[TM]))
    setMem(cost, Array.fill(max_history)(0.to[TM]))
    setMem(true_w, W_gold)

    Accel {
      val y_cache = SRAM[TM](tileSize)
      val y_cache_base = Reg[Int](-1)
      val w_k = SRAM[TM](D)

      // Debug
      val cost_sram = SRAM[TM](max_history)
      val true_w_sram = SRAM[TM](D)
      if (TRACK.value == 1) true_w_sram load true_w

      w_k load w(0::D par loadPar)

      Sequential.Foreach(E by 1 par PX){e => 
        val A = if (e < BUMP_EPOCH) A1.value else A2.value
        // Choose random point
        val i = random[Int](8912) % N

        if (debug) println("i is " + i)
        // Get y for this point
        val y_point = Reg[TM](0) // DM*DX
        if (i - y_cache_base >= 0 && i - y_cache_base < tileSize && y_cache_base >= 0) {
          y_point := y_cache(i-y_cache_base)
        } else {
          y_cache_base := i - (i % tileSize)
          y_cache load y(y_cache_base::y_cache_base + tileSize par loadPar)
          y_point := y_cache(i % tileSize)
        }

        // Get x for this point
        val x_point = SRAM[TX](D) // DX
        x_point load x(i, 0::D par loadPar)

        // Compute gradient against w_k_t
        val y_hat = Reg[TM](0.to[TM])
        Reduce(y_hat)(D by 1 par P13){j => (w_k(j) *&! x_point(j).to[TM])}{_+!_}
        val y_err = y_hat.value -! y_point

        // Update w_k_t with reduced variance update
        Foreach(D by 1 par P10){i => w_k(i) = w_k(i) -! A.to[TM] *! y_err *! x_point(i).to[TM] }

        if (debug) { 
          println("*** Step " + {e} + ": ")
          println("y_err = " + y_err + " ( = " + y_hat.value + " - " + y_point + ")")
          Foreach(D by 1) { i => 
            val gradient = A.to[TM] *! y_err *! x_point(i).to[TM]

            print(" " + gradient)
          }
          println("\nWeights: ")
          Foreach(D by 1) { i => 
            print(" " + w_k(i))
          }
          println("\n")
        }

        if (TRACK.value == 1) {
          val current_cost = Reduce(Reg[TM](0))(D by 1){i => (w_k(i) - true_w_sram(i)) *! (w_k(i) - true_w_sram(i))}{_+!_}
          cost_sram(min((max_history-1).to[Int], e)) = current_cost
          if (current_cost < THRESH) {
            E_ACTUAL := min(e, (max_history-1).to[Int])
            cost(0 :: E) store cost_sram
            w(0 :: D par storePar) store w_k

            breakpoint()
          }
        }

      }

      if (TRACK.value == 1) cost(0 :: E) store cost_sram
      w(0 :: D par storePar) store w_k

    }

    val result = getMem(w)

    val cartesian_dist = W_gold.zip(result) { case (a, b) => (a - b) * (a - b) }.reduce{_+_}

    printArray(result, "result: ")
    printArray(W_gold, "gold: ")
    println("Dist: " + cartesian_dist + " <? " + threshold.to[TM])
    println("Finished in " + getArg(E_ACTUAL) + " epochs (out of " + getArg(E) + ")")

    if (track == 1) {
      val cost_result = getMem(cost)
      val hist_len = min(getArg(E_ACTUAL), max_history.to[Int])
      val relevent_history = Array.tabulate(hist_len){i => cost_result(i)}
      printMatrix(relevent_history.reshape(hist_len, 1), "Cost vs iter:")
    }


    val cksum = cartesian_dist < threshold.to[TM]
    println("PASS: " + cksum + " (SGD)")
  }
}
