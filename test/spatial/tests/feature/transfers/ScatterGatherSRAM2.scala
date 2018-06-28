package spatial.tests.feature.transfers


import spatial.dsl._


@spatial class ScatterGatherSRAM2 extends SpatialTest {
  override def runtimeArgs: Args = "160"

  val tileSize = 32
  val P = param(1)


  def loadScatter[T:Num](addrs: Array[Int], offchip_data: Array[T], numAddr: Int, numData: Int): Array[T] = {
    val na = ArgIn[Int]
    setArg(na, numAddr)
    val nd = ArgIn[Int]
    setArg(nd, numData)

    val srcAddrs = DRAM[Int](na)
    val inData = DRAM[T](nd)
    val scatterResult = DRAM[T](nd)

    setMem(srcAddrs, addrs)
    setMem(inData, offchip_data)

    Accel {
      val addrs = SRAM[Int](tileSize)
      Sequential.Foreach(na by tileSize) { i =>
        val sram = SRAM[T](tileSize)
        // val numscats = scatgats_per + random[Int](8)
        val numscats = tileSize
        addrs load srcAddrs(i::i + numscats par P)
        sram gather inData(addrs par P, numscats)
        scatterResult(addrs par P, numscats) scatter sram
      }
    }

    getMem(scatterResult)
  }


  def main(args: Array[String]): Unit = {
    val numAddr = args(0).to[Int]
    val mul = 2
    val numData = numAddr*mul*mul

    val nd = numData
    val na = numAddr
    val addrs = Array.tabulate(na) { i => i * mul }
    val offchip_data = Array.tabulate[Int](nd){ i => i * 10 }

    val received = loadScatter(addrs, offchip_data, na,nd)

    def contains(a: Array[Int], elem: Int) = {
      a.map { e => e == elem }.reduce {_||_}
    }

    def indexOf(a: Array[Int], elem: Int) = {
      val indices = Array.tabulate(a.length.to[Int]) { i => i }
      if (contains(a, elem)) {
        a.zip(indices){(e, idx) => if (e == elem) idx else 0 }.reduce{_+_}
      }
      else -1
    }

    val gold = Array.tabulate(nd) { i =>
      if (contains(addrs, i)) offchip_data(i) else 0
    }

    printArray(offchip_data, "data:")
    printArray(addrs, "addrs:")
    printArray(gold, "gold:")
    printArray(received, "received:")
    assert(received == gold)
  }
}