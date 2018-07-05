package spatial.tests.feature.control


import spatial.dsl._


@spatial class NIter extends SpatialTest {
  override def runtimeArgs: Args = "192"

  val constTileSize = 96

  def nIterTest[T:Num](len: Int): T = {
    val innerPar = 1 (1 -> 1)
    val tileSize = constTileSize (constTileSize -> constTileSize)
    bound(len) = 9216

    val N = ArgIn[Int]
    val out = ArgOut[T]
    setArg(N, len)

    Accel {
      Sequential {
        Sequential.Foreach(N by tileSize){ i =>
          val accum = Reduce(Reg[T](0.to[T]))(tileSize par innerPar){ ii =>
            (i + ii).to[T]
          } {_+_}
          Pipe { out := accum }
        }
      }
    }

    getArg(out)
  }


  def main(args: Array[String]): Unit = {
    val len = args(0).to[Int]

    val result = nIterTest[Int](len)

    val b1 = Array.tabulate(len){i => i}

    val gold = b1.reduce{_+_} - ((len-constTileSize) * (len-constTileSize-1))/2
    println("expected: " + gold)
    println("result:   " + result)

    val cksum = gold == result
    println("PASS: " + cksum + " (Niter)")
    assert(cksum)
  }
}

