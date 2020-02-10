package spatial.tests.feature.control

import spatial.dsl._

@spatial class ScanLoop extends SpatialTest {
  override def runtimeArgs: Args = "32"
  type T = FixPt[TRUE, _16, _16]

  def main(args: Array[String]): Unit = {

    val dram = DRAM[Int](512)

    Accel {
      val sram = SRAM[Int](512)
      Foreach(512 by 1) { i => sram(i) = -1 }

      val bv = Reg[U512]
      bv := 0xF0A5.to[U512]
      Foreach(Scan(bv)) { i => sram(i) = i }

      dram store sram
    }

    printArray(getMem(dram), "Got: ")
  }
}

@spatial class SplitterLoop extends SpatialTest {
  override def runtimeArgs: Args = "32"
  type T = FixPt[TRUE, _16, _16]

  def main(args: Array[String]): Unit = {

    val dram = DRAM[T](512)

    def func(i: Int): T = 2.to[T] * i.to[T]
    Accel {
      val sram = SRAM[T](512)
      Foreach(512 by 1) { i => sram(i) = -1 }

      Foreach(512 by 1 par 16) { i =>
        val addrs = i * 3 % 7
        splitter(addrs) {
          sram(addrs) = func(i)
        }

        val more_addrs = i * 5 % 9
        splitter(more_addrs) {
          sram(more_addrs) = func(i)
        }
      }

      dram store sram

    }

    printArray(getMem(dram), "Got: ")
  }
}