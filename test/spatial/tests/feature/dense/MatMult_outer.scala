package spatial.tests.feature.dense

import spatial.dsl._

@spatial class MatMult_outer extends SpatialTest {
  override def dseModelArgs: Args = "32 128 128"
  override def finalModelArgs: Args = "32 128 128"
  override def runtimeArgs: Args = "32 128 128"

  type X = FixPt[TRUE,_16,_16]

  def main(args: Array[String]): Unit = {
    // Get sizes for matrices from command line
    val m = args(0).to[Int]
    val n = args(1).to[Int]
    val p = args(2).to[Int]

    // Generate data for input matrices A and B, and initialize C
    val a = (0::m, 0::p){(i,j) => ((i + j * p) % 8).to[X] }
    val b = (0::p, 0::n){(i,j) => ((i + j * n) % 8).to[X] }
    val c_init = (0::m, 0::n){(_,_) => 0.to[X] }

    // Communicate dimensions to FPGA with ArgIns
    val M = ArgIn[Int]
    val N = ArgIn[Int]
    val P = ArgIn[Int]
    setArg(M,m)
    setArg(N,n)
    setArg(P,p)

    // Create pointers to matrices
    val A = DRAM[X](M, P)
    val B = DRAM[X](P, N)
    val C = DRAM[X](M, N)

    // Set up parallelizations
    val op = 1
    val mp = 1
    val ip = 1

    val bm = 16
    val bn = 64
    val bp = 64

    setMem(A, a)
    setMem(B, b)
    setMem(C, c_init)

    Accel {
      // Tile by output regions in C
      Foreach(M by bm par op, N by bn par op) { (i,j) =>
        val tileC = SRAM[X](bm, bn).buffer
                                                   
        // Prefetch C tile
        tileC load C(i::i+bm, j::j+bn par ip)
                                                   
        // Accumulate on top of C tile over all tiles in P dimension
        MemFold(tileC)(P by bp) { k =>
          val tileA = SRAM[X](bm, bp)
          val tileB = SRAM[X](bp, bn)
          val accum = SRAM[X](bm, bn)
                                 
          // Load A and B tiles
          // Parallel {
          tileA load A(i::i+bm, k::k+bp par ip)
          tileB load B(k::k+bp, j::j+bn par ip)
          // }
          
          // Perform matrix multiply on tile
          MemReduce(accum)(bp by 1 par mp){ kk =>
            val tileC_partial = SRAM[X](bm,bn)
            Foreach(bm by 1, bn by 1 par ip){ (ii,jj) =>
              tileC_partial(ii,jj) = tileA(ii,kk) * tileB(kk,jj)
            }
            tileC_partial
          }{_+_}
        }{_+_}
                                                   
        // Store C tile to DRAM
        C(i::i+bm, j::j+bn par ip) store tileC
      }
    }
    
  // Fetch result on host
    val result = getMatrix(C)

    // Compute correct answer
    val gold = (0::m, 0::n){(i,j) => 
      Array.tabulate(p){k => a(i,k) * b(k,j)}.reduce{_+_}
    }

    // Show results
    println(r"expected cksum: ${gold.map(a => a).reduce{_+_}}")
    println(r"result cksum: ${result.map(a => a).reduce{_+_}}")
    printMatrix(gold, "Gold: ")
    printMatrix(result, "Result: ")

    assert(gold == result)
  }

}
