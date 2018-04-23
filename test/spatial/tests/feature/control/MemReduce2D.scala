package spatial.tests.feature.control


import spatial.dsl._


@test class MemReduce2D extends SpatialTest {
  override def runtimeArgs: Args = "192 384"

  val N = 1920
  val tileSize = 16


  def main(args: Array[String]): Unit = {
    val numRows = args(0).to[Int]
    val numCols = args(1).to[Int]
    val src = Array.tabulate(numRows) { i => Array.tabulate(numCols) { j => (i*numCols + j)%256 } } // Standard array
    val flatsrc = src.flatten

    val rowsIn = ArgIn[Int]; setArg(rowsIn, numRows)
    val colsIn = ArgIn[Int]; setArg(colsIn, numCols)

    val srcFPGA = DRAM[Int](rowsIn, colsIn)
    val dstFPGA = DRAM[Int](tileSize, tileSize)
    val probe = ArgOut[Int]

    setMem(srcFPGA, src.flatten)

    Accel {
      val accum = SRAM[Int](tileSize,tileSize)
      MemReduce(accum)(rowsIn by tileSize, colsIn by tileSize par 2){ (i,j)  =>
        val tile = SRAM[Int](tileSize,tileSize)
        tile load srcFPGA(i::i+tileSize, j::j+tileSize  par 1)
        tile
      }{_+_}
      probe := accum(tileSize-1, tileSize-1)
      dstFPGA(0::tileSize, 0::tileSize par 1) store accum
    }
    val dst = getMem(dstFPGA)

    val numHorizontal = numRows/tileSize
    val numVertical = numCols/tileSize
    val a1 = Array.tabulate(tileSize) { i => i }
    val a2 = Array.tabulate(tileSize) { i => i }
    val a3 = Array.tabulate(numHorizontal) { i => i }
    val a4 = Array.tabulate(numVertical) { i => i }
    val gold = a1.map{i=> a2.map{j => a3.map{ k=> a4.map {l=>
      flatsrc(i*numCols + j + k*tileSize*tileSize + l*tileSize) }}.flatten.reduce{_+_}
    }}.flatten

    printArray(gold, "src:")
    printArray(dst, "dst:")
    println("Probe is " + getArg(probe) + ".  Should equal " + gold(tileSize * tileSize - 1))
    assert(gold == dst)
    assert(getArg(probe) == gold(tileSize*tileSize - 1))
  }
}
