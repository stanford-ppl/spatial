import spatial.dsl._

@spatial object ArgInOut {

  def main(): Void = {
    val a = ArgIn[I32]
    val b = ArgOut[I32]
    setArg(a, args(0).to[I32])
    Accel {
      b := a + 4
    }
    println("b = a + 4 = " + getArg(b))
    println(r"PASS: ${getArg(b) == a + 4} (ArgInOut)")
  }
}

@spatial object Niter extends SpatialApp { // Regression (Unit) // Args: 100
  val constTileSize = 16

  def nIterTest(len: I32): I32 = {
    val innerPar = 1 (1 -> 1)
    val tileSize = constTileSize (constTileSize -> constTileSize)

    val N = ArgIn[I32]
    val out = ArgOut[I32]
    setArg(N, len)

    Accel {
      Sequential {
        Sequential.Foreach(N by tileSize){ i =>
          val redMax = Reg[I32](999)
          Pipe{ redMax := min(tileSize, N.value-i) }
          val accum = Reduce(Reg[I32](0.to[I32]))(redMax par innerPar){ ii =>
            (i + ii).to[I32]
          } {_+_}
          Pipe { out := accum }
        }
      }
    }

    getArg(out)
  }

  def main(): Void = {
    val len = args(0).to[I32]

    val result = nIterTest(len)

    val m = (len-1)%constTileSize + 1
    val b1 = m*(m-1)/2
    val gold = b1 + (len - m)*m
    println("expected: " + gold)
    println("result:   " + result)

    val cksum = gold == result
    println(r"PASS: $cksum (Niter)")
  }
}
