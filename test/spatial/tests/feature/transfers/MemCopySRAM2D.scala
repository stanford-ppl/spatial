package spatial.tests.feature.transfers

import spatial.dsl._

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
