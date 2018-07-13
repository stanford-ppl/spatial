package spatial.tests.apps

import spatial.dsl._

@spatial class SVRG extends SpatialTest {  // Test Args: 20 3 256 0.0001 0.0009 10 1 2

  type TM = FixPt[TRUE, _11, _21]
  type TX = FixPt[TRUE, _11, _21]

  val tileSize = 64 (16 -> 128)

  val loadPar = 16
  val storePar = 16
  val max_history = 512
  val P1 = 1 (1 -> 8)
  val P2 = 1 (1 -> 8)
  val P3 = 1 (1 -> 8)
  val P4 = 1 (1 -> 8)
  val P5 = 1 (1 -> 8)
  val P6 = 1 (1 -> 8)
  val P7 = 1 (1 -> 8)
  val P8 = 1 (1 -> 8)
  val P9 = 1 (1 -> 8)
  val P10 = 1 (1 -> 8)
  val P11 = 1 (1 -> 8)
  val P12 = 1 (1 -> 8)
  val PX = 1

  val debug = true

  def getValue(table: Matrix[String], key: String): Float = {
    val sum = Array.tabulate(table.rows){i => if (table(i,0) == key) table(i,1).to[Float] else 0.to[Float]}.reduce{_+_}
    if (sum == 0.to[Float]) println("WARN: Possibly could not find " + key)
    sum
  }

  def main(args: Array[String]): Void = {
    val config = loadCSV2D[String](s"$DATA/training/svrg.config", "\n")
    printMatrix(config, "Config")

    val epochs = getValue(config, "epochs").to[Int]
    val len_epoch = getValue(config, "len_epoch").to[Int]
    val points = getValue(config, "points").to[Int] // Total Points
    val alpha1 = getValue(config, "alpha1").to[TM] // Step size
    val alpha2 = getValue(config, "alpha2").to[TM]
    val bump_epoch = getValue(config, "bump_epoch").to[Int]
    val track = getValue(config, "track").to[Int] // Track cost vs time
    val threshold = getValue(config, "threshold").to[TM] // Cost at which to quit (only quits if track is on)
    val variance = getValue(config, "variance").to[Int] // numerator for noise
    val warmup = getValue(config, "warmup").to[Int]

    val maxX = 6
    val D = 128

    val noise_num = variance
    val noise_denom = 10
    // Generate some test data
    val sX = (0::points, 0::D){(i,j) => random[TX](maxX.to[TX]) - (maxX/3).to[TX]}
    val W_gold = Array.tabulate(D) { i => random[TM](3.to[TM]) / 2.to[TM]}
    val Y_noise = Array.tabulate(points) { i => (random[Int](noise_num).to[TX] - (noise_num.to[TX]/2)) / noise_denom.to[TX] }
    val sY = Array.tabulate(points) { i => Y_noise(i) + Array.tabulate(D){j => W_gold(j) * sX(i,j).to[TM]}.reduce{_+_}.to[TX] }

    val E = ArgIn[Int]
    val N = ArgIn[Int]
    val T = ArgIn[Int]
    val BUMP_EPOCH = ArgIn[Int]
    val A1 = ArgIn[TM]
    val A2 = ArgIn[TM]
    val TRACK = ArgIn[Int]
    val THRESH = ArgIn[TM]
    val E_ACTUAL = HostIO[Int]
    val WARMUP = ArgIn[Int]

    setArg(E, epochs)
    setArg(N, points)
    setArg(T, len_epoch)
    setArg(A1, alpha1)
    setArg(A2, alpha2)
    setArg(BUMP_EPOCH, bump_epoch)
    setArg(TRACK, track)
    setArg(THRESH, threshold)
    setArg(E_ACTUAL, epochs*len_epoch-1)
    setArg(WARMUP, warmup-1)

    val x = DRAM[TX](N, D)
    val y = DRAM[TX](N)
    val w = DRAM[TM](D)
    val true_w = DRAM[TM](D)
    val cost = DRAM[TM](max_history)

    if (points < 10) printMatrix(sX, "X Data")
    else {
      printMatrix((0::10, 0::D){(i,j) => sX(i,j)}, "X Data")
      println("... Skipped last " + {points-10} + " rows")
    }
    printArray(Y_noise, "Y noise")
    printArray(sY, "Y Data")
    printArray(W_gold, "W Gold")
    setMem(x, sX)
    setMem(y, sY)
    setMem(w, Array.fill(D)(0.to[TM]))
    setMem(true_w, W_gold)
    setMem(cost, Array.fill(D)(0.to[TM]))

    Accel { 
      // Create model and gradient memories
      val w_k = SRAM[TM](D)
      val g_k = SRAM[TM](D)
      val y_cache = SRAM[TX](tileSize)
      val y_cache_base = Reg[Int](-1)

      // Debug
      val true_w_sram = SRAM[TM](D)
      val cost_sram = SRAM[TM](max_history)
      val w_k_t = SRAM[TM](D)

      if (TRACK.value == 1) true_w_sram load true_w
      w_k load w(0::D par loadPar)
      w_k_t load w(0::D par loadPar)
      

      // Outer loop (epochs)
      Sequential.Foreach(E by 1 par PX) { e =>
        // Choose correct step for this epoch
        val A = mux(e < BUMP_EPOCH, A1.value, A2.value)

        // Do full update over all points to get g_k and w_k (outer loop)
        if (e > WARMUP.value) {
          MemReduce(g_k, 0.to[TM])(N by tileSize par P4){i => 
            val y_tile = SRAM[TX](tileSize)
            val x_tile = SRAM[TX](tileSize,D)
            Parallel{
              y_tile load y(i::i + tileSize)
              x_tile load x(i::i + tileSize, 0::D par loadPar)            
            }
            val g_k_partial = SRAM[TM](D)
            // Full update tile (inner loop)
            MemReduce(g_k_partial, 0.to[TM])(tileSize by 1 par P6){ii =>
              val g_k_local = SRAM[TM](D)
              val y_err = Reduce(Reg[TX](0.to[TX]))(D by 1 par P12){j => (w_k(j) *&! x_tile(ii, j).to[TM]).to[TX]}{_+!_} -! y_tile(ii)
              Foreach(D by 1 par P7){j => g_k_local(j) =  y_err.to[TM] *&! x_tile(ii, j).to[TM] / N.value.to[TM]}
              g_k_local
            }{_+!_}
          }{(a,b) => a +! b}

          // Accumulation here may not be necessary
          // // Accumulate g_k into w_k
          // MemFold(w_k par P8)(1 by 1){_ => g_k}{_+!_}
          ()
        }

        // Run len_epoch number of SGD points
        Sequential.Foreach(T by 1 par PX){t => 
          // Choose random point
          // val i = random[Int](8192) % N
          val i = (e*T+t) % N

          // Get y for this point
          val y_point = Reg[TX](0)
          if (i - y_cache_base >= 0 && i - y_cache_base < tileSize && y_cache_base >= 0) {
            y_point := y_cache(i-y_cache_base)
          } else {
            y_cache_base := i - (i % tileSize)
            y_cache load y(y_cache_base::y_cache_base + tileSize)
            y_point := y_cache(i % tileSize)
          }

          // Get x for this point
          val x_point = SRAM[TX](D)
          x_point load x(i, 0::D par loadPar)

          // Compute gradient against w_k_t
          val y_err_t = Reduce(Reg[TX](0.to[TX]))(D by 1){j => (w_k_t(j) *&! x_point(j).to[TM]).to[TX]}{_+!_} -! y_point

          // Compute gradient against w_k
          val y_err_k = if (e > WARMUP.value) {Reduce(Reg[TX](0.to[TX]))(D by 1){j => (w_k(j) *&! x_point(j).to[TM]).to[TX]}{_+!_} -! y_point} else 0.to[TX]

          // Update w_k_t with reduced variance update
          Foreach(D by 1 par P10){i => w_k_t(i) = w_k_t(i) -! (A *&! ((y_err_t *&! x_point(i)).to[TM] +! mux(e > WARMUP.value, -(y_err_k *&! x_point(i)).to[TM] +! g_k(i),0.to[TM]) ))}

          if (TRACK.value == 1) {
            val current_cost = Reduce(Reg[TM](0))(D by 1){i => pow(w_k_t(i) - true_w_sram(i), 2)}{_+!_}
            cost_sram(min((max_history-1).to[Int], e*T+t)) = current_cost
            if (current_cost < THRESH) {
              E_ACTUAL := min(e*T+t, (max_history-1).to[Int])
              cost(0 :: min((max_history-1).to[Int], getArg(E_ACTUAL))) store cost_sram
              w(0 :: D par storePar) store w_k_t

              breakpoint()
            }
          }

          if (debug) { 
            println("*** Step " + {t + e*T} + ": ")
            println("y_err_t = " + y_err_t + " ( = " + {y_err_t + y_point} + " - " + y_point + "), A = " + A.to[TM])
            Foreach(5 by 1) { i => 
              val gradientLP = A *&! ((y_err_t *&! x_point(i)).to[TM] +! mux(e > WARMUP.value, -(y_err_k *&! x_point(i)).to[TM] +! g_k(i),0.to[TM]) )

              print(" " + gradientLP + " (" + {y_err_t *&! x_point(i)} + " +! " + mux(e > WARMUP.value, -(y_err_k *&! x_point(i)).to[TM],0.to[TM]) + " +! " + mux(e > WARMUP.value, g_k(i).to[TM], 0.to[TM]) + ")")
            }
            println("\nWeights: ")
            Foreach(5 by 1) { i => 
              print(" " + w_k_t(i))
            }
            println("\n")
          }

        }
        // Copy w_k_t to w_k
        if (e >= WARMUP.value) Foreach(D by 1 par P11){i => w_k(i) = w_k_t(i)}
      }

      // Store back values
      if (TRACK.value == 1) cost(0 :: min((max_history-1).to[Int], getArg(E_ACTUAL))) store cost_sram
      w(0 :: D par storePar) store w_k_t
    } // Close accel

    
    val w_result = getMem(w)
    val cartesian_dist = W_gold.zip(w_result) { case (a, b) => (a - b) * (a - b) }.reduce{_+_}


    if (track == 1) {
      val cost_result = getMem(cost)
      val hist_len = min(getArg(E_ACTUAL), max_history.to[Int])
      val relevent_history = Array.tabulate[TM](hist_len){i => i.to[TM]} ++ Array.tabulate(hist_len){i => cost_result(i)}
      val reshaped = (0::hist_len,0::2){(i,j) => relevent_history(j * hist_len + i)}
      printMatrix(reshaped, "Cost vs iter:")
    }

    val cksum = cartesian_dist < threshold
    printArray(w_result, "result: ")
    printArray(W_gold, "gold: ")
    println("Cartesian Distance From W_gold: " + cartesian_dist.to[TM] + " <? " + {threshold.to[TM]})
    println("Finished in " + getArg(E_ACTUAL) + " epochs (out of " + {epochs*len_epoch} + ")")


    println("PASS: " + cksum + " (SVRG)")
  }
}
