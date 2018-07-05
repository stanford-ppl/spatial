package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class GatherStore extends SpatialTest {
  val tileSize = 128
  val numAddr = tileSize * 100
  val numData = tileSize * 1000

  val P = param(1)

  def gatherStore[T:Num](addrs: Array[Int], offchip_data: Array[T]): Array[T] = {

    val srcAddrs = DRAM[Int](numAddr)
    val gatherData = DRAM[T](numData)
    val denseResult = DRAM[T](numAddr)

    setMem(srcAddrs, addrs)
    setMem(gatherData, offchip_data)

    Accel {
      val addrs = SRAM[Int](tileSize)
      Sequential.Foreach(numAddr by tileSize) { i =>
        val sram = SRAM[T](tileSize)
        addrs load srcAddrs(i::i + tileSize par P)
        sram gather gatherData(addrs par P, tileSize)
        denseResult(i::i+tileSize) store sram
      }
    }

    getMem(denseResult)
  }


  def main(args: Array[String]): Unit = {

    val addrs = Array.tabulate(numAddr) { i =>
      // i*2 // for debug
      // TODO: Macro-virtualized winds up being particularly ugly here..
      if      (i == 4)  199
      else if (i == 6)  numData-2
      else if (i == 7)  191
      else if (i == 8)  203
      else if (i == 9)  381
      else if (i == 10) numData-97
      else if (i == 15) 97
      else if (i == 16) 11
      else if (i == 17) 99
      else if (i == 18) 245
      else if (i == 94) 3
      else if (i == 95) 1
      else if (i == 83) 101
      else if (i == 70) 203
      else if (i == 71) numData-1
      else if (i % 2 == 0) i*2
      else i*2 + numData/2
    }

    val offchip_data = Array.tabulate[Int](numData){ i => i }

    val received = gatherStore(addrs, offchip_data)

    val gold = Array.tabulate(numAddr){ i => offchip_data(addrs(i)) }

    printArray(gold, "gold:")
    printArray(received, "received:")
    assert(received == gold)
  }
}