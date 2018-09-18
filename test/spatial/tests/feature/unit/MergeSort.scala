package spatial.tests.feature.unit

import spatial.dsl._

@spatial class MergeSort extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    type T = I32

    val n = 128

    val mem = DRAM[T](n)
    val memBuffer = DRAM[T](n)

    val data = Array.tabulate(n)(i => n - i)
    setMem(mem, data)
    setMem(memBuffer, data)

    /*
    Accel {
      val sramBlockSize = 64
      val ways = 2
      val mergePar = 16
      val mergeSizeInit = mergePar * ways
      val mergeCountInit = sramBlockSize / mergeSizeInit
      // extra level for base case
      val levelCount = (sramBlockSize / mergeSizeInit) + 1

      val doubleBuf = Reg[Boolean]
      doubleBuf := true
      val srams = List.fill(2) { SRAM[T](sramBlockSize) }
      val mergeBuf = MergeBuffer[T](ways, mergePar)

      val blockSize = Reg[T](mergePar)
      val mergeSize = Reg[T](mergeSizeInit)
      val mergeCount = Reg[T](sramBlockSize / mergeSizeInit)

      Sequential.Foreach(0 until n by sramBlockSize) { blockAddr =>
        val blockRange = blockAddr::blockAddr + sramBlockSize par mergePar
        srams(0) load mem(blockRange)
        Sequential.Foreach(0 until levelCount) { level =>
          val initMerge = (level == 0)
          //Pipe { mergeBuf.init(initMerge) }
          Sequential.Foreach(0 until mergeCount) { block =>
            //val addr = List.tabulate(ways) { i => (block * mergeSize) + (i * blockSize) }
            Pipe { mergeBuf.bound(0, blockSize) }
            Pipe { mergeBuf.bound(1, blockSize) }
            /*
            addr.zipWithIndex.foreach { case (a, i) =>
              Pipe { mergeBuf.bound(i, blockSize) }
            }
            */
            if (doubleBuf) {
              Stream {
                Foreach(0 until blockSize par mergePar) { j =>
                  mergeBuf.enq(0, srams(0)(0 + j))
                }
                Foreach(0 until blockSize par mergePar) { j =>
                  mergeBuf.enq(1, srams(0)(mergePar + j))
                }
                Foreach(0 until mergeSize par mergePar) { j =>
                  srams(1)(0 + j) = mergeBuf.deq()
                }
              }
            } else {
              Stream {
                Foreach(0 until blockSize par mergePar) { j =>
                  mergeBuf.enq(0, srams(1)(0 + j))
                }
                Foreach(0 until blockSize par mergePar) { j =>
                  mergeBuf.enq(1, srams(1)(mergePar + j))
                }
                Foreach(0 until mergeSize par mergePar) { j =>
                  srams(0)(0 + j) = mergeBuf.deq()
                }
              }
            }
          }
          Pipe {
            if (!initMerge) {
              blockSize := blockSize * ways
              mergeSize := mergeSize * ways
              mergeCount := mergeCount / ways
            }
            doubleBuf := !doubleBuf
          }
        }
        mem(blockRange) store srams(0)
      }
    }
    */

    Accel {
      val ways = 2
      val mergePar = 16
      val mergeSizeInit = mergePar * ways
      val mergeCountInit = n / mergeSizeInit
      // extra level for base case
      val levelCount = (n / mergeSizeInit) + 1

      val doubleBuf = Reg[Boolean]
      doubleBuf := true
      val fifos = List.fill(ways) { FIFO[T](n) }
      val mergeBuf = MergeBuffer[T](ways, mergePar)

      val blockSize = Reg[T](mergePar)
      val mergeSize = Reg[T](mergeSizeInit)
      val mergeCount = Reg[T](mergeCountInit)
      Sequential.Foreach(0 until levelCount) { level =>
        val initMerge = (level == 0)
        Pipe { mergeBuf.init(initMerge) }
        Sequential.Foreach(0 until mergeCount) { block =>
          fifos.zipWithIndex.foreach { case (f, i) =>
            Pipe { mergeBuf.bound(i, blockSize) }
          }
          val addr = List.tabulate(ways) { i => (block * mergeSize) + (i * blockSize) }
          Stream {
            fifos.zipWithIndex.foreach { case (f, i) =>
              val lAddr = addr(i)::addr(i) + blockSize par mergePar
              if (doubleBuf) {
                f load mem(lAddr)
              } else {
                f load memBuffer(lAddr)
              }
              Foreach(0 until blockSize par mergePar) { j =>
                mergeBuf.enq(i, f.deq())
              }
            }
            val sAddr = addr(0)::addr(0) + mergeSize par mergePar
            if (doubleBuf) {
              memBuffer(sAddr) store mergeBuf
            } else {
              mem(sAddr) store mergeBuf
            }
          }
        }
        Pipe {
          if (!initMerge) {
            blockSize := blockSize * ways
            mergeSize := mergeSize * ways
            mergeCount := mergeCount / ways
          }
          doubleBuf := !doubleBuf
        }
      }
    }

    val result = getMem(memBuffer)
    val result2 = getMem(mem)
    val cksum1 = Array.tabulate(n-1){i => result(i+1) >= result(i)}.reduce{_&&_}
    val cksum2 = Array.tabulate(n-1){i => result2(i+1) >= result2(i)}.reduce{_&&_}

    printArray(result, r"Result: (Sorted? $cksum1)")
    printArray(result2, r"Result2: (Sorted? $cksum2)")
    println("One buffer must be sorted to pass")
    assert(cksum1 || cksum2)
  }
}

