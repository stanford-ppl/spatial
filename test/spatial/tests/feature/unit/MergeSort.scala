package spatial.tests.feature.unit

import spatial.dsl._

@spatial class MergeSort extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def log(x: scala.Int, base: scala.Int) = Math.round(scala.math.log(x.toDouble) / scala.math.log(base.toDouble)).toInt

  def main(args: Array[String]): Unit = {
    type T = I32

    val n = 4096
    val mem = List.fill(2) { DRAM[T](n) }
    val data = Array.tabulate(n) { i => random[T](n) }
    setMem(mem(0), data)

    Accel {
      val ways = 4
      val mergePar = 16
      val sramBlockSize = 256

      // SRAM block sort
      Pipe {
        val blockSizeInit = mergePar
        val mergeSizeInit = blockSizeInit * ways
        val mergeCountInit = sramBlockSize / mergeSizeInit
        // extra level for base case
        val levelCount = log(sramBlockSize / blockSizeInit, ways) + 1

        val sram = SRAM[T](sramBlockSize)
        val sramMergeBuf = MergeBuffer[T](ways, mergePar)

        val blockSize = Reg[T](blockSizeInit)
        val mergeSize = Reg[T](mergeSizeInit)
        val mergeCount = Reg[T](mergeCountInit)

        Sequential.Foreach(0 until n by sramBlockSize) { blockAddr =>
          val blockRange = blockAddr::blockAddr + sramBlockSize par mergePar
          sram load mem(0)(blockRange)
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
          mem(0)(blockRange) store sram
          blockSize := blockSizeInit
          mergeSize := mergeSizeInit
          mergeCount := mergeCountInit
        }
      }

      // off-chip DRAM sort
      Pipe {
        val blockSizeInit = sramBlockSize
        val mergeSizeInit = blockSizeInit * ways
        val mergeCountInit = n / mergeSizeInit
        // extra level for base case
        val levelCount = log(n / blockSizeInit, ways)

        val doubleBuf = Reg[Boolean]
        doubleBuf := true
        val fifos = List.fill(ways) { FIFO[T](n) }
        val mergeBuf = MergeBuffer[T](ways, mergePar)

        val blockSize = Reg[T](blockSizeInit)
        val mergeSize = Reg[T](mergeSizeInit)
        val mergeCount = Reg[T](mergeCountInit)

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
                  f load mem(0)(lAddr)
                } else {
                  f load mem(1)(lAddr)
                }
                Foreach(0 until blockSize par mergePar) { j =>
                  mergeBuf.enq(i, f.deq())
                }
              }
              val sAddr = addr(0)::addr(0) + mergeSize par mergePar
              if (doubleBuf) {
                mem(1)(sAddr) store mergeBuf
              } else {
                mem(0)(sAddr) store mergeBuf
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

