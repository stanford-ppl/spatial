import spatial.dsl._

@spatial class LPSVRG_1 extends SpatialTest {  // Test Args: 


  val tileSize = 64 (16 -> 128)

  val loadPar = 16
  val storePar = 16
  val max_history = 512
  val P3 =  1 (1 -> 8)
  val P4 =  1 (1 -> 8)
  val P5 =  1 (1 -> 8)
  val P6 =  1 (1 -> 8)
  val P7 =  1 (1 -> 8)
  val P8 =  1 (1 -> 8)
  val P9 =  1 (1 -> 8)
  val P10 = 1 (1 -> 8)
  val P11 = 1 (1 -> 8)
  val P12 = 1 (1 -> 8)
  val P13 = 1 (1 -> 8)
  val P14 = 1 (1 -> 8)
  val PX = 1

  val shift_factor = 16

  type B = Int8
  type BB = Int16
  type BBBB = Int32

  val debug = true
  /*

    Basic type relationships:
      X ->  (DX,    B)
      W ->  (DM,    B)
      Y ->  (DM*DX, BB)
      GR -> (DG,    BBBB)   -> (DM*DX*DX*DA, BBBB)

      ... Choose DA so that DG is off by a power of 2 from DM.  I.E.- DM = DG*2^8 if shift_factor=8 ...

      A ->  (DA,    B)      -> (1/(2^SF*DX*DX), B)

  */

  def toDM(in: BBBB): B = { ((in + (scala.math.pow(2.0,shift_factor-1).to[BBBB])) >> shift_factor).to[B] }
  def toDG(in: B): BBBB = { in.to[BBBB] << shift_factor }

  def FloatToLP[T:Num](in: Float, delta: Float, precision: scala.Int): T = {
    val exact = in / delta
    
    if (exact < -scala.math.pow(2,(precision-1))) -(scala.math.pow(2,(precision-1))).to[T]
    else if (exact > scala.math.pow(2, (precision-1)-1)) scala.math.pow(2, (precision-1)-1).to[T]
    else (exact + 0.5).to[T]
  }

  def LPToFloat[T:Num](in: T, delta: Float, precision: scala.Int): Float = {delta * in.to[Float]}


  def getValue(table: Matrix[String], key: String): Float = {
    val sum = Array.tabulate(table.rows){i => if (table(i,0) == key) table(i,1).to[Float] else 0.to[Float]}.reduce{_+_}
    if (sum == 0.to[Float]) println("WARN: Possibly could not find " + key)
    sum
  }

  def main(args: Array[String]): Void = {
    val config = loadCSV2D[String](s"/home/mattfel/ppoppfiles/lp_svrg.config", ",", "\n")
    printMatrix(config, "Config")

    val epochs = getValue(config, "epochs").to[Int]
    val len_epoch = getValue(config, "len_epoch").to[Int]
    val points = getValue(config, "points").to[Int] // Total Points
    val dm = getValue(config, "dm").to[Float]
    val dx = getValue(config, "dx").to[Float]
    val alpha1 = getValue(config, "alpha1").to[Float] // Step size
    val alpha2 = getValue(config, "alpha2").to[Float]
    val bump_epoch = getValue(config, "bump_epoch").to[Int]
    val track = getValue(config, "track").to[Int] // Track cost vs time
    val threshold = getValue(config, "threshold").to[Float] // Cost at which to quit (only quits if track is on)
    val variance = getValue(config, "variance").to[Int] // numerator for noise
    val warmup = getValue(config, "warmup").to[Int]

    val da = 1/(scala.math.pow(2.0,shift_factor).to[Float]*dx*dx)
    val maxX = 6
    val D = 128

    val noise_num = variance
    val noise_denom = 10
    
    // Generate some test data
    val sX = (0::points, 0::D){(i,j) => (((i+j)%6).to[Float] - (maxX/3).to[Float])}
    val W_gold = Array.tabulate(D) { i => (1.5.to[Float] / 2)}
    val Y_noise = Array.tabulate(points) { i => ((noise_num/2).to[Float] - (noise_num.to[Float]/2)) / noise_denom.to[Float] }
    val sY = Array.tabulate(points) { i => Array.tabulate(D){j => (W_gold(j) * sX(i,j))}.reduce{_+_} + Y_noise(i) }

    // Convert data to LP
    val W_bits = Array.tabulate(D) { i => FloatToLP[B](W_gold(i), dm, 8)}

    val Y_noise_bits = Array.tabulate(points) {i => FloatToLP[BB](Y_noise(i), dx*dm, 16)}
    val X_bits = (0::points, 0::D){(i,j) => FloatToLP[B](sX(i,j), dx, 8)}
    val Y_bits = Array.tabulate(points){ i => FloatToLP[BB](sY(i), dx*dm, 16)}
    val alpha1_bits = FloatToLP[B](alpha1, da, 8)
    val alpha2_bits = FloatToLP[B](alpha2, da, 8)

    // Debug
    val W_recompute = Array.tabulate(D) { i => LPToFloat[B](W_bits(i), dm, 8)}
    printArray(W_gold, "W_gold")
    printArray(W_bits, "W_bits")
    printArray(W_recompute, "W_gold Reconstructed")
    println("dm = " + dm)
    println("dx = " + dx)
    println("da = " + da)
    println("dm*dx = " + dm*dx)
    println("dm*dx*dx*da = " + dm*dx*dx*da)
    println("Alpha1 bits: " + alpha1_bits)
    println("Alpha2 bits: " + alpha2_bits)
    if (points < 10) printMatrix(X_bits, "X Data")
    else {
      printMatrix((0::10, 0::D){(i,j) => X_bits(i,j)}, "X Data")
      println("... Skipped last " + {points-10} + " rows")
    }
    printArray(Y_bits, "Y_bits")
    printArray(W_bits, "W_bits")
    printArray(Y_noise_bits, "Y_noise_bits")


    val E = ArgIn[Int]
    val N = ArgIn[Int]
    val T = ArgIn[Int]
    val DM = HostIO[Float]
    val DX = HostIO[Float]
    val DMDX = HostIO[Float]
    val DG = HostIO[Float]
    val DA = HostIO[Float]
    val A1 = ArgIn[B]
    val A2 = ArgIn[B]
    val BUMP_EPOCH = ArgIn[Int]
    val TRACK = ArgIn[Int]
    val THRESH = ArgIn[BB]
    val E_ACTUAL = HostIO[Int]
    val WARMUP = ArgIn[Int]

    setArg(E, epochs)
    setArg(N, points)
    setArg(T, len_epoch)
    setArg(A1, alpha1_bits)
    setArg(A2, alpha2_bits)
    setArg(DM,   dm)
    setArg(DX,   dx)
    setArg(DA,   da)
    setArg(DMDX, dm*dx)
    setArg(DG,   dm*dx*dx*da)
    setArg(BUMP_EPOCH, bump_epoch)
    setArg(TRACK, track)
    setArg(THRESH, FloatToLP[BB](threshold, dm*dm, 16))
    setArg(E_ACTUAL, epochs*len_epoch-1)
    setArg(WARMUP, warmup-1)

    val x = DRAM[B](N, D)
    val y = DRAM[BB](N)
    val w = DRAM[B](D)
    val cost = DRAM[BB](max_history)
    val true_w = DRAM[B](D)

    setMem(x, X_bits)
    setMem(y, Y_bits)
    setMem(w, Array.fill(D)(0.to[B]))
    setMem(cost, Array.fill(max_history)(0.to[BB]))
    setMem(true_w, W_bits)

    Accel {
      // Create model and gradient memories
      val w_k = SRAM[B](D) // DM
      val g_k = SRAM[BBBB](D) // DG
      val y_cache = SRAM[BB](tileSize) // DM*DX
      val y_cache_base = Reg[Int](-1) 
      val true_w_sram = SRAM[B](D)
      val cost_sram = SRAM[BB](max_history)
      val w_k_t = SRAM[B](D) // DM

      if (TRACK.value == 1) true_w_sram load true_w(0 :: D)
      w_k load w(0::D par loadPar)
      w_k_t load w(0::D par loadPar)

      // Outer loop (epochs)
      Sequential.Foreach(E by 1 par PX) { e =>
        // Choose correct step for this epoch
        val A = if (e < BUMP_EPOCH) A1.value else A2.value

        if (e > WARMUP.value) {
          // Do full update over all points to get g_k and w_k (outer loop)
          MemReduce(g_k par P3)(N by tileSize par P4){i => 
            val y_tile = SRAM[BB](tileSize) // DM*DX
            y_tile load y(i::i + tileSize par loadPar)
            val g_k_partial = SRAM[BBBB](D)    // DG
            // Full update tile (inner loop)
            MemReduce(g_k_partial par P5)(tileSize by 1 par P6){ii =>
              val x_tile = SRAM[B](D) // DX
              x_tile load x(i+ii, 0::D par loadPar)            
              val g_k_local = SRAM[BBBB](D)  // DG
              val y_hat = Reg[BB](0.to[BB]) // DM*DX
              Reduce(y_hat)(D by 1 par P12){j => w_k(j).to[BB] *! x_tile(j).to[BB]}{_+!_} // DM*DX
              val y_err = y_hat.value -! y_tile(ii) // DM*DX
              Foreach(D by 1 par P7){j => 
                g_k_local(j) =  y_err.to[BBBB] *! x_tile(j).to[BBBB] /& N.value.to[BBBB]
              } // DG
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
          val i = (e*T+t) % N

          // Get y for this point
          val y_point = Reg[BB](0) // DM*DX
          if (i - y_cache_base >= 0 && i - y_cache_base < tileSize && y_cache_base >= 0) {
            y_point := y_cache(i-y_cache_base)
          } else {
            y_cache_base := i - (i % tileSize)
            y_cache load y(y_cache_base::y_cache_base + tileSize par loadPar)
            y_point := y_cache(i % tileSize)
          }

          // Get x for this point
          val x_point = SRAM[B](D) // DX
          x_point load x(i, 0::D par loadPar)

          // Compute gradient against w_k_t
          val y_hat_t = Reg[BB](0.to[BB])
          Reduce(y_hat_t)(D by 1 par P13){j => (w_k_t(j).to[BB] *&! x_point(j).to[BB])}{_+!_}
          val y_err_t = y_hat_t.value -! y_point

          // Compute gradient against w_k
          val y_hat_k = Reg[BB](0.to[BB])
          val y_err_k = Reg[BB](0.to[BB])
          if (e > WARMUP.value) {
            Reduce(y_hat_k)(D by 1 par P14){j => (w_k(j).to[BB] *&! x_point(j).to[BB])}{_+!_}
            y_err_k := y_hat_k.value -! y_point            
          }

          // Update w_k_t with reduced variance update
          Foreach(D by 1 par P10){i => w_k_t(i) = toDM(toDG(w_k_t(i)) -! 
                                                    A.to[BBBB] *! (
                                                      (y_err_t.to[BBBB] *! x_point(i).to[BBBB]) +! 
                                                      mux(e > WARMUP.value, -(y_err_k.value.to[BBBB] *&! x_point(i).to[BBBB]) +! g_k(i), 0.to[BBBB])
                                                    )
                                                  )
                                                }

          if (debug) { 
            println("*** Step " + {t + e*T} + ": ")
            println("y_err_t = " + y_err_t + " ( = " + y_hat_t.value + " - " + y_point + "), A = " + A.to[BBBB])
            Foreach(5 by 1) { i => 
              val part1 = y_err_t.to[BBBB] *! x_point(i).to[BBBB]
              val part2 = mux(e > WARMUP.value, -(y_err_k.value.to[BBBB] *&! x_point(i).to[BBBB]), 0.to[BBBB])
              val part3 = mux(e > WARMUP.value, g_k(i), 0.to[BBBB])
              val gradientLP = A.to[BBBB] *! (part1 +! part2 +! part3)

              print(" " + gradientLP + " ( " + part1 + " +! " + part2 + " +! " + part3 + ")")
            }
            println("\nWeights: ")
            Foreach(5 by 1) { i => 
              print(" " + w_k_t(i))
            }
            println("\n")
          }


          if (TRACK.value == 1) {
            val current_cost = Reduce(Reg[BB](0))(D by 1){i => pow((w_k_t(i) - true_w_sram(i)).to[BB], 2)}{_+!_}
            cost_sram(min((max_history-1).to[Int], e*T+t)) = current_cost
            // if (current_cost < THRESH) {
            //   E_ACTUAL := min(e*T+t, (max_history-1).to[Int])
            //   cost(0 :: min((max_history-1).to[Int], getArg(E_ACTUAL))) store cost_sram
            //   w(0 :: D par storePar) store w_k_t

            //   exit()
            // }
          }

        }
        // Copy w_k_t to w_k
        Foreach(D by 1 par P11){i => w_k(i) = w_k_t(i)}
      }

      // Store back values
      if (TRACK.value == 1) cost(0 :: min((max_history-1).to[Int], getArg(E_ACTUAL))) store cost_sram 
      w(0 :: D par storePar) store w_k
    }

    
    val w_result = getMem(w)

    val w_result_fullprecision = Array.tabulate(D){i => LPToFloat[B](w_result(i), dm, 8)}
    val cartesian_dist = W_gold.zip(w_result_fullprecision) { case (a, b) => pow(a - b, 2) }.reduce{_+_}
    val cksum =  cartesian_dist < threshold
    printArray(w_result_fullprecision, "result: ")
    printArray(W_gold, "gold: ")
    println("Finished in " + getArg(E_ACTUAL) + " epochs (out of " + {epochs*len_epoch} + ")")

    if (track == 1) {
      val cost_result = getMem(cost)
      val hist_len = min(getArg(E_ACTUAL), max_history.to[Int])
      val relevent_history_LP = Array.tabulate(hist_len){i => cost_result(i)}
      val relevent_history = Array.tabulate(hist_len){i => LPToFloat[BB](cost_result(i), dm*dm, 16)}
      printMatrix(relevent_history.reshape(hist_len, 1), "Cost vs iter:")
      printMatrix(relevent_history_LP.reshape(hist_len, 1), "Cost vs iter (LP):")
      val got_lp = relevent_history_LP.reshape(hist_len, 1)
      val gold_lp = Array[BB](27582,26335,25117,23981,22793,21634,20582,19507,18459,17489,16477,15492,15047,14606,14169,13764,13365,12951,12951,12951,12934,12934,12953,12953,12953,12953,12936,12936,12955,12955,12955,12955,12955,12955,12955,12955,12938,12938,12957,12957,12957,12957,12940,12940,12959,12959,12959,12959,12959,12959,12959,12959,12942,12942,12961,12961,12961,12961,12944,12944,12963,12963,12963,12963,12963,12963,12963,12963,12946,12946,12965,12965,12965,12965,12948,12948,12967,12967,12967,12967,12967,12967,12967,12967,12950,12950,12969,12969,12969,12969,12952,12952,12971,12971,12971,12971,12971,12971,12971,12971,12954,12954,12973,12973,12973,12973,12956,12956,12975,12975,12975,12975,12975,12975,12975,12975,12958,12958,12977,12977,12977,12977,12960,12960,12979,12979,12979)
      println(s"does LP result match expected? ${got_lp === gold_lp}")
    }

    println("Cartesian Distance From W_gold: " + cartesian_dist + " <? " + {threshold.to[Float]})

    println("PASS: " + cksum + " (LP_SVRG)")
    // assert(cksum)
  }
}

@spatial class LPSVRG_2 extends SpatialTest {  // Test Args: 


  val tileSize = 64 (16 -> 128)

  val loadPar = 16
  val storePar = 16
  val max_history = 512
  val P3 =  1 (1 -> 8)
  val P4 =  1 (1 -> 8)
  val P5 =  1 (1 -> 8)
  val P6 =  1 (1 -> 8)
  val P7 =  1 (1 -> 8)
  val P8 =  1 (1 -> 8)
  val P9 =  1 (1 -> 8)
  val P10 = 1 (1 -> 8)
  val P11 = 1 (1 -> 8)
  val P12 = 1 (1 -> 8)
  val P13 = 1 (1 -> 8)
  val P14 = 1 (1 -> 8)
  val PX = 1

  val shift_factor = 16

  type B = Int8
  type BB = Int16
  type BBBB = Int32

  val debug = true
  /*

    Basic type relationships:
      X ->  (DX,    B)
      W ->  (DM,    B)
      Y ->  (DM*DX, BB)
      GR -> (DG,    BBBB)   -> (DM*DX*DX*DA, BBBB)

      ... Choose DA so that DG is off by a power of 2 from DM.  I.E.- DM = DG*2^8 if shift_factor=8 ...

      A ->  (DA,    B)      -> (1/(2^SF*DX*DX), B)

  */

  def toDM(in: BBBB): B = { ((in + (scala.math.pow(2.0,shift_factor-1).to[BBBB])) >> shift_factor).to[B] }
  def toDG(in: B): BBBB = { in.to[BBBB] << shift_factor }

  def FloatToLP[T:Num](in: Float, delta: Float, precision: scala.Int): T = {
    val exact = in / delta
    
    if (exact < -scala.math.pow(2,(precision-1))) -(scala.math.pow(2,(precision-1))).to[T]
    else if (exact > scala.math.pow(2, (precision-1)-1)) scala.math.pow(2, (precision-1)-1).to[T]
    else (exact + 0.5).to[T]
  }

  def LPToFloat[T:Num](in: T, delta: Float, precision: scala.Int): Float = {delta * in.to[Float]}


  def getValue(table: Matrix[String], key: String): Float = {
    val sum = Array.tabulate(table.rows){i => if (table(i,0) == key) table(i,1).to[Float] else 0.to[Float]}.reduce{_+_}
    if (sum == 0.to[Float]) println("WARN: Possibly could not find " + key)
    sum
  }

  def main(args: Array[String]): Void = {
    val config = loadCSV2D[String](s"/home/mattfel/ppoppfiles/lp_svrg.config", ",", "\n")
    printMatrix(config, "Config")

    val epochs = getValue(config, "epochs").to[Int]
    val len_epoch = getValue(config, "len_epoch").to[Int]
    val points = getValue(config, "points").to[Int] // Total Points
    val dm = getValue(config, "dm").to[Float]
    val dx = getValue(config, "dx").to[Float]
    val alpha1 = getValue(config, "alpha1").to[Float] // Step size
    val alpha2 = getValue(config, "alpha2").to[Float]
    val bump_epoch = getValue(config, "bump_epoch").to[Int]
    val track = getValue(config, "track").to[Int] // Track cost vs time
    val threshold = getValue(config, "threshold").to[Float] // Cost at which to quit (only quits if track is on)
    val variance = getValue(config, "variance").to[Int] // numerator for noise
    val warmup = getValue(config, "warmup").to[Int]

    val da = 1/(scala.math.pow(2.0,shift_factor).to[Float]*dx*dx)
    val maxX = 6
    val D = 128

    val noise_num = variance
    val noise_denom = 10
    
    // Generate some test data
    val sX = (0::points, 0::D){(i,j) => (((i+j)%6).to[Float] - (maxX/3).to[Float])}
    val W_gold = Array.tabulate(D) { i => (1.5.to[Float] / 2)}
    val Y_noise = Array.tabulate(points) { i => ((noise_num/2).to[Float] - (noise_num.to[Float]/2)) / noise_denom.to[Float] }
    val sY = Array.tabulate(points) { i => Array.tabulate(D){j => (W_gold(j) * sX(i,j))}.reduce{_+_} + Y_noise(i) }

    // Convert data to LP
    val W_bits = Array.tabulate(D) { i => FloatToLP[B](W_gold(i), dm, 8)}

    val Y_noise_bits = Array.tabulate(points) {i => FloatToLP[BB](Y_noise(i), dx*dm, 16)}
    val X_bits = (0::points, 0::D){(i,j) => FloatToLP[B](sX(i,j), dx, 8)}
    val Y_bits = Array.tabulate(points){ i => FloatToLP[BB](sY(i), dx*dm, 16)}
    val alpha1_bits = FloatToLP[B](alpha1, da, 8)
    val alpha2_bits = FloatToLP[B](alpha2, da, 8)

    // Debug
    val W_recompute = Array.tabulate(D) { i => LPToFloat[B](W_bits(i), dm, 8)}
    printArray(W_gold, "W_gold")
    printArray(W_bits, "W_bits")
    printArray(W_recompute, "W_gold Reconstructed")
    println("dm = " + dm)
    println("dx = " + dx)
    println("da = " + da)
    println("dm*dx = " + dm*dx)
    println("dm*dx*dx*da = " + dm*dx*dx*da)
    println("Alpha1 bits: " + alpha1_bits)
    println("Alpha2 bits: " + alpha2_bits)
    if (points < 10) printMatrix(X_bits, "X Data")
    else {
      printMatrix((0::10, 0::D){(i,j) => X_bits(i,j)}, "X Data")
      println("... Skipped last " + {points-10} + " rows")
    }
    printArray(Y_bits, "Y_bits")
    printArray(W_bits, "W_bits")
    printArray(Y_noise_bits, "Y_noise_bits")


    val E = ArgIn[Int]
    val N = ArgIn[Int]
    val T = ArgIn[Int]
    val DM = HostIO[Float]
    val DX = HostIO[Float]
    val DMDX = HostIO[Float]
    val DG = HostIO[Float]
    val DA = HostIO[Float]
    val A1 = ArgIn[B]
    val A2 = ArgIn[B]
    val BUMP_EPOCH = ArgIn[Int]
    val TRACK = ArgIn[Int]
    val THRESH = ArgIn[BB]
    val E_ACTUAL = HostIO[Int]
    val WARMUP = ArgIn[Int]

    setArg(E, epochs)
    setArg(N, points)
    setArg(T, len_epoch)
    setArg(A1, alpha1_bits)
    setArg(A2, alpha2_bits)
    setArg(DM,   dm)
    setArg(DX,   dx)
    setArg(DA,   da)
    setArg(DMDX, dm*dx)
    setArg(DG,   dm*dx*dx*da)
    setArg(BUMP_EPOCH, bump_epoch)
    setArg(TRACK, track)
    setArg(THRESH, FloatToLP[BB](threshold, dm*dm, 16))
    setArg(E_ACTUAL, epochs*len_epoch-1)
    setArg(WARMUP, warmup-1)

    val x = DRAM[B](N, D)
    val y = DRAM[BB](N)
    val w = DRAM[B](D)
    val cost = DRAM[BB](max_history)
    val true_w = DRAM[B](D)

    setMem(x, X_bits)
    setMem(y, Y_bits)
    setMem(w, Array.fill(D)(0.to[B]))
    setMem(cost, Array.fill(max_history)(0.to[BB]))
    setMem(true_w, W_bits)

    Accel {
      // Create model and gradient memories
      val w_k = SRAM[B](D) // DM
      val g_k = SRAM[BBBB](D).buffer // DG
      val y_cache = SRAM[BB](tileSize) // DM*DX
      val y_cache_base = Reg[Int](-1) 
      val true_w_sram = SRAM[B](D)
      val cost_sram = SRAM[BB](max_history)
      val w_k_t = SRAM[B](D) // DM

      if (TRACK.value == 1) true_w_sram load true_w(0 :: D)
      w_k load w(0::D par loadPar)
      w_k_t load w(0::D par loadPar)

      // Outer loop (epochs)
      Sequential.Foreach(E by 1 par PX) { e =>
        // Choose correct step for this epoch
        val A = if (e < BUMP_EPOCH) A1.value else A2.value

        if (e > WARMUP.value) {
          // Do full update over all points to get g_k and w_k (outer loop)
          MemReduce(g_k par P3)(N by 1 par P4){i => 
            val y_tile = SRAM[BB](tileSize).nonbuffer // DM*DX
            val ii = i % tileSize
            if (ii == 0) y_tile load y(i::i + tileSize par loadPar)
            val x_tile = SRAM[B](D) // DX
            x_tile load x(i+ii, 0::D par loadPar)            
            val g_k_local = SRAM[BBBB](D)  // DG
            val y_hat = Reg[BB](0.to[BB]) // DM*DX
            Reduce(y_hat)(D by 1 par P12){j => w_k(j).to[BB] *! x_tile(j).to[BB]}{_+!_} // DM*DX
            val y_err = y_hat.value -! y_tile(ii) // DM*DX
            Foreach(D by 1 par P7){j => 
              g_k_local(j) =  y_err.to[BBBB] *! x_tile(j).to[BBBB] /& N.value.to[BBBB]
            } // DG
            g_k_local
          }{(a,b) => a +! b}

          // Accumulation here may not be necessary
          // // Accumulate g_k into w_k
          // MemFold(w_k par P8)(1 by 1){_ => g_k}{_+!_}
          ()
        }

        // Run len_epoch number of SGD points
        Sequential.Foreach(T by 1 par PX){t => 
          // Choose random point
          val i = (e*T+t) % N

          // Get y for this point
          val y_point = Reg[BB](0) // DM*DX
          if (i - y_cache_base >= 0 && i - y_cache_base < tileSize && y_cache_base >= 0) {
            y_point := y_cache(i-y_cache_base)
          } else {
            y_cache_base := i - (i % tileSize)
            y_cache load y(y_cache_base::y_cache_base + tileSize par loadPar)
            y_point := y_cache(i % tileSize)
          }

          // Get x for this point
          val x_point = SRAM[B](D) // DX
          x_point load x(i, 0::D par loadPar)

          // Compute gradient against w_k_t
          val y_hat_kt = Reg[Tup2[BB,BB]](pack(0.to[BB],0.to[BB]))
          Reduce(y_hat_kt)(D by 1 par P13){j => 
            val hat_k = w_k(j).to[BB] *&! x_point(j).to[BB]
            val hat_t = (w_k_t(j).to[BB] *&! x_point(j).to[BB])
            pack(hat_k, hat_t)
          }{case (a,b) => pack(a._1 +! b._1, a._2 +! b._2)}
          val y_err_t = y_hat_kt.value._2 -! y_point
          val y_err_k = mux(e > WARMUP.value, y_hat_kt.value._1 -! y_point, 0.to[BB])

          // Update w_k_t with reduced variance update
          Foreach(D by 1 par P10){i => w_k_t(i) = toDM(toDG(w_k_t(i)) -! 
                                                    A.to[BBBB] *! (
                                                      (y_err_t.to[BBBB] *! x_point(i).to[BBBB]) +! 
                                                      mux(e > WARMUP.value, -(y_err_k.to[BBBB] *&! x_point(i).to[BBBB]) +! g_k(i), 0.to[BBBB])
                                                    )
                                                  )
                                                }

          if (debug) { 
            println("*** Step " + {t + e*T} + ": ")
            println("y_err_t = " + y_err_t + " ( = " + y_hat_kt.value._2 + " - " + y_point + "), A = " + A.to[BBBB])
            Foreach(5 by 1) { i => 
              val part1 = y_err_t.to[BBBB] *! x_point(i).to[BBBB]
              val part2 = mux(e > WARMUP.value, -(y_err_k.to[BBBB] *&! x_point(i).to[BBBB]), 0.to[BBBB])
              val part3 = mux(e > WARMUP.value, g_k(i), 0.to[BBBB])
              val gradientLP = A.to[BBBB] *! (part1 +! part2 +! part3)

              print(" " + gradientLP + " ( " + part1 + " +! " + part2 + " +! " + part3 + ")")
            }
            println("\nWeights: ")
            Foreach(5 by 1) { i => 
              print(" " + w_k_t(i))
            }
            println("\n")
          }


          if (TRACK.value == 1) {
            val current_cost = Reduce(Reg[BB](0))(D by 1){i => pow((w_k_t(i) - true_w_sram(i)).to[BB], 2)}{_+!_}
            cost_sram(min((max_history-1).to[Int], e*T+t)) = current_cost
            // if (current_cost < THRESH) {
            //   E_ACTUAL := min(e*T+t, (max_history-1).to[Int])
            //   cost(0 :: min((max_history-1).to[Int], getArg(E_ACTUAL))) store cost_sram
            //   w(0 :: D par storePar) store w_k_t

            //   exit()
            // }
          }
          // Copy w_k_t to w_k
          if (t == T-1) Foreach(D by 1 par P11){i => w_k(i) = w_k_t(i)}
        }

      }

      // Store back values
      if (TRACK.value == 1) cost(0 :: min((max_history-1).to[Int], getArg(E_ACTUAL))) store cost_sram 
      w(0 :: D par storePar) store w_k
    }

    
    val w_result = getMem(w)

    val w_result_fullprecision = Array.tabulate(D){i => LPToFloat[B](w_result(i), dm, 8)}
    val cartesian_dist = W_gold.zip(w_result_fullprecision) { case (a, b) => pow(a - b, 2) }.reduce{_+_}
    val cksum =  cartesian_dist < threshold
    printArray(w_result_fullprecision, "result: ")
    printArray(W_gold, "gold: ")
    println("Finished in " + getArg(E_ACTUAL) + " epochs (out of " + {epochs*len_epoch} + ")")

    if (track == 1) {
      val cost_result = getMem(cost)
      val hist_len = min(getArg(E_ACTUAL), max_history.to[Int])
      val relevent_history_LP = Array.tabulate(hist_len){i => cost_result(i)}
      val relevent_history = Array.tabulate(hist_len){i => LPToFloat[BB](cost_result(i), dm*dm, 16)}
      printMatrix(relevent_history.reshape(hist_len, 1), "Cost vs iter:")
      printMatrix(relevent_history_LP.reshape(hist_len, 1), "Cost vs iter (LP):")
      val got_lp = relevent_history_LP.reshape(hist_len, 1)
      val gold_lp = Array[BB](27582,26335,25117,23981,22793,21634,20582,19507,18459,17489,16477,15492,15047,14606,14169,13764,13365,12951,12951,12951,12934,12934,12953,12953,12953,12953,12936,12936,12955,12955,12955,12955,12955,12955,12955,12955,12938,12938,12957,12957,12957,12957,12940,12940,12959,12959,12959,12959,12959,12959,12959,12959,12942,12942,12961,12961,12961,12961,12944,12944,12963,12963,12963,12963,12963,12963,12963,12963,12946,12946,12965,12965,12965,12965,12948,12948,12967,12967,12967,12967,12967,12967,12967,12967,12950,12950,12969,12969,12969,12969,12952,12952,12971,12971,12971,12971,12971,12971,12971,12971,12954,12954,12973,12973,12973,12973,12956,12956,12975,12975,12975,12975,12975,12975,12975,12975,12958,12958,12977,12977,12977,12977,12960,12960,12979,12979,12979)
      println(s"does LP result match expected? ${got_lp === gold_lp}")
    }

    println("Cartesian Distance From W_gold: " + cartesian_dist + " <? " + {threshold.to[Float]})

    println("PASS: " + cksum + " (LP_SVRG)")
    // assert(cksum)
  }
}
