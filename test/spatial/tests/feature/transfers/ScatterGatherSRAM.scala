package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class ScatterGatherSRAM extends SpatialTest {
  override def runtimeArgs: Args = "1536"
  val N = 1920

  val tileSize = 384
  val P = param(1)

  def scattergather[T:Num](addrs: Array[Int], offchip_data: Array[T], size: Int, dataSize: Int): Array[T] = {
    val srcAddrs = DRAM[Int](size)
    val gatherData = DRAM[T](dataSize)
    val scatterResult = DRAM[T](dataSize)

    setMem(srcAddrs, addrs)
    setMem(gatherData, offchip_data)

    Accel {
      val addrs = SRAM[Int](tileSize)
      Sequential.Foreach(size by tileSize) { i =>
        val sram = SRAM[T](tileSize)
        addrs load srcAddrs(i::i + tileSize par P)
        sram gather gatherData(addrs par P, tileSize)
        scatterResult(addrs par P, tileSize) scatter sram // TODO: What to do about parallel scatter when sending to same burst simultaneously???
      }
    }

    getMem(scatterResult)
  }


  def main(args: Array[String]): Unit = {
    val size = args(0).to[Int]

    // val size = maxNumAddrs
    val dataSize = size*6
    val addrs = Array.tabulate(size) { i =>
      // i*2 // for debug
      if      (i == 4)  199
      else if (i == 6)  dataSize-2
      else if (i == 7)  191
      else if (i == 8)  203
      else if (i == 9)  381
      else if (i == 10) dataSize-97
      else if (i == 15) 97
      else if (i == 16) 11
      else if (i == 17) 99
      else if (i == 18) 245
      else if (i == 94) 3
      else if (i == 95) 1
      else if (i == 83) 101
      else if (i == 70) 203
      else if (i == 71) dataSize-1
      else if (i % 2 == 0) i*2
      else i*2 + dataSize/2
    }
    val offchip_data = Array.fill(dataSize){ random[Int](dataSize) }
    // val offchip_data = Array.tabulate (dataSize) { i => i}

    val received = scattergather(addrs, offchip_data, size, dataSize)

    // printArr(addrs, "addrs: ")
    // (0 until dataSize) foreach { i => println(i + " match? " + (addrs.map{a => a==i}.reduce{_||_}) ) }
    val gold = Array.tabulate(dataSize){ i => if (addrs.map{a => a == i}.reduce{_||_}) offchip_data(i) else 0 }

    printArray(gold, "gold:")
    printArray(received, "received:")
    val cksum = received.zip(gold){_ == _}.reduce{_&&_}
    println("PASS: " + cksum + " (ScatterGather)")
    assert(cksum)
  }
}
