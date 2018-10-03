package spatial.tests.feature.memories.dram

import spatial.dsl._

@spatial class DRAMAccelTest extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val n = 100
    val data = Array.tabulate(n) { i => i }
    val hostDRAM = DRAM[Int](n)
    setMem(hostDRAM, data)
    val size = random[Int](n)

    Accel {
      val accelDRAM = DRAM1[Int]
      val sram = SRAM[Int](n)
      sram(0::size) load hostDRAM(0::size)

      accelDRAM.alloc(size)
      accelDRAM(0::size) store sram(0::size)
      sram(0::size) load accelDRAM(0::size)
      accelDRAM.dealloc
      hostDRAM(0::size) store sram(0::size)
    }

    val out = getMem(hostDRAM)
    println(out)
    assert(out.zip(data) { (a,b) => a == b }.reduce{_&&_})
  }
}
