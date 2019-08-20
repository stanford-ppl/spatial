import spatial.dsl._

@spatial object AccumType16 extends SpatialApp {
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


      'ACC16.Foreach(n by vecLen, k by vecLen) { (iTile, jTile) => // map and then reduce

        val acc16List = List.tabulate(vecLen)(_ => Reg[Int](0) )
        val lastTile = (k / vecLen - 1) * vecLen
        acc16List.zipWithIndex.foreach {
          case (acc, accOffSet) =>
            val t = List.tabulate(vecLen) ( ii => aMem(iTile + accOffSet, jTile + ii) * bMem(jTile + ii)).sumTree
            acc := mux(jTile == 0, t, t + acc.value)
            // Ideal II = 1, however this gives 2.
            // VCS also reports the wrong result at the first element of each foreach.
            // IR shows an extra register.
            cMem(iTile + accOffSet) = mux(jTile == lastTile, acc.value, 0)
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
