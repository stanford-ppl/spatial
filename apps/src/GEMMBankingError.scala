import spatial.dsl._

@spatial object GEMMBankingError extends SpatialApp {
  type X = FixPt[TRUE,_16,_16]

  def main(args: Array[String]): Unit = {


    val M = 4
    val K = 64
    val N = 128

    val a = (0::M, 0::K){(i,j) => ((i + j * K) % 8).to[X] }
    val b = (0::K, 0::N){(i,j) => ((i + j * N) % 8).to[X] }
    val c_init = (0::M, 0::N){(_,_) => 0.to[X] }

    val A = DRAM[X](M, K)
    val B = DRAM[X](K, N)
    val C = DRAM[X](M, N)

    // *** Set mp and ip > 1
    val mp = 1
    val kp = 4
    val np = 4

    setMem(A, a)
    setMem(B, b)
    setMem(C, c_init)

    Accel {
      val sramA = SRAM[X](M, K)
      val sramB = SRAM[X](K, N)
      val sramC = SRAM[X](M, N)

      sramA load A(0::M,0::K par 1)
      sramB load B(0::K,0::N par 1)
      sramC load C(0::M,0::N par 1)

      val lastTile = (K/kp - 1) * kp

      Foreach (M by 1 par mp, N by 1 par np, K by kp) { (m,n,kTile) =>
        val f = Reg[X]
        def reduceTreeDp(): X = {
          List.tabulate(kp){ ii =>
            val kk = kTile + ii
            val re = sramA(m,kk) * sramB(kk,n)
            re
          }.sumTree
        }

        val t = reduceTreeDp()
        f := mux(kTile == 0.to[I32] , t , t + f.value)

        if(kTile == lastTile) {
          sramC(m, n) = f.value
        }

      }
      C(0::M,0::N par 1) store sramC
    }
  val result = getMatrix(C)
  val gold = (0::M, 0::N){(i,j) =>
    Array.tabulate(K){k => a(i,k) * b(k,j)}.reduce{_+_}
  }
  println(r"expected cksum: ${gold.map(a => a).reduce{_+_}}")
  println(r"result cksum: ${result.map(a => a).reduce{_+_}}")
  printMatrix(gold, "Gold: ")
  printMatrix(result, "Result: ")
  assert(gold == result)
  }
}


