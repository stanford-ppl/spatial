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
      Foreach(Scan(bv.value)) { i => sram(i) = i }

      dram store sram
    }

    printArray(getMem(dram), "Got: ")
    assert(true)
  }
}

@spatial class MultiScanLoop extends SpatialTest {
  override def runtimeArgs: Args = "32"
  type T = FixPt[TRUE, _16, _16]

  def main(args: Array[String]): Unit = {

    val dram = DRAM[Int](512)

    Accel {
      val sram = SRAM[Int](512)
      Foreach(512 by 1) { i => sram(i) = -1 }

      val bv1 = Reg[U512]
      bv1 := 0xF0A5.to[U512]
      val bv2 = Reg[U512]
      bv2 := 0x7183.to[U512]
      Foreach(Scan(par = 16, bv1.value, bv2.value)) { case List(a,b) => if (a != -1 && b != -1) sram(a) = a + b }

      dram store sram
    }

    printArray(getMem(dram), "Got: ")
    assert(true)
  }
}


@spatial class SplitterLoop extends SpatialTest {
  override def runtimeArgs: Args = "32"
  //type T = FixPt[TRUE, _16, _16]
  type T = Int

  def main(args: Array[String]): Unit = {

    val dram = DRAM[T](512)

    Accel {
      // disable banking on dense sram is causing issue. In general dense memory cannot be accessed
      // within splitter because they cannot be banked
      //val sram = SparseSRAM[T](512)
      //Foreach(512 by 1) { i => sram(i) = -1 }

      val sram = SRAM[T](512)
      Foreach(512 by 1 par 16) { i =>
        val addrs = i * 3 % 7
        splitter(addrs) {
          sram(i) = i.to[T] // CSE causing weird behavior. The FixToFix on i is passed from the first splitter to the second splitter
        }

        //val more_addrs = i * 5 % 9
        //splitter(more_addrs) {
          //sram(more_addrs) = func(i)
        //}
      }
      dram store sram

    }

    printArray(getMem(dram), "Got: ")
    assert(true)
  }
}
