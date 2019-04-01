package spatial.tests.feature.control


import spatial.dsl._


@spatial class FoldSimple extends SpatialTest {
  override def dseModelArgs: Args = "1920"
  override def finalModelArgs: Args = "1920"
  override def runtimeArgs: Args = "1920"
  val constTileSize = 16

  def simple_fold[T:Num](src: Array[T]): T = {
    val outerPar = 1 (16 -> 16)
    val innerPar = 1 (16 -> 16)
    val tileSize = constTileSize (constTileSize -> constTileSize)
    val len = src.length; bound(len) = 9216

    val N = ArgIn[Int]
    val out = ArgOut[T]
    setArg(N, len)

    val v1 = DRAM[T](N)
    setMem(v1, src)

    Accel {
      val accum = Reg[T](0.to[T])
      Reduce(accum)(N by tileSize par outerPar){ i =>
        val b1 = SRAM[T](tileSize)
        b1 load v1(i::i+tileSize par 16)
        Reduce(Reg[T](0.to[T]))(tileSize par innerPar){ ii =>
          b1(ii)
        } {_+_}
      } {_+_}
      Pipe { out := accum }
    }

    getArg(out)
  }


  def main(args: Array[String]): Unit = {
    val len = args(0).to[Int]

    val src = Array.tabulate(len){i => i % 256}
    val result = simple_fold(src)

    val gold = src.reduce{_+_}
    println("expected: " + gold)
    println("result:   " + result)
    assert(result == gold)
  }
}

