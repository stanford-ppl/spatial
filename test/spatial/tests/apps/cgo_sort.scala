import spatial.dsl._

@spatial class sort_1 extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    def log(x: scala.Int, base: scala.Int) = Math.round(scala.math.log(x.toDouble) / scala.math.log(base.toDouble)).toInt
    type T = I32

    val n = 4096
    val mem = List.fill(2) { DRAM[T](n) }
    val data = Array.tabulate(n) { i => random[T](n) }
    setMem(mem(0), data)

    val mergePar = 16
    val ways = 2
    val numel = n
    val src = mem(0)
    val dst = mem(1)

    Accel {


      val sramBlockSize = 256

      // SRAM block sort
      Pipe {
        val sram = SRAM[T](sramBlockSize).flat.effort(0)
        Sequential.Foreach(0 until numel by sramBlockSize) { blockAddr =>
          val blockRange = blockAddr::blockAddr + sramBlockSize par mergePar
          sram load src(blockRange)
          // mergeSort(sram, mergePar, ways)
          // {
            val blockSizeInit = mergePar
            val mergeSizeInit = blockSizeInit * ways
            val mergeCountInit = sramBlockSize / mergeSizeInit
            // extra level for base case
            val levelCount = log(sramBlockSize / blockSizeInit, ways) + 1

            val sramMergeBuf = MergeBuffer[T](ways, mergePar)

            val blockSize = Reg[I32](blockSizeInit)
            val mergeSize = Reg[I32](mergeSizeInit)
            val mergeCount = Reg[I32](mergeCountInit)

            Sequential.Foreach(0 until levelCount) { level =>
              val initMerge = (level == 0)
              Pipe { sramMergeBuf.init(initMerge) }
              Sequential.Foreach(0 until mergeCount) { block =>
                List.tabulate(ways) { i => i }.foreach { case i =>
                  Pipe { sramMergeBuf.bound(i, blockSize) }
                }
                val addr = List.tabulate(ways) { i => (block * mergeSize) + (i * blockSize) }
                addr.zipWithIndex.foreach { case (a, i) =>
                  Foreach(0 until blockSize par mergePar) { j =>
                    sramMergeBuf.enq(i, sram(a + j))
                  }
                }
                Stream {
                  Foreach(0 until mergeSize par mergePar) { j =>
                    sram(addr(0) + j) = sramMergeBuf.deq()
                  }
                }
              }
              Pipe {
                if (!initMerge) {
                  blockSize := blockSize * ways
                  mergeSize := mergeSize * ways
                  mergeCount := mergeCount / ways
                }
              }
            }

            blockSize := blockSizeInit
            mergeSize := mergeSizeInit
            mergeCount := mergeCountInit
          // }
          src(blockRange) store sram
        }
      }

      // off-chip DRAM sort
      Pipe {
        val blockSizeInit = sramBlockSize
        val mergeSizeInit = blockSizeInit * ways
        val mergeCountInit = numel / mergeSizeInit
        // extra level for base case
        val levelCount = log(numel / blockSizeInit, ways)

        val doubleBuf = Reg[Boolean]
        doubleBuf := true
        val fifos = List.fill(ways) { FIFO[T](numel) }
        val mergeBuf = MergeBuffer[T](ways, mergePar)

        val blockSize = Reg[I32](blockSizeInit)
        val mergeSize = Reg[I32](mergeSizeInit)
        val mergeCount = Reg[I32](mergeCountInit)

        Sequential.Foreach(0 until levelCount) { level =>
          Sequential.Foreach(0 until mergeCount) { block =>
            fifos.zipWithIndex.foreach { case (f, i) =>
              Pipe { mergeBuf.bound(i, blockSize) }
            }
            val addr = List.tabulate(ways) { i => (block * mergeSize) + (i * blockSize) }
            Stream {
              fifos.zipWithIndex.foreach { case (f, i) =>
                val lAddr = addr(i)::addr(i) + blockSize par mergePar
                if (doubleBuf) {
                  f load src(lAddr)
                } else {
                  f load dst(lAddr)
                }
                Foreach(0 until blockSize par mergePar) { j =>
                  mergeBuf.enq(i, f.deq())
                }
              }
              val sAddr = addr(0)::addr(0) + mergeSize par mergePar
              if (doubleBuf) {
                dst(sAddr) store mergeBuf
              } else {
                src(sAddr) store mergeBuf
              }
            }
          }
          Pipe {
            blockSize := blockSize * ways
            mergeSize := mergeSize * ways
            mergeCount := mergeCount / ways
            doubleBuf := !doubleBuf
          }
        }
      }
    
    }

    val result = getMem(mem(1))
    val result2 = getMem(mem(0))
    val cksum1 = Array.tabulate(n-1){i => result(i+1) >= result(i)}.reduce{_&&_}
    val cksum2 = Array.tabulate(n-1){i => result2(i+1) >= result2(i)}.reduce{_&&_}

    printArray(result, r"Result: (Sorted? $cksum1)")
    printArray(result2, r"Result2: (Sorted? $cksum2)")
    println("One buffer must be sorted to pass")
    assert(cksum1 || cksum2)
  }
}

@spatial class sort_2 extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    def log(x: scala.Int, base: scala.Int) = Math.round(scala.math.log(x.toDouble) / scala.math.log(base.toDouble)).toInt
    type T = I32

    val n = 4096
    val mem = List.fill(2) { DRAM[T](n) }
    val data = Array.tabulate(n) { i => random[T](n) }
    setMem(mem(0), data)

    val mergePar = 16
    val ways = 2
    val numel = n
    val src = mem(0)
    val dst = mem(1)

    Accel {


      val sramBlockSize = 256

      // SRAM block sort
      Pipe {
        val sram = SRAM[T](sramBlockSize).flat.axesfission(List(List(0))).effort(0)
        Sequential.Foreach(0 until numel by sramBlockSize) { blockAddr =>
          val blockRange = blockAddr::blockAddr + sramBlockSize par mergePar
          sram load src(blockRange)
          // mergeSort(sram, mergePar, ways)
          // {
            val blockSizeInit = mergePar
            val mergeSizeInit = blockSizeInit * ways
            val mergeCountInit = sramBlockSize / mergeSizeInit
            // extra level for base case
            val levelCount = log(sramBlockSize / blockSizeInit, ways) + 1

            val sramMergeBuf = MergeBuffer[T](ways, mergePar)

            val blockSize = Reg[I32](blockSizeInit)
            val mergeSize = Reg[I32](mergeSizeInit)
            val mergeCount = Reg[I32](mergeCountInit)

            Sequential.Foreach(0 until levelCount) { level =>
              val initMerge = (level == 0)
              Pipe { sramMergeBuf.init(initMerge) }
              Sequential.Foreach(0 until mergeCount) { block =>
                List.tabulate(ways) { i => i }.foreach { case i =>
                  Pipe { sramMergeBuf.bound(i, blockSize) }
                }
                val addr = List.tabulate(ways) { i => (block * mergeSize) + (i * blockSize) }
                addr.zipWithIndex.foreach { case (a, i) =>
                  Foreach(0 until blockSize par mergePar) { j =>
                    sramMergeBuf.enq(i, sram(a + j))
                  }
                }
                Stream {
                  Foreach(0 until mergeSize par mergePar) { j =>
                    sram(addr(0) + j) = sramMergeBuf.deq()
                  }
                }
              }
              Pipe {
                if (!initMerge) {
                  blockSize := blockSize * ways
                  mergeSize := mergeSize * ways
                  mergeCount := mergeCount / ways
                }
              }
            }

            blockSize := blockSizeInit
            mergeSize := mergeSizeInit
            mergeCount := mergeCountInit
          // }
          src(blockRange) store sram
        }
      }

      // off-chip DRAM sort
      Pipe {
        val blockSizeInit = sramBlockSize
        val mergeSizeInit = blockSizeInit * ways
        val mergeCountInit = numel / mergeSizeInit
        // extra level for base case
        val levelCount = log(numel / blockSizeInit, ways)

        val doubleBuf = Reg[Boolean]
        doubleBuf := true
        val fifos = List.fill(ways) { FIFO[T](numel) }
        val mergeBuf = MergeBuffer[T](ways, mergePar)

        val blockSize = Reg[I32](blockSizeInit)
        val mergeSize = Reg[I32](mergeSizeInit)
        val mergeCount = Reg[I32](mergeCountInit)

        Sequential.Foreach(0 until levelCount) { level =>
          Sequential.Foreach(0 until mergeCount) { block =>
            fifos.zipWithIndex.foreach { case (f, i) =>
              Pipe { mergeBuf.bound(i, blockSize) }
            }
            val addr = List.tabulate(ways) { i => (block * mergeSize) + (i * blockSize) }
            Stream {
              fifos.zipWithIndex.foreach { case (f, i) =>
                val lAddr = addr(i)::addr(i) + blockSize par mergePar
                if (doubleBuf) {
                  f load src(lAddr)
                } else {
                  f load dst(lAddr)
                }
                Foreach(0 until blockSize par mergePar) { j =>
                  mergeBuf.enq(i, f.deq())
                }
              }
              val sAddr = addr(0)::addr(0) + mergeSize par mergePar
              if (doubleBuf) {
                dst(sAddr) store mergeBuf
              } else {
                src(sAddr) store mergeBuf
              }
            }
          }
          Pipe {
            blockSize := blockSize * ways
            mergeSize := mergeSize * ways
            mergeCount := mergeCount / ways
            doubleBuf := !doubleBuf
          }
        }
      }
    
    }

    val result = getMem(mem(1))
    val result2 = getMem(mem(0))
    val cksum1 = Array.tabulate(n-1){i => result(i+1) >= result(i)}.reduce{_&&_}
    val cksum2 = Array.tabulate(n-1){i => result2(i+1) >= result2(i)}.reduce{_&&_}

    printArray(result, r"Result: (Sorted? $cksum1)")
    printArray(result2, r"Result2: (Sorted? $cksum2)")
    println("One buffer must be sorted to pass")
    assert(cksum1 || cksum2)
  }
}

