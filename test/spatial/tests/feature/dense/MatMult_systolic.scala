package spatial.tests.feature.dense

import spatial.dsl._



@spatial class MatMult_systolic extends SpatialTest {
  override def compileArgs = super.compileArgs and "--noModifyStream"
  override def dseModelArgs: Args = "32 128 128"

  type X = FixPt[TRUE,_32,_0]

  val outerTile = 4
  val innerTile = 32
  val redPar = 4

  def MatMult_inner[T:Num](A: Matrix[T], B: Matrix[T], mm: Int, nn: Int, pp: Int) : Matrix[T] = {
    val M = ArgIn[Int]
    val N = ArgIn[Int]
    val P = ArgIn[Int]
    setArg(M,mm)
    setArg(N,nn)
    setArg(P,pp)

    val a = DRAM[T](M, P)
    val b = DRAM[T](N, P)
    val c = DRAM[T](M, N)

    setMem(a, A)
    setMem(b, B.t) // Need to reorder and get the transpose for this to work

    Accel {
      val C = SRAM[T](outerTile, outerTile)
      Stream.Foreach(M by outerTile, N by outerTile, P by innerTile) { (ii, jj, kk) =>
        val A_fifos = List.tabulate(outerTile){i => FIFO[T](innerTile)}
        val B_fifos = List.tabulate(outerTile){i => FIFO[T](innerTile)}
        // Load FIFOs
        A_fifos.zipWithIndex.foreach{case (f,i) => if (ii + i < M) f load a(ii + i,kk::kk+innerTile )}
        B_fifos.zipWithIndex.foreach{case (f,j) => if (jj + j < N) f load b(jj + j,kk::kk+innerTile )}

        // Drain FIFOs and compute
        val A_sr = RegFile[T](outerTile, outerTile)
        val B_sr = RegFile[T](outerTile, outerTile)
        val computeDone = FIFO[Int](8)
        Pipe {
          val C_acc = SRAM[T](outerTile, outerTile)
          Foreach(innerTile + outerTile*2 - 1 by 1) { k =>
            Parallel {
              def deqOr0(f: FIFO[T], lineId: scala.Int, shiftId: Int): T = {
                if (shiftId >= lineId && shiftId < lineId + innerTile) f.deq() else 0.to[T]
              }
              A_fifos.zipWithIndex.foreach { case (f, i) => Pipe{A_sr(i, *) <<= deqOr0(f, i, k) }}
              B_fifos.zipWithIndex.foreach { case (f, j) => Pipe{B_sr(*, j) <<= deqOr0(f, j, k) }}
            }
            Pipe.II(1).Foreach(outerTile by 1 par redPar, outerTile by 1) { (i, j) => // II=1 is safe as long as outerTile is bigger than, say, 3?
              val A = mux(k >= i + j, A_sr(i, j), 0.to[T])
              val B = mux(k >= i + j, B_sr(i, j), 0.to[T])
              val update = A * B + mux(k > i + j || kk != 0, C_acc(i, j), 0.to[T])
              C_acc(i, j) = update
              if (kk == P - innerTile && k == innerTile + outerTile*2 - 2) C(i, j) = update
            }
          }
          computeDone.enq(mux(kk == P - innerTile, 1, 0))
        }

        // Store
        Pipe{
          if (computeDone.deq() == 1) c(ii::ii+outerTile, jj::jj+outerTile) store C
        }
      }

    }
    getMatrix(c)
  }


  def main(args: Array[String]): Unit = {
    val M = args(0).to[Int]
    val N = args(1).to[Int]
    val P = args(2).to[Int]
    assert(M % outerTile == 0, r"M must be divisible by $outerTile")
    assert(N % outerTile == 0, r"N must be divisible by $outerTile")
    assert(P % innerTile == 0, r"P must be divisible by $innerTile")

    val a = (0::M,0::P)((i,j) => ((i*P + j)%8).to[X]  )
    val b = (0::P,0::N)((i,j) => ((i*N + j)%8).to[X]  )
//    val bt = (0::N,0::P)((j,i) => ((i*N + j)%8).to[X]  )
    // val a = Array.fill(M){ Array.fill(P){random[T](100)} }
    // val b = Array.fill(P){ Array.fill(N){random[T](100)} }

    val result = MatMult_inner(a, b, M, N, P)

    val gold = (0::M,0::N){(i,j) =>
        Array.tabulate(P){k => a(i,k) * b(k,j)}.reduce{_+_}
    }

    val gold_cksum = gold.map(a => a).reduce{_+_}
    val result_cksum = result.flatten.map(a => a).reduce{_+_}
    printMatrix(gold, "Gold: ")
    printMatrix(result, "Result: ")
    println("expected cksum: " + gold_cksum)
    println("result cksum:   " + result_cksum)

    // (0 until M*N) foreach { i => assert(result(i) == gold(i)) }

    val cksum = result_cksum == gold_cksum
    println("PASS: " + cksum + " (MatMult_systolic)")
    assert(cksum)
  }

}

