package spatial.tests.feature.control


import spatial.dsl._


@spatial class MemReduce2D extends SpatialTest {
  override def dseModelArgs: Args = "192 384"
  override def finalModelArgs: Args = "192 384"
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

@spatial class PartialMemReduce2D extends SpatialTest { // Regression (Unit) // Args: 192 384
  override def dseModelArgs: Args = "192 384 16 16"
  override def finalModelArgs: Args = "192 384 16 16"
  override def runtimeArgs: Args = "192 384"


  val N = 1920
  val tileSize = 16


  def main(args: Array[String]): Void = {
    val numRows = args(0).to[Int]
    val numCols = args(1).to[Int]
    val src = Array.tabulate(numRows) { i => Array.tabulate(numCols) { j => (i*numCols + j)%256 } } // Standard array
    val flatsrc = src.flatten

    val rowsIn = ArgIn[Int]; setArg(rowsIn, numRows)
    val colsIn = ArgIn[Int]; setArg(colsIn, numCols)

    val srcFPGA = DRAM[Int](rowsIn, colsIn)
    val dstFPGA = DRAM[Int](tileSize, tileSize)
    val dstFPGAPartial = DRAM[Int](tileSize*2, tileSize*2)
    val probe = ArgOut[Int]

    val partialQuadrantX = ArgIn[Int]
    val partialQuadrantY = ArgIn[Int]
    setArg(partialQuadrantX, 0.to[Int])
    setArg(partialQuadrantY, 1.to[Int])

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

      // Reduce into top right corner of 32x32 accum
      val partial_accum = SRAM[Int](tileSize*2,tileSize*2)
      val x_start = mux(partialQuadrantX.value == 0.to[Int], 0.to[Int], tileSize.to[Int])
      val x_end = mux(partialQuadrantX.value == 0.to[Int], tileSize.to[Int], 2*tileSize.to[Int])
      val y_start = mux(partialQuadrantY.value == 0.to[Int], 0.to[Int], tileSize.to[Int])
      val y_end = mux(partialQuadrantY.value == 0.to[Int], tileSize.to[Int], 2*tileSize.to[Int])
      MemReduce(partial_accum(x_start::x_end, y_start::y_end))(4 by 1) {i =>
        val partial_tile = SRAM[Int](tileSize*2, tileSize*2)
        Foreach(tileSize by 1, tileSize by 1){ (ii,jj) =>
          partial_tile(x_start + ii, y_start + jj) = i
        }
        partial_tile(x_start::x_end, y_start::y_end)
      }{_+_}
      dstFPGAPartial store partial_accum

    }
    val dst = getMem(dstFPGA)
    val dstpartial = getMatrix(dstFPGAPartial)

    val portion = (0::tileSize, tileSize::tileSize*2){(i,j) => dstpartial(i,j)}


    val numHorizontal = numRows/tileSize
    val numVertical = numCols/tileSize
    val numBlocks = numHorizontal*numVertical
    // val gold = Array.tabulate(tileSize){i =>
    //   Array.tabulate(tileSize){j =>

    //     flatsrc(i*tileSize*tileSize + j*tileSize) }}.flatten
    // }.reduce{(a,b) => a.zip(b){_+_}}
    val a1 = Array.tabulate(tileSize) { i => i }
    val a2 = Array.tabulate(tileSize) { i => i }
    val a3 = Array.tabulate(numHorizontal) { i => i }
    val a4 = Array.tabulate(numVertical) { i => i }
    val gold = a1.map{i=> a2.map{j => a3.map{ k=> a4.map {l=>
      flatsrc(i*numCols + j + k*tileSize*tileSize + l*tileSize) }}.flatten.reduce{_+_}
    }}.flatten

    val gold_partial = (0::tileSize, 0::tileSize){(i,j) => 6.to[Int]}
    printArray(gold, "src:")
    printArray(dst, "dst:")
    printMatrix(gold_partial, "gold partial:")
    printMatrix(portion, "portion:")
    printMatrix(dstpartial, "Full tile from partial accum:")
    println("Probe is " + getArg(probe) + ".  Should equal " + gold(tileSize * tileSize - 1))
    // dst.zip(gold){_==_} foreach {println(_)}
    val cksum = dst.zip(gold){_ == _}.reduce{_&&_} && getArg(probe) == gold(tileSize * tileSize - 1) && gold_partial.zip(portion){_==_}.reduce{_&&_}
    assert(cksum)
    println("PASS: " + cksum + " (BlockReduce2D)")

    //    (0 until tileSize) foreach { i => assert(dst(i) == gold(i)) }
  }
}
