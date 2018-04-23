package spatial.tests.feature.dense


import spatial.dsl._


@test class Kmeans_slow extends SpatialTest {
  override def runtimeArgs: Args = "3 64"

  type X = Int

  val numcents = 16
  val dim = 32
  val pts_per_ld = 1 // ???

  val element_max = 10
  val margin = (element_max * 0.2).to[X]

  val MAXK = numcents
  val MAXD = dim


  def kmeans[T:Num](points_in: Array[T], cent_inits: Array[T], numPoints: Int, numCents: Int, numDims: Int, it: Int): Array[T] = {
    bound(numPoints) = 960000
    bound(numCents) = MAXK
    bound(numDims) = MAXD

    val BN = pts_per_ld (96 -> 96 -> 9600)
    val BD = MAXD
    val par_load = 16
    val par_store = 16
    val PX = 1 (1 -> 1)
    val P0 = 4 (1 -> 2 -> dim)
    val P1 = 4 (1 -> 2 -> dim)
    val P2 = 4 (1 -> 2 -> dim)
    val P3 = 16 (1 -> 2 -> numcents)

    val iters = ArgIn[Int]
    val N     = ArgIn[Int]
    val K     = numCents //ArgIn[Int]
    val D     = numDims //ArgIn[Int]

    setArg(iters, it)
    setArg(N, numPoints)
    // setArg(K, numCents)
    // setArg(D, numDims)

    val points = DRAM[T](N, D)    // Input points
    val centroids = DRAM[T](numcents*dim) // Output centroids
    // val init_cents = DRAM[T](K,D) // Output centroids
    setMem(points, points_in)
    // setMem(init_cents, cent_inits)


    Accel {
      val cts = SRAM[T](MAXK, MAXD)
      val newCents = SRAM[T](MAXK,MAXD)

      // Load initial centroids (from points)
      cts load points(0::K, 0::D par par_load)

      // Initialize newCents
      // FPGA:
      Foreach(K by 1, D by 1 par P0) {(i,j) => newCents(i,j) = cts(i,j)}

      val DM1 = D - 1

      Sequential.Foreach(iters by 1){epoch =>
        // Flush centroid accumulator
        // FPGA:
        Foreach(K by 1, D par P0){(ct,d) =>
          newCents(ct,d) = 0.to[T]
        }

        // For each set of points
        Foreach(N by BN par PX){i =>
          val pts = SRAM[T](BN, BD)
          pts load points(i::i+BN, 0::BD par par_load)

          // For each point in this set
          MemFold(newCents par P0)(BN par PX){pt =>
            // Find the index of the closest centroid
            val accum = Reg[Tup2[Int,T]]( pack(0.to[Int], 100000.to[T]) )
            val minCent = Reduce(accum)(K par PX){ct =>
              val dist = Reg[T](0.to[T])
              Reduce(dist)(D par P2){d => (pts(pt,d) - cts(ct,d)) ** 2 }{_+_}
              pack(ct, dist.value)
            }{(a,b) =>
              mux(a._2 < b._2, a, b)
            }

            // Store this point to the set of accumulators
            val localCent = SRAM[T](MAXK,MAXD)
            Foreach(K by 1, D par P2){(ct,d) =>
              //val elem = mux(d == DM1, 1.to[T], pts(pt, d)) // fix for vanishing mux
              val elem = pts(pt,d)
              localCent(ct, d) = mux(ct == minCent.value._1, elem, 0.to[T])
            }
            localCent
          }{_+_} // Add the current point to the accumulators for this centroid
        }

        val centCount = SRAM[T](MAXK)
        Foreach(K by 1 par P3){ct => centCount(ct) = max(newCents(ct,DM1), 1.to[T]) }

        // Average each new centroid
        // val centsOut = SRAM[T](MAXK, MAXD)
        Foreach(K by 1, D par P0){(ct,d) =>
          cts(ct, d) = mux(centCount(ct) == 0.to[T], 0.to[T], newCents(ct,d) / centCount(ct)) //updateMux
        }
      }

      val flatCts = SRAM[T](MAXK * MAXD)
      Foreach(K by 1, D by 1 par PX) {(i,j) => // Parallelize when issue #159 is fixed
        flatCts(i.to[I32]*D+j.to[I32]) = cts(i,j)
      }
      // Store the centroids out
      centroids(0::K*D par par_store) store flatCts
    }

    getMem(centroids)
  }


  def main(args: Array[String]): Unit = {
    val iters = args(0).to[Int]
    val N = args(1).to[Int]
    val K = numcents //args(2).to[SInt];
    val D = dim //args(3).to[SInt];

    val pts = Array.tabulate(N){i => Array.tabulate(D){d => if (d == D-1) 1.to[X] else random[X](element_max) + i }}
    val cnts = Array.tabulate(K){i => Array.tabulate(D){d => if (d == D-1) 1.to[X] else random[X](element_max) + (i*N/K) }}

    val result = kmeans(pts.flatten, cnts.flatten, N, K, D, iters)

    val cts = Array.empty[Array[X]](K)
    for (k <- 0 until K) {
      cts(k) = Array.tabulate(D){i => pts(k).apply(i) }
    }
    val ii = Array.tabulate(K){i => i}

    for(epoch <- 0 until iters) {
      def dist[T:Num](p1: Array[T], p2: Array[T]) = p1.zip(p2){(a,b) => (a - b)**2 }.reduce(_+_)

      // Make weighted points
      val map = pts.groupByReduce{pt =>
        val dists = cts.map{ct => dist(ct, pt) }
        dists.zip(ii){(a,b) => pack(a,b) }.reduce{(a,b) => if (a._1 < b._1) a else b}._2  // minIndex
      }{pt => pt}{(x,y) => x.zip(y){_+_} }

      // Average
      for (k <- 0 until K) {
        if (!map.contains(k)) {
          cts(k) = Array.tabulate(D){d => 0.to[X]}
        } else {
          val wp = map(k)
          val n  = wp(D - 1)
          cts(k) = Array.tabulate(D){d => if (d == D-1) 1.to[X] else wp(d)/n }
        }
      }
    }

    val gold = cts.flatten

    println("\n\nOriginal Centers:")
    (0 until K).foreach{ i => printArray(cnts(i))}
    println("\n\nOriginal Points:")
    (0 until N).foreach{ i => printArray(pts(i))}
    println("\n\nCorrect Centers:")
    (0 until K).foreach{ i => printArray(cts(i))}
    println("\n\nFPGA Centers:")
    (0 until K).foreach { i =>
      val resrow = Array.tabulate(D){j => result(i*D + j)}
      printArray(resrow)
    }

    val cksum = result.zip(gold){ case (o, g) => (g < (o + margin)) && g > (o - margin)}.reduce{_&&_}

    println("PASS: " + cksum + " (Kmeans)")
    assert(cksum)
  }
}
