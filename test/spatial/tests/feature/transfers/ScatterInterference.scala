package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class ScatterInterference extends SpatialTest {
  type T = Int8
  def main(args: Array[String]): Unit = {
    val data_dram = DRAM[T](128)
    setMem(data_dram, Array.fill(128)(-1.to[T]))
    val dummy = ArgOut[Int]
    val lots_of_stores = DRAM[Int](16)

    Accel {
      val s = SRAM[Int](16)
      Foreach(16 by 1){i => s(i) = i}
      lots_of_stores store s
      lots_of_stores store s
      lots_of_stores store s
      lots_of_stores store s
      lots_of_stores store s
      lots_of_stores store s
      lots_of_stores store s
      lots_of_stores store s
      lots_of_stores store s
      lots_of_stores store s
      lots_of_stores store s
      Parallel{
        Pipe {
          val addrs1 = FIFO[Int](8)
          val data1 = FIFO[T](8)
          Pipe{
            addrs1.enq(0)
            data1.enq(5)
          }
          data_dram(addrs1) scatter data1
        }
        Pipe {
          val addrs2 = FIFO[Int](8)
          val data2 = FIFO[T](8)
          Pipe{dummy := 3}
          Pipe{addrs2.enq(65)}
          Pipe{data2.enq(9)}
          data_dram(addrs2) scatter data2
        }
      }
    }
    println(r"${getArg(dummy)}")
    val gold = Array.tabulate(128){i => if (i == 0) 5.to[T] else if (i == 65) 9.to[T] else -1}
    val got = getMem(data_dram)
    printArray(gold, "gold")
    printArray(got, "got")
    assert(got == gold)

  }
}
