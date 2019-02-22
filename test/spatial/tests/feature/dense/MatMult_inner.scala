package spatial.tests.feature.dense

import spatial.dsl._



@spatial class MatMult_inner extends SpatialTest {
  override def dseModelArgs: Args = "32 128 128"
  override def finalModelArgs: Args = "32 128 128"
  override def runtimeArgs: Args = "32 128 128"

  type X = FixPt[TRUE,_16,_16]

  val innerPar = 16
  val midPar = 2
  val outerPar = 2

  val tsm = 16
  val tsn = 64
  val tsp = 64


  def MatMult_inner[T:Num](A: Array[T], B: Array[T], mm: Int, nn: Int, pp: Int) = {
    val M = ArgIn[Int]
    val N = ArgIn[Int]
    val P = ArgIn[Int]
    setArg(M,mm)
    setArg(N,nn)
    setArg(P,pp)

    val a = DRAM[T](M, P)
    val b = DRAM[T](P, N)
    val c = DRAM[T](M, N)

    val bm = tsm (1 -> 1536)
    val bn = tsn (64 -> 64 -> 1536)
    val bp = tsp (64 -> 64 -> 1536)

    val op = outerPar (1 -> 6)
    val mp = midPar   (1 -> 64)
    val ip = innerPar (1 -> 64)
    val px = 1 (1 -> 1) // Cannot parallelize accum across k blocks

    setMem(a, A)
    setMem(b, B)

    Accel {
      Foreach(M by bm, N by bn par op){(i,j) =>
        val tileC = SRAM[T](bm, bn)

        Foreach(P by bp par px){k =>
          val tileA = SRAM[T](bm, bp)
          val tileB = SRAM[T](bp, bn)
          Parallel {
            tileA load a(i::i+bm, k::k+bp par 1) // Reads M*N*P times
            tileB load b(k::k+bp, j::j+bn par 1)
          }
          Foreach(bm by 1, bn by 1 par mp){ (ii,jj) =>    // MetaPipe?
            val prod = Reduce(Reg[T])(bp by 1 par ip){kk => tileA(ii, kk) * tileB(kk, jj) }{_+_}
            val prev = mux(k == 0, 0.to[T], tileC(ii,jj))
            tileC(ii,jj) = prev + prod.value // Is a unit pipe that should be recognized as accum
          }
        }
        c(i::i+bm, j::j+bn) store tileC // Writes M*N times
      }
    }
    getMem(c)
  }


  def main(args: Array[String]): Unit = {
    val M = args(0).to[Int]
    val N = args(1).to[Int]
    val P = args(2).to[Int]

    val a = Array.tabulate(M){ i => Array.tabulate(P){ j => ((i*P + j)%8).to[X] } }
    val b = Array.tabulate(P){ i => Array.tabulate(N){ j => ((i*N + j)%8).to[X] } }
    // val a = Array.fill(M){ Array.fill(P){random[T](100)} }
    // val b = Array.fill(P){ Array.fill(N){random[T](100)} }

    val result = MatMult_inner(a.flatten, b.flatten, M, N, P)

    val gold = Array.tabulate(M){i =>
      val aRow = a(i)
      Array.tabulate(N){j =>
        val bCol = b.map{row => row(j)}
        aRow.zip(bCol){_*_}.reduce{_+_}
      }
    }.flatten

    val gold_cksum = gold.map(a => a).reduce{_+_}
    val result_cksum = result.map(a => a).reduce{_+_}
    printArray(gold, "Gold: ")
    printArray(result, "Result: ")
    println("expected cksum: " + gold_cksum)
    println("result cksum:   " + result_cksum)

    // (0 until M*N) foreach { i => assert(result(i) == gold(i)) }

    val cksum = result_cksum == gold_cksum
    println("PASS: " + cksum + " (MatMult_inner) * Remember to fix GEMM_MemoryHierarchy once issue #159 is fixed!")
    assert(cksum)
  }

}

