import spatial.dsl._

@spatial object AccumType17 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val vecLen = 16
    val n = 128
    val k = 64

    val A = (0 :: n, 0 :: k) {(i, j) => 1.to[Int]}
    val BVec = (0 :: k) { i => 1.to[Int] }

    val ADRAM = DRAM[Int](n, k)
    val BDRAM = DRAM[Int](k)

    val cDRAM = DRAM[Int](n)
    setMem(ADRAM, A)
    setMem(BDRAM, BVec)
    Accel {
      val aMem = SRAM[Int](n, k)
      val bMem = SRAM[Int](k)
      val cMem = SRAM[Int](n)

      aMem load ADRAM
      bMem load BDRAM

      // VCS Result is incorrect. The first element is 32 instead of 64...
      'ACC17.Foreach(n by vecLen) { iTile =>
        val acc17List = List.tabulate(vecLen)(_ => Reg[Int](0) )
        Foreach (k by vecLen) { jTile =>
          acc17List.zipWithIndex.foreach {
            case (acc, accOffSet) =>
              val t = List.tabulate(vecLen) ( ii => aMem(iTile + accOffSet, jTile + ii) * bMem(jTile + ii)).sumTree
              acc := mux(jTile == 0, t, t + acc.value)
          }
        }

        acc17List.zipWithIndex.foreach {
          case (acc, accOffSet) =>
            cMem(iTile + accOffSet) = acc.value
        }
      }

      cDRAM store cMem
    }

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
