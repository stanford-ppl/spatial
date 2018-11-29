package spatial.tests.feature.memories.fifo

import spatial.dsl._

@spatial class FIFORing extends SpatialTest {
  override def runtimeArgs: Args = "16"

  val tileSize = 256
  val n = 3

  def main(args: Array[String]): Unit = {
    val numel = args(0).to[Int]
    val N = ArgIn[Int]
    setArg(N, numel)
    val src = Array.tabulate(tileSize){i => i }
    val dram = DRAM[Int](tileSize)
    setMem(dram, src)
    val dummy = DRAM[Int](tileSize)

    val result1 = DRAM[Int](tileSize)
    val result2 = List.tabulate(n){i => DRAM[Int](tileSize)}
    Accel{
      // Ring
      val f1 = FIFO[Int](tileSize)
      f1 load dram
      Foreach(N by 1){i => 
        f1.enq(f1.deq())
      }
      result1 store f1

      // Cross-ring
      val fifs = List.tabulate(n){i => FIFO[Int](tileSize + 1)}
      fifs.foreach(_ load dram)
      Foreach(10 by 1){i => 
        List.tabulate(n){th =>
          val dst = ((th - 1) % n + n) % n
          fifs(dst).enq(fifs(th).deq())
        }
      }
      result2.zip(fifs).foreach(_ store _)

    }


    val gold = Array.tabulate(tileSize){i => (i + numel) % 256}

    printArray(src, "Sent in: ")
    printArray(gold, "Wanted: ")
    printArray(getMem(result1), "Got1: ")
    result2.foreach(printArray(getMem(_), "Got Arr"))

    val cksum1 = getMem(result1).zip(gold){_ == _}.reduce{_&&_}
    val cksum2 = result2.map(getMem(_).zip(gold){_ == _}.reduce{_&&_}).reduce{_&&_}
    println("PASS: " + cksum1 + "," + cksum2 + " (FIFORing)")
    assert(cksum1 && cksum2)
  }
}
