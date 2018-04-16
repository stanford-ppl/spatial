package spatial.test.feature

import spatial.dsl._
import spatial.test.SpatialTest

@spatial object DenseTransfer1D extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val dram256 = DRAM[I32](256)
    val dram128 = DRAM[I32](128)
    val dram64  = DRAM[I32](64)

    Accel {
      val sram1 = SRAM[I32](128)

      sram1 load dram256(0::128)
      dram128 store sram1

      sram1 load dram128(64::128)
      dram64(32::64) store sram1(16::48)

      sram1(17::49) load dram64(0::32)
      dram256(99::131) store sram1(1::17)
    }
  }
}

@spatial object SparseTransfer1D extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val dram256 = DRAM[F32](256)
    val addr128 = DRAM[I32](128)

    Accel {
      val sram1 = SRAM[F32](128)
      val addr  = SRAM[I32](128)

      addr load addr128
      sram1 gather dram256(addr)
    }
  }
}

@spatial object FIFOGather extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  val n = 32
  val T = 8

  def test(dat: Array[Int], adr: Array[Int]): Array[Int] = {
    val N = ArgIn[Int]
    setArg(N, n)
    val data = DRAM[Int](N)
    val addr = DRAM[Int](N)
    val out  = DRAM[Int](N)
    setMem(data, dat)
    setMem(addr, adr)

    Accel {
      val localAddr = FIFO[Int](T)
      val localData = FIFO[Int](T)
      localAddr load addr(0::T)
      localData gather data(localAddr)
      out(0::T) store localData
    }
    getMem(out)
  }

  def main(args: Array[String]): Unit = {
    val addrs = Array.tabulate(n){i => random[Int](n) }
    val datas = Array.tabulate(n){i => random[Int](10) }
    printArray(addrs, "addrs")
    printArray(datas, "datas")
    val out = test(datas, addrs)

    val gold = Array.tabulate(n){i => if (i < T) datas(addrs(i)) else 0 }

    val isCorrect = gold.zip(out){(a,b) => a == b }.reduce{_&&_}
    printArray(out, "out")
    printArray(gold, "gold")
    println("PASS: " + isCorrect)
  }
}

