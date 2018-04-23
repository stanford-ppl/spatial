package spatial.tests.apps

import spatial.dsl._

@test class SVRG extends SpatialTest {
  override def runtimeArgs: Args = "25 30 256 0.0001 0.0009 10"

  type TM = FixPt[TRUE, _8, _24]
  type TX = FixPt[TRUE, _8, _8]
  val margin = 2 // Maximum distance between gold weights and computed weights to consider the app "passing"

  val tileSize = 16 (16 -> 128)

  val loadPar = 1
  val storePar = 1
  val P1 = 2 (1 -> 8)
  val P2 = 2 (1 -> 8)
  val P3 = 2 (1 -> 8)
  val P4 = 2 (1 -> 8)
  val P5 = 2 (1 -> 8)
  val P6 = 2 (1 -> 8)
  val P7 = 2 (1 -> 8)
  val P8 = 2 (1 -> 8)
  val P9 = 2 (1 -> 8)
  val P10 = 2 (1 -> 8)
  val P11 = 2 (1 -> 8)
  val P12 = 2 (1 -> 8)
  val PX = 1


  def main(args: Array[String]): Unit = {
    val epochs = args(0).to[Int] // Epochs
    val len_epoch = args(1).to[Int] // Epoch Length
    val points = args(2).to[Int] // Total Points
    val alpha1 = args(3).to[TM] // Step size
    val alpha2 = args(4).to[TM] // Step size
    val D = 16
    val bump_epoch = args(5).to[Int]

    val noise_num = 2
    val noise_denom = 10
    // Generate some test data
    val sX = (0::points, 0::D){(i,j) => random[TX](3.to[TX]) + 1.to[TX]}
    val W_gold = Array.tabulate(D) { i => random[TM](3.to[TM]) / 2.to[TM]}
    val sY = Array.tabulate(points) { i => (random[TX](noise_num.to[TX]) / noise_denom - noise_num/2) + Array.tabulate(D){j => W_gold(j) * sX(i,j).to[TM]}.reduce{_+_}.to[TX] }

    val E = ArgIn[Int]
    val N = ArgIn[Int]
    val T = ArgIn[Int]
    val BUMP_EPOCH = ArgIn[Int]
    val A1 = ArgIn[TM]
    val A2 = ArgIn[TM]

    setArg(E, epochs)
    setArg(N, points)
    setArg(T, len_epoch)
    setArg(A1, alpha1)
    setArg(A2, alpha2)
    setArg(BUMP_EPOCH, bump_epoch)

    val x = DRAM[TX](N, D)
    val y = DRAM[TX](N)
    val result = DRAM[TM](D)

    printMatrix(sX, "X Data")
    printArray(sY, "Y Data")
    printArray(W_gold, "W Gold")
    setMem(x, sX)
    setMem(y, sY)


    Accel {
      // Create model and gradient memories
      val w_k = SRAM[TM](D)
      val g_k = SRAM[TM](D)
      val y_cache = SRAM[TX](tileSize)
      val y_cache_base = Reg[Int](0)

      Foreach(D by 1 par P2){ i => w_k(i) = 0.to[TM] }
      y_cache load y(0::tileSize par loadPar)

      // Outer loop (epochs)
      Sequential.Foreach(E by 1 par PX) { e =>
        // Choose correct step for this epoch
        val A = mux(e < BUMP_EPOCH, A1.value, A2.value)

        // Do full update over all points to get g_k and w_k (outer loop)
        MemReduce(g_k par P3)(N by tileSize par P4){i =>
          val y_tile = SRAM[TX](tileSize)
          val x_tile = SRAM[TX](tileSize,D)
          y_tile load y(i::i + tileSize par loadPar)
          x_tile load x(i::i + tileSize, 0::D par loadPar)
          val g_k_partial = SRAM[TM](D)
          // Full update tile (inner loop)
          MemReduce(g_k_partial par P5)(tileSize by 1 par P6){ii =>
            val g_k_local = SRAM[TM](D)
            val y_err = Reduce(Reg[TX](0.to[TX]))(D by 1 par P12){j => (w_k(j) *&! x_tile(ii, j).to[TM]).to[TX]}{_+!_} -! y_tile(ii)
            Foreach(D by 1 par P7){j => g_k_local(j) =  -A *&! y_err.to[TM] *&! x_tile(ii, j).to[TM]}
            g_k_local
          }{_+!_}
        }{(a,b) => a +! b/tileSize.to[TM]}

        // Accumulation here may not be necessary
        // // Accumulate g_k into w_k
        // MemFold(w_k par P8)(1 by 1){_ => g_k}{_+!_}

        val w_k_t = SRAM[TM](D)

        // Copy w_k to w_k_t
        Foreach(D by 1 par P9){i => w_k_t(i) = w_k(i)}

        // Run len_epoch number of SGD points
        Foreach(T by 1 par PX){t =>
          // Choose random point
          val i = random[Int](1024) % N

          // Get y for this point
          val y_point = Reg[TX](0)
          if (i - y_cache_base >= 0 && i - y_cache_base < tileSize) {
            y_point := y_cache(i-y_cache_base)
          } else {
            y_cache_base := i - (i % tileSize)
            y_cache load y(y_cache_base::y_cache_base + tileSize par loadPar)
            y_point := y_cache(i % tileSize)
          }

          // Get x for this point
          val x_point = SRAM[TX](D)
          x_point load x(i, 0::D par loadPar)

          // Compute gradient against w_k_t
          val y_err_t = Reduce(Reg[TX](0.to[TX]))(D by 1){j => (w_k_t(j) *&! x_point(j).to[TM]).to[TX]}{_+!_} -! y_point

          // Compute gradient against w_k
          val y_err_k = Reduce(Reg[TX](0.to[TX]))(D by 1){j => (w_k(j) *&! x_point(j).to[TM]).to[TX]}{_+!_} -! y_point

          // Update w_k_t with reduced variance update
          Foreach(D by 1 par P10){i => w_k_t(i) = w_k_t(i) -! (A *&! ((y_err_t *&! x_point(i)).to[TM] +! (y_err_k *&! x_point(i)).to[TM] -! g_k(i)))}

        }
        // Copy w_k_t to w_k
        Foreach(D by 1 par P11){i => w_k(i) = w_k_t(i)}
      }

      // Store back values
      result(0 :: D par storePar) store w_k
    }


    val w_result = getMem(result)
    val cartesian_dist = W_gold.zip(w_result) { case (a, b) => (a - b) * (a - b) }.reduce{_+_}

    val cksum = cartesian_dist < margin
    printArray(w_result, "result: ")
    printArray(W_gold, "gold: ")
    println("Cartesian Distance From W_gold: " + cartesian_dist.to[TM] + " <? " + {margin.to[TM]})

    println("PASS: " + cksum + " (SVRG)")
    assert(cksum)
  }
}
