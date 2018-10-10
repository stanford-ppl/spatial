package spatial.tests.feature.dense

import spatial.dsl._

@spatial class OuterProduct extends SpatialTest {
  override def runtimeArgs: Args = "640 640"
  type X = FixPt[TRUE,_32,_0]

  defaultParams(defaults=
    "ts1" -> 32 (64 -> 64 -> 38400),
    "ts2" -> 32 (64 -> 64 -> 38400),
    "ip" -> 8 (1 -> 256),
    "op" -> 1 (1 -> 4)
  )

  def outerproduct[T:Num](a: Array[T], b: Array[T]): Matrix[T] = {
    val tileSizeA = params("ts1")
    val tileSizeB = params("ts2")
    val outerPar  = params("op")
    val innerPar  = params("ip")

    val M = a.length;  bound(M) = 38400
    val N = b.length;  bound(N) = 38400

    val sizeA = ArgIn[Int]
    val sizeB = ArgIn[Int]
    setArg(sizeA, M)
    setArg(sizeB, N)

    val vec1 = DRAM[T](sizeA)
    val vec2 = DRAM[T](sizeB)
    val out = DRAM[T](sizeA, sizeB)

    setMem(vec1, a)
    setMem(vec2, b)

    Accel {
      Foreach(sizeA by tileSizeA, sizeB by tileSizeB par outerPar){ (i,j) =>
        val b1 = SRAM[T](tileSizeA)
        val b2 = SRAM[T](tileSizeB)
        val outTile = SRAM[T](tileSizeA, tileSizeB)
        //val blkA = Reg[Int]
        //val blkB = Reg[Int]
        Parallel {
          b1 load vec1(i::i+tileSizeA par innerPar)
          b2 load vec2(j::j+tileSizeB par innerPar)
          //Pipe{ blkA := min(sizeA - i, tileSizeA) }
          //Pipe{ blkB := min(sizeB - j, tileSizeB) }
        }
        Foreach(tileSizeA by 1, tileSizeB par innerPar){ (ii,jj) => outTile(ii, jj) = b1(ii) * b2(jj) } // 2

        out(i::i+tileSizeA, j::j+tileSizeB par 16) store outTile
      }
    }
    getMatrix(out)
  }


  def main(args: Array[String]): Unit = {
    val M = args(0).to[Int]
    val N = args(1).to[Int]
    val a = Array.tabulate(M) { i => (i % 64).to[X] }
    val b = Array.tabulate(N){ i => (i % 64).to[X] }

    val result = outerproduct(a, b)

    val gold = (0::M, 0::N){(i,j) =>  a(i) * b(j) }
    val gold_cksum = gold.flatten.reduce{_+_}
    val result_cksum = result.flatten.reduce{_+_}
    println("expected cksum: " + gold_cksum)
    println("result cksum:   " + result_cksum)
    printMatrix(gold, "Gold")
    printMatrix(result, "Result")
    // (0 until M*N) foreach { i => assert(result(i) == gold(i)) }

    val cksum = result_cksum == gold_cksum
    println("PASS: " + cksum + " (OuterProduct)")
    assert(cksum)
  }
}
