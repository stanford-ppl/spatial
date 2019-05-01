package spatial.tests.feature.dense

import spatial.dsl._

@spatial class GDA extends SpatialTest {
  override def dseModelArgs: Args = "64 20"
  override def finalModelArgs: Args = "64 20 20"
  override def runtimeArgs: Args = "64"

  type X = Float

  val MAXC = 96
  val C = MAXC
  val margin = 1

  def gda[T:Num](xCPU: Array[T], yCPU: Array[Int], mu0CPU: Array[T], mu1CPU: Array[T]): Array[T] = {
    val ts = loadParam("ts", 32 (96 -> 19200))
    val op = loadParam("op", 2 (1 -> 8))
    val mp = loadParam("mp", 2 (1 -> 8))
    val ip = loadParam("ip", 16 (1 -> 12))
    val subLoopPar = loadParam("subPar", 16 (1 -> 16))

    val rows = yCPU.length
    bound(rows) = 360000
    val cols = mu0CPU.length
    bound(cols) = MAXC

    val R = ArgIn[Int]
    // val C = ArgIn[SInt]
    // setArg(C, cols)
    setArg(R, rows)

    val x = DRAM[T](R, C)
    val y = DRAM[Int](R)
    val mu0 = DRAM[T](C)
    val mu1 = DRAM[T](C)
    val sigma = DRAM[T](C, C)

    setMem(x, xCPU)
    setMem(y, yCPU)
    setMem(mu0, mu0CPU)
    setMem(mu1, mu1CPU)

    Accel {
      val mu0Tile = SRAM[T](MAXC)
      val mu1Tile = SRAM[T](MAXC)
      Parallel {
        mu0Tile load mu0(0 :: C par ip) // Load mu0
        mu1Tile load mu1(0 :: C par ip) // Load mu1
      }

      val sigmaOut = SRAM[T](MAXC, MAXC)

      MemReduce(sigmaOut par ip)(R by ts par op){ r =>
        val gdaYtile = SRAM[Int](ts)
        val gdaXtile = SRAM[T](ts, MAXC)
        val blk = Reg[Int]
        Parallel {
          gdaYtile load y(r :: r + ts par ip)
          gdaXtile load x(r :: r + ts, 0 :: C par ip) // Load tile of x
          Pipe {
            blk := min(R.value - r, ts)
          }
        }

        val sigmaBlk = SRAM[T](MAXC, MAXC)

        MemReduce(sigmaBlk par ip)(blk par mp) { rr =>
          val subTile = SRAM[T](MAXC)
          val sigmaTile = SRAM[T](MAXC, MAXC)
          Foreach(C par subLoopPar) { cc =>
            subTile(cc) = gdaXtile(rr, cc) - mux(gdaYtile(rr) == 1, mu1Tile(cc), mu0Tile(cc))
          }
          Foreach(C by 1, C par ip) { (ii, jj) =>
            sigmaTile(ii, jj) = subTile(ii) * subTile(jj)
          }
          sigmaTile
        }{_+_}
      }{_+_}

      sigma(0 :: C, 0 :: C par ip) store sigmaOut
    }

    getMem(sigma)
  }


  def main(args: Array[String]): Unit = {
    val R = args(0).to[Int]

    val x = Array.tabulate(R) { i => Array.tabulate(C) { j => (i * C + j) % 256 } }
    val ys = Array.tabulate(R) { i => i % 256 }
    val mu0 = Array.tabulate(C) { i => i % 2 }
    val mu1 = Array.tabulate(C) { i => i % 2 }

    val result = gda(x.flatten, ys, mu0, mu1)

    val gold = x.zip(ys) { (row, y) =>
      val sub = if (y == 1) row.zip(mu1){_-_} else row.zip(mu0) {_-_}
      Array.tabulate(C) { i => Array.tabulate(C) { j => sub(i) * sub(j) } }.flatten
    }.reduce { (a, b) => a.zip(b) {_+_} }

    printArray(gold, "gold: ")
    printArray(result, "result: ")

    val cksum = gold.zip(result){ case (a,b) => a < b + margin && a > b - margin }.reduce{_&&_}
    println("PASS: " + cksum  + " (GDA)")
    assert(cksum)
  }
}
