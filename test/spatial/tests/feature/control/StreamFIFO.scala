package spatial.tests.feature.unit

import spatial.dsl._

@spatial class StreamFIFO extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    type T = I32

    val n = 30
    val p = 16
    val mem = DRAM[T](n)
    val mem2 = DRAM[T](n)

    val data = Array.tabulate(n)(i => n - i)
    setMem(mem, data)

    Accel {
      val fifo1 = FIFO[T](p)
      val fifo2 = FIFO[T](p)

      Stream {
        fifo1 load mem(0::n par p)
        Foreach(0 until n par p) { i =>
          fifo2.enq(fifo1.deq())
        }
        mem2(0::n par p) store fifo2
      }

    }

    val result = getMem(mem)
    val result2 = getMem(mem2)
    val result3 = getMem(mem3)

    printArray(result, "Result: ")
    printArray(result2, "Result: ")
    assert(true)
  }
}
