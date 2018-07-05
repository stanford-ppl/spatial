package spatial.tests.feature.transfers


import spatial.dsl._


@spatial class UnalignedLoadStore2D extends SpatialTest {
  override def runtimeArgs: Args = "400"


  def main(args: Array[String]): Unit = {

    val src1 = (0::16,0::16){(i,j) => i}
    val dram1 = DRAM[Int](16,16)
    val dram2 = DRAM[Int](16,16)

    setMem(dram1,src1)

    Accel {
      val sram1 = SRAM[Int](16)
      Foreach(-5 until 15 by 1){i =>
        val ldrow: Int = if ((i.to[Int] + 1.to[Int]) >= 0.to[Int] && (i.to[Int] + 1.to[Int]) <= 15) {i.to[Int] + 1.to[Int]} else 0
        sram1 load dram1(ldrow,0::16)
        dram2(ldrow,0::16) store sram1
      }
    }
    val out = getMatrix(dram2)
    printMatrix(out, "Result:")
    printMatrix(src1, "Gold:")
    assert(out == src1)
  }
}


@spatial class UnalignedTileLoadStore extends SpatialTest { // Regression (Unit) // Args: 100
  override def runtimeArgs: Args = "100"


  val M = 10
  val N = 10
  def main(args: Array[String]): Void = {
    type T1 = Int
    type T2 = FixPt[TRUE, _16, _0]

    val srcT1 = Array.tabulate[T1](10) { i => (i % 10).to[T1] }
    val initT1 = (0::10, 0::20){(i,j) => -1.to[T1]}
    val srcT2 = Array.tabulate[T2](10) { i => (i % 10).to[T2] }
    val initT2 = (0::10, 0::20){(i,j) => -1.to[T2]}

    val srcFPGAT1 = DRAM[T1](10)
    val srcFPGAT2 = DRAM[T2](10)
    val dstFPGA1 = DRAM[T1](10,20)
    val dstFPGA2 = DRAM[T1](10,20)
    val dstFPGA3 = DRAM[T2](10,20)
    val dstFPGA4 = DRAM[T2](10,20)
    setMem(srcFPGAT1, srcT1)
    setMem(srcFPGAT2, srcT2)
    setMem(dstFPGA1, initT1)    
    setMem(dstFPGA2, initT1)    
    setMem(dstFPGA3, initT2)    
    setMem(dstFPGA4, initT2)    


    Accel {
      val tile1 = SRAM[T1](16)
      Foreach(10 by 1){i => 
        tile1 load srcFPGAT1(0::10)
        dstFPGA1(i, i::i+10) store tile1
      }

      val tile2 = SRAM[T1](16)
      Foreach(10 by 1){i => 
        tile2 load srcFPGAT1(0::10 par 8)
        dstFPGA2(i, i::i+10 par 8) store tile2
      }

      val tile3 = SRAM[T2](16)
      Foreach(10 by 1){i => 
        tile3 load srcFPGAT2(0::10)
        dstFPGA3(i, i::i+10) store tile3
      }

      val tile4 = SRAM[T2](16)
      Foreach(10 by 1){i => 
        tile4 load srcFPGAT2(0::10 par 8)
        dstFPGA4(i, i::i+10 par 8) store tile4
      }
    }

    val gold1 = (0::10, 0::20){(i,j) => if (j < i || j >= i + 10) -1.to[T1] else srcT1(j-i) }
    val gold2 = (0::10, 0::20){(i,j) => if (j < i || j >= i + 10) -1.to[T1] else srcT1(j-i) }
    val gold3 = (0::10, 0::20){(i,j) => if (j < i || j >= i + 10) -1.to[T2] else srcT2(j-i) }
    val gold4 = (0::10, 0::20){(i,j) => if (j < i || j >= i + 10) -1.to[T2] else srcT2(j-i) }
    val got1 = getMatrix(dstFPGA1)
    val got2 = getMatrix(dstFPGA2)
    val got3 = getMatrix(dstFPGA3)
    val got4 = getMatrix(dstFPGA4)

    printMatrix(gold1, "Wanted1:")
    printMatrix(got1, "Got1:")
    printMatrix(gold2, "Wanted2:")
    printMatrix(got2, "Got2:")
    printMatrix(gold3, "Wanted3:")
    printMatrix(got3, "Got3:")
    printMatrix(gold4, "Wanted4:")
    printMatrix(got4, "Got4:")

    val cksum1 = got1 == gold1
    val cksum2 = got2 == gold2
    val cksum3 = got3 == gold3
    val cksum4 = got4 == gold4
    println("cksum1: " + cksum1)
    println("cksum2: " + cksum2)
    println("cksum3: " + cksum3)
    println("cksum4: " + cksum4)
    val cksum = cksum1 && cksum2 && cksum3 && cksum4
    println("PASS: " + cksum + " (UnalignedTileLoadStore)")
    assert(cksum)
  }
}
