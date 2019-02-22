package spatial.tests.feature.control


import spatial.dsl._


@spatial class NIter2 extends SpatialTest {
  override def dseModelArgs: Args = "192"
  override def finalModelArgs: Args = "192"
  override def runtimeArgs: Args = "100"

  val constTileSize = 16


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
          val redMax = Reg[Int](999)
          Pipe{ redMax := min(tileSize, N.value-i) }
          val accum = Reduce(Reg[T](0.to[T]))(redMax par innerPar){ ii =>
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

    val m = (len-1)%constTileSize + 1
    val b1 = m*(m-1)/2
    val gold = b1 + (len - m)*m
    println("expected: " + gold)
    println("result:   " + result)
    assert(gold == result)
  }
}

