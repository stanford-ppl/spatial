package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class ScatterMixed extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val size = 128
    val r0 = DRAM[X](size)
    val r1 = DRAM[X](size)
    val r2 = DRAM[X](size)
    type X = FixPt[TRUE, _32, _32] // Float

    val iters = ArgIn[Int]
    val numel = args(0).to[Int]
    setArg(iters, numel)
    Accel {
      // FIFO - FIFO
      val addrs0 = FIFO[Int](size)
      val data0 = FIFO[X](size)
      Foreach(iters by 1){i => 
        addrs0.enq(i)
        data0.enq(i.to[X])
      }
      r0(addrs0) scatter data0

      // SRAM - FIFO
      val addrs1 = SRAM[Int](size)
      val data1 = FIFO[X](size)
      Foreach(iters by 1){i => 
        addrs1(i) = i
        data1.enq(i.to[X])
      }
      r1(addrs1, data1.numel) scatter data1

      // FIFO - SRAM
      val addrs2 = FIFO[Int](size)
      val data2 = SRAM[X](size)
      Foreach(iters by 1){i => 
        addrs2.enq(i)
        data2(i) = i.to[X]
      }
      r2(addrs2) scatter data2


    }
    val gold = Array.tabulate(size){i => if (i < numel) i.to[X] else 0.to[X]}
    printArray(gold, "Gold")
    printArray(getMem(r0), "res0")
    printArray(getMem(r1), "res1")
    printArray(getMem(r2), "res2")

    println(r"Test0: FIFO addrs, FIFO data: ${gold == getMem(r0)}")
    println(r"Test1: SRAM addrs, FIFO data: ${gold == getMem(r1)}")
    println(r"Test2: FIFO addrs, SRAM data: ${gold == getMem(r2)}")
    assert(gold == getMem(r0))
    assert(gold == getMem(r1))
    assert(gold == getMem(r2))
  }

}
