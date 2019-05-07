package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class GatherStore extends SpatialTest {
  val tileSize = 128
  val numAddr = tileSize * 100
  val numData = tileSize * 1000

  val P = param(16)

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
        denseResult(i::i+tileSize par P) store sram
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
    val cksum = (received == gold)
    println("PASS: " + cksum + " (GatherStore)")
    assert(cksum)
  }
}

/*
 * Gather amount is a runtime variable
 * */
@spatial class GatherStore2 extends SpatialTest {
  override def runtimeArgs: Args = "32"

  val tileSize = 32
  val numAddr = tileSize * 2
  val numData = tileSize * 4

  val P = param(16)

  def gatherStore[T:Num](ts:Int, addrs: Array[Int], offchip_data: Array[T]): Array[T] = {

    val B = ArgIn[Int]
    val srcAddrs = DRAM[Int](numAddr)
    val gatherData = DRAM[T](numData)
    val denseResult = DRAM[T](numAddr)

    setArg(B, ts)
    setMem(srcAddrs, addrs)
    setMem(gatherData, offchip_data)

    Accel {
      val addrs = SRAM[Int](tileSize)
      Sequential.Foreach(numAddr by tileSize) { i =>
        val sram = SRAM[T](tileSize)
        addrs load srcAddrs(i::i + tileSize par P)
        sram gather gatherData(addrs par P, B)
        denseResult(i::i+tileSize par P) store sram
      }
    }

    getMem(denseResult)
  }


  def main(args: Array[String]): Unit = {

    val ts = args(0).to[Int]
    val addrs = Array.tabulate[Int](numAddr) { i => 2*i }
    val offchip_data = Array.tabulate[Int](numData){ i => i }

    val received = gatherStore(ts, addrs, offchip_data)

    val gold = Array.tabulate(numAddr){ i => offchip_data(addrs(i)) }

    printArray(gold, "gold:")
    printArray(received, "received:")
    val cksum = (received == gold)
    println("PASS: " + cksum + " (SimpleGatherStore)")
    assert(cksum)
  }
}

// Gather Size == tileSize
@spatial class SimpleGatherStore extends SpatialTest {
  val tileSize = 32
  val numAddr = tileSize * 2
  val numData = tileSize * 4

  val P = param(16)

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
        denseResult(i::i+tileSize par P) store sram
      }
    }

    getMem(denseResult)
  }


  def main(args: Array[String]): Unit = {

    val addrs = Array.tabulate[Int](numAddr) { i => random[Int](tileSize) }
    val offchip_data = Array.tabulate[Int](numData){ i => i }

    val received = gatherStore(addrs, offchip_data)

    val gold = Array.tabulate(numAddr){ i => offchip_data(addrs(i)) }

    printArray(gold, "gold:")
    printArray(received, "received:")
    val cksum = (received == gold)
    println("PASS: " + cksum + " (SimpleGatherStore)")
    assert(cksum)
  }
}

@spatial class SimpleScatter extends SpatialTest {
  val tileSize = 32
  val numAddr = tileSize * 2
  val targetSize = tileSize * 4

  val P = param(16)

  def main(args: Array[String]): Unit = {
    val data = Array.tabulate[Int](numAddr) { i => i }
    val addrs = Array.tabulate[Int](numAddr) { i => random[Int](tileSize) }
    val scatterTarget = Array.tabulate[Int](targetSize){ i => 0 }

    val dataDRAM = DRAM[Int](numAddr)
    val srcAddrs = DRAM[Int](numAddr)
    val scatterDRAM = DRAM[Int](targetSize)

    setMem(dataDRAM, data)
    setMem(srcAddrs, addrs)
    setMem(scatterDRAM, scatterTarget)

    Accel {
      val addrs = SRAM[Int](tileSize)
      val data = SRAM[Int](tileSize)
      Sequential.Foreach(numAddr by tileSize) { i =>
        data load dataDRAM(i::i+tileSize par P)
        addrs load srcAddrs(i::i+tileSize par P)
        scatterDRAM(addrs par P, tileSize) scatter data
      }
    }

    val received = getMem(scatterDRAM)

    val gold = Array.empty[Int](targetSize)
    (0 until targetSize).foreach { i => gold(i) = 0 }
    (0 until numAddr).foreach { i =>
      gold(addrs(i)) = data(i)
    }

    printArray(gold, "gold:")
    printArray(received, "received:")
    val cksum = (received == gold)
    println("PASS: " + cksum + " (SimpleScatter)")
    assert(cksum)
  }
}

