package spatial.tests.feature.transfers


import spatial.dsl._


@test class MemCopySRAM2D_2 extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val m     = 16.to[Int]
    val n     = 16.to[Int]

    val dataIn  = (0 :: m.to[Int], 0 :: n.to[Int]){(i,j) => (i*m+j).to[Float] }
    val dramIn  = DRAM[Float](m,n)
    val dramOut = DRAM[Float](n,m)

    setMem(dramIn, dataIn)
    print("\n*** Input Matrix ***\n")
    printMatrix(dataIn)

    Accel {
      val sramIn  = SRAM[Float](m,n)
      val sramOut = SRAM[Float](n,m)

      // Load data from DRAM
      sramIn load dramIn(0 :: m, 0 :: n)

      Foreach(0 :: sramIn.rows, 0 :: sramIn.cols) { (i,j) =>
        sramOut(i,j) = sramIn(j,i)
      }

      dramOut(0 :: n, 0 :: m) store sramOut
    }

    val dataOut = getMatrix(dramOut)
    print("\n*** Output Matrix ***\n")
    printMatrix(dataOut)
    (0 :: n, 0 :: m).foreach{(i,j) => assert(dataOut(i,j) == dataIn(j,i)) }
    () // annoying but required
  }
}
