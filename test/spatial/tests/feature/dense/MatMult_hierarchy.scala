package spatial.tests.feature.dense

import spatial.dsl._

/*

  Sketch of this GEMM:


    ###########
    # LAYER 1 #
    ###########
                                              N
                          (k) →        ___________________
                                      |                   |
                                   kc |                   |
                                      |     tileB         |
                                      |___________________|
                               P      |                   |
                                      |                   |
                                      |                   |
                                      |                   |
                                      |                   |
          (k)                         |___________________|
          ↓                        x
                      P
          ___kc_________________       ___________________
         |         |            |     |                   |
         |         |            |     |                   |
         |         |            |     |                   |
  M      |         |            |     |                   |
         |         |            |     |                   |
         |         |            |     |                   |
         |         |            |     |                   |
         | tileA   |            |     | tileC             |
         |_________|____________|     |___________________|




    ###########
    # LAYER 2 #
    ##################
        # outer pipe #
        ##############                              N
                                       ___________________
                                      |                   |
                                    kc|                   |
                                      |     tileB         |
                                      |___________________|
                                  P   .                   .
                                      .                   .
                                      .                   .
                                      .                   .
                                      .                   .
                                      .....................
                               x
(local_m)
     ↳   ___kc____..............       ___________________
        |         |            .      |                   |
     mc | tileA_ip|            .      |                   |
        |         |            .      |                   |
  M     |_________|            .      |                   |
        |         |            .      |                   |
        |         |            .      |                   |
        |         |            .      |                   |
        |         |            .      | tileC             |
        |_________|.............      |___________________|


    ###########
    # LAYER 2 #
    ##################
        # inner pipe #
        ##############
                                  (local_n)
                                      ↓
                                       _nr _______________
                                      |   |               |
                                    kc|tileB_pj           |
                                      |   |               |
                                      |___|_______________|
                                      .                   .
                                      .                   .
                                      .                   .
                                      .                   .
                                      .                   .
                                      .....................
                        x
                                 (local_m,local_n)
         ___kc____ ............       ↓___________________
        |         |            .     ↳|   |               |
     mc | tileA_ip|            .      |tileC_acc          |
        |         |            .      |   |               |
  M     |_________|            .      |___|               |
        .         .            .      |                   |
        .         .            .      |                   |
        .         .            .      |                   |
        .         .            .      |                   |
        ........................      |___________________|


    ###########
    # LAYER 3 #
    ###########


                                             (compute_n)
                                     ↓ ↓
                                      _nr ................
                        (compute_k) →|o o|               .
                                   kc|   |               .
                                     |   |               .
                                     |___|................
                                     .                   .
                                     .                   .
    (compute_m)                      .                   .
    |                                .                   .
    | (accum_m)                      .                   .
    |  |                             .....................
    |  | (compute_k)           x
    |  | ↓
    |  ↳ ____kc___ ............      ___ ................
    ↳   |o        |            .    |o o|               .
    ↳   |o        |            .    |o o|               .
      mc|o        |            .    |   |               .
  M     |o        |            .    |   |               .
        .‾‾‾‾‾‾‾‾‾.            .    .‾‾‾                .
        .         .            .    .                   .
        .         .            .    .                   .
        .         .            .    .                   .
        ........................    .....................


*/

@spatial class MatMult_hierarchy extends SpatialTest {
  type T = Int

  def main(args: Array[String]): Unit = {
    val m = 64
    val n = 64
    val p = 64

    val M = m
    val N = n
    val P = p

    val a = (0::m, 0::p){(i,j) => ((i*p + j)%8).to[T] }
    val b = (0::p, 0::n){(i,j) => ((i*n + j)%8).to[T] }
    val c = (0::m, 0::n){(i,j) => 0.to[T] }

    val A_dram = DRAM[T](M, P)
    val B_dram = DRAM[T](P, N)
    val C_dram = DRAM[T](M, N)

    val n_r        = 4 // rank-1 updated width/height
    // We could have two n_r but we assume we do square rank-1 updated

    val m_c        = 16 // blockram number of A matrix rows in each tile
    val k_c        = 16 // number of B rows/A Columns  in each tile
    val n_r_par    = 1

    setMem(A_dram, a)
    setMem(B_dram, b)
    setMem(C_dram, c)

    Accel {
      // **LAYER 1** ON-CHIP MEMORY
      val tileC = SRAM[T](M, N).buffer
      tileC load C_dram(0::M, 0::N par 16)

      Foreach(P by k_c) { k =>
        val tileA = SRAM[T](M, k_c)
        val tileB = SRAM[T](k_c, N)
        Parallel {
          tileA load A_dram(0::M, k::k+k_c par min(16,k_c))
          tileB load B_dram(k::k+k_c, 0::N par min(16,k_c))
        }

        // **LAYER 2** LOCAL-LEVEL STORE
        //      *outer*
        Foreach(M by m_c) { local_m =>
          val tileA_ip = SRAM[T](m_c, k_c)
          val tileB_pj = SRAM[T](k_c, n_r)

          // DATA MOVEMENT
          Foreach(m_c by 1, k_c by 1 par min(16,k_c)) { (copy_m, copy_k) => tileA_ip(copy_m, copy_k) = tileA(local_m + copy_m, copy_k) }

          // **LAYER 2** LOCAL-LEVEL STORE
          //      *inner*
          Foreach(N by n_r) { local_n =>
            // DATA MOVEMENT
            Foreach(k_c by 1, n_r by 1) { (copy_k, copy_n) => tileB_pj(copy_k, copy_n) = tileB(copy_k, local_n + copy_n) }
            // Loaded local store level of tileB_pj


            // **LAYER 3** ACCUMULATOR (REGISTER LEVEL)
            Foreach(m_c by n_r){ accum_m =>
              val tileC_acc = RegFile[T](n_r,n_r).coalesce.buffer
              /** NOT SURE WHY IT WAS WRITTEN IN THIS WAY WITH AN AMBIGUOUS BROADCAST READ ON tileC*/
              // // DATA MOVEMENT
              // Foreach(n_r by 1, n_r by 1 par n_r) { (copy_m, copy_n) =>
              //   // tileC_acc(copy_m, copy_n) = tileC(local_m + accum_m + copy_m, local_n + copy_n)
              //   tileC_acc(copy_m, copy_n) = tileC(local_m + accum_m + copy_m, local_n + copy_n)
              // }

              // MemFold(tileC_acc)(k_c by 1) { compute_k =>

              MemReduce(tileC_acc)(k_c by 1) { compute_k =>
                val tileC_partial = RegFile[T](n_r,n_r)
                Foreach(n_r by 1, n_r by 1 par n_r) { (compute_m, compute_n) =>
                  tileC_partial(compute_m, compute_n) = tileA_ip(compute_m, compute_k) * tileB_pj(compute_k, compute_n)
                }
                tileC_partial
              }{_+_}

              // DATA MOVEMENT
              Foreach(n_r by 1, n_r by 1 par n_r) { (copy_m, copy_n) =>
                tileC(local_m + accum_m + copy_m, local_n + copy_n) = tileC_acc(copy_m, copy_n) + tileC(local_m + accum_m + copy_m, local_n + copy_n)
              }
            }
          }
        }
      }
      C_dram(0::M, 0::N par 16) store tileC
    }
    val result = getMatrix(C_dram)

    val gold = (0::m, 0::n){(i,j) =>
      Array.tabulate(p){k => a(i,k) * a(k,j)}.reduce{_+_}
    }

    printMatrix(gold, "Gold:")
    printMatrix(result, "Result:")
    assert(gold == result)
  }
}

