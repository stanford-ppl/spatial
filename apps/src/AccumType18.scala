import spatial.dsl._

@spatial object AccumType18 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val vecLen = 16
    val fifoDepth = 2
    val n = 128
    val k = 64

    val A = (0 :: n, 0 :: k) {(i, j) => 1.to[Int]}
    val BVec = (0 :: k) { i => 1.to[Int] }

    val ADRAM = DRAM[Int](n, k)
    val BDRAM = DRAM[Int](k)

    val cDRAM = DRAM[Int](n)
    setMem(ADRAM, A)
    setMem(BDRAM, BVec)

    val delayLineDepth = 2
    val delayLineArgs = List.tabulate(delayLineDepth)(_ => ArgIn[Int])
    delayLineArgs.foreach(r => setArg(r, 1))
    val delayArgOut = ArgOut[Int]

    Accel {
      val aMem = SRAM[Int](n, k)
      val bMem = SRAM[Int](k)
      val cMem = SRAM[Int](n)

      aMem load ADRAM
      bMem load BDRAM

      'ACC18.Stream.Foreach(n by vecLen) { iTile =>
        val fifo = FIFO[Int](fifoDepth)
        val acc18List = List.tabulate(vecLen)(_ => Reg[Int](0) )
        Foreach (k by vecLen) { jTile =>
          val lastTile = (k / vecLen - 1) * vecLen
          acc18List.zipWithIndex.foreach {
            case (acc, accOffSet) =>
              val t = List.tabulate(vecLen) ( ii => aMem(iTile + accOffSet, jTile + ii) * bMem(jTile + ii)).sumTree
              acc := mux(jTile == 0, t, t + acc.value)
              if (jTile == lastTile)
                fifo.enq(t + delayLineArgs.map(r => r.value).reduce((a, b) => a + b))
          }
        }

        Pipe {
          delayArgOut := fifo.deq()
          acc18List.zipWithIndex.foreach {
            case (acc, accOffSet) =>
              cMem(iTile + accOffSet) = acc.value
          }
        }
      }

      cDRAM store cMem
    }

    println("delayArgOut = " + getArg(delayArgOut))
    val result = getMem(cDRAM)
    val gold = (0 :: n) { i =>
      Array.tabulate(k) { j =>
        A(i, j) * BVec(j)
      }.reduce{_+_}
    }
    printArray(result)
    printArray(gold)
  }
}
