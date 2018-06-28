package spatial.tests.feature.dense

import spatial.dsl._



@spatial class MatMult_outer extends SpatialTest {
  override def runtimeArgs: Args = "32 128 128"
  type X = FixPt[TRUE,_16,_16]

  val innerPar = 4
  val midPar = 2
  val outerPar = 2

  val tsm = 16
  val tsn = 64
  val tsp = 64


  def MatMult_outer[T:Num](A: Array[T], B: Array[T], C_init: Array[T], mm: Int, nn: Int, pp: Int): Array[T] = {
    val M = ArgIn[Int]
    val N = ArgIn[Int]
    val P = ArgIn[Int]
    setArg(M,mm)
    setArg(N,nn)
    setArg(P,pp)

    val a = DRAM[T](M, P)
    val b = DRAM[T](P, N)
    // val c_init = DRAM[T](M, N)
    val c = DRAM[T](M, N)

    val op = outerPar (1 -> 1)
    val mp = midPar (1 -> 16)
    val ip = innerPar (1 -> 64)
    val px = 1 (1 -> 1) // Cannot parallelize accum across k blocks

    val bm = tsm (48 -> 48 -> 1920)
    val bn = tsn (48 -> 48 -> 1920)
    val bp = tsp (48 -> 48 -> 1920)

    setMem(a, A)
    setMem(b, B)
    setMem(c, C_init)

    Accel {
      Sequential.Foreach(M by bm, N by bn par op) { (i,j) =>
        val tileC = SRAM[T](bm, bn)
        tileC load c(i::i+bm, j::j+bn par ip)
        MemFold(tileC)(P by bp) { k =>
          val tileA = SRAM[T](bm, bp)
          val tileB = SRAM[T](bp, bn)
          val accum = SRAM[T](bm, bn)
          Parallel {
            tileA load a(i::i+bm, k::k+bp par ip)
            tileB load b(k::k+bp, j::j+bn par ip)
          }
          MemReduce(accum)(bp by 1 par mp){ kk =>
            val tileC_partial = SRAM[T](bm,bn)
            Foreach(bm by 1, bn by 1 par ip){ (ii,jj) =>
              tileC_partial(ii,jj) = tileA(ii,kk) * tileB(kk,jj)
            }
            tileC_partial
          }{_+_}
        }{_+_}
        c(i::i+bm, j::j+bn par ip) store tileC
      }
    }
    getMem(c)
  }


  def main(args: Array[String]): Unit = {
    val M = args(0).to[Int]
    val N = args(1).to[Int]
    val P = args(2).to[Int]

    val a = Array.tabulate(M){ j => Array.tabulate(P){ i => ((i + j * P) % 8).to[X] } } // Standard array
    val b = Array.tabulate(P){ j => Array.tabulate(N){ i => ((i + j * N) % 8).to[X] } } // Standard array
    val c_init = Array.fill(M){ Array.fill(N){ 0.to[X] } }
    // val a = Array.fill(M){ Array.fill(P){random[T](100)} }
    // val b = Array.fill(P){ Array.fill(N){random[T](100)} }

    val result = MatMult_outer(a.flatten, b.flatten, c_init.flatten, M, N, P)

    val gold = Array.tabulate(M){i =>
      val aRow = a(i)
      Array.tabulate(N){j =>
        val bCol = b.map{row => row(j)}
        aRow.zip(bCol){_*_}.reduce{_+_}
      }
    }.flatten

    println("expected cksum: " + gold.map(a => a).reduce{_+_})
    println("result cksum: " + result.map(a => a).reduce{_+_})
    printArray(gold, "Gold: ")
    printArray(result, "Result: ")

    val cksum = result.zip(gold){_ == _}.reduce{_&&_}
    println("PASS: " + cksum + " (MatMult_outer)")
    assert(cksum)
  }

}
