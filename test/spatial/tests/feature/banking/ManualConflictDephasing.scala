package spatial.tests.feature.banking

import spatial.dsl._

@spatial class ManualConflictDephasing extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val a = DRAM[Int](16)
    val in = ArgIn[Int]
    setArg(in, 1)
    Accel {
      val aBlk = SRAM[Int](16).conflictable // Should be unbankable without this flag
      Foreach(16 by 1){i => aBlk(i) = 0}
      Foreach(16 by 1 par 2){i => 
        val addr = Reg[Int]
        Pipe{
          sleep(1 + i % 2) // <--- You promised the writes won't conflict, so this is required to get the right answer
          addr := i / in.value
          aBlk(addr.value) = i // You can also put a condition on this write that will only be true for one unrolled lane at at time
        }
      }
      a store aBlk
    }

    printArray(getMem(a), "got:")
    val gold = Array.tabulate(16){i => i.to[Int]}
    printArray(gold, "want:")
    assert(gold == getMem(a))
  }
}
