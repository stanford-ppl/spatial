package spatial.tests.feature.transfers


import spatial.dsl._


@spatial class MemCopySRAM1D extends SpatialTest {
  override def runtimeArgs: Args = "100"

  val N = 192


  def simpleLoadStore[T:Num](srcHost: Array[T], value: T): Array[T] = {
    val loadPar  = 1 (1 -> 1)
    val storePar = 1 (1 -> 1)
    val tileSize = 16 (16 -> 16)

    val srcFPGA = DRAM[T](N)
    val dstFPGA = DRAM[T](N)
    setMem(srcFPGA, srcHost)

    Accel {
      Sequential.Foreach(N by tileSize par 2) { i =>
        val b1 = SRAM[T](tileSize)

        b1 load srcFPGA(i::i+tileSize par 1)
        dstFPGA(i::i+tileSize par 1) store b1
      }
    }
    getMem(dstFPGA)
  }


  def main(args: Array[String]): Unit = {
    val arraySize = N
    val value = args(0).to[Int]

    val src = Array.tabulate[Int](arraySize) { i => i % 256 }
    val dst = simpleLoadStore(src, value)

    printArray(src, "Source")
    printArray(dst, "Dest")

    assert(dst == src)
  }
}



@spatial class MemCopySRAM2D extends SpatialTest {
  val R = 16
  val C = 16

  def memcpy_2d[T:Num](src: Array[T], rows: Int, cols: Int): Array[T] = {
    val tileDim1 = R
    val tileDim2 = C

    val rowsIn = rows
    val colsIn = cols

    val srcFPGA = DRAM[T](rows, cols)
    val dstFPGA = DRAM[T](rows, cols)

    // Transfer data and start accelerator
    setMem(srcFPGA, src)

    Accel {
      Sequential.Foreach(rowsIn by tileDim1, colsIn by tileDim2) { (i,j) =>
        val tile = SRAM[T](tileDim1, tileDim2)
        tile load srcFPGA(i::i+tileDim1, j::j+tileDim2 par 1)
        dstFPGA (i::i+tileDim1, j::j+tileDim2 par 1) store tile
      }
    }
    getMem(dstFPGA)
  }


  def main(args: Array[String]): Unit = {
    val rows = R
    val cols = C
    val src = Array.tabulate(rows*cols) { i => i % 256 }

    val dst = memcpy_2d(src, rows, cols)

    printArray(src, "src:")
    printArray(dst, "dst:")

    val cksum = dst.zip(src){_ == _}.reduce{_&&_}
    assert(cksum)
  }
}

@spatial class MemCopySRAM2D_2 extends SpatialTest {

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
