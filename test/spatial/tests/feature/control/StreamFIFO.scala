package spatial.tests.feature.unit

import spatial.dsl._

@spatial class StreamFIFO extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    type T = I32

    val n = 90
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

    printArray(result, "Result: ")
    printArray(result2, "Result: ")
    println(r"Pass: ${result == result2}")
    assert(result == result2, r"Expected: ${result}, received: ${result2}")
  }
}

@spatial class StreamMultiFIFO extends SpatialTest {
  override def runtimeArgs: Args = "3"

  def main(args: Array[String]): Unit = {
    type T = I32

    val N = ArgIn[Int]
    setArg(N, args(0).to[Int])
    val n = 90
    val p = 16
    val mem = DRAM[T](n)

    Accel {
      val fifo1 = FIFO[T](p)
      val fifo2 = FIFO[T](p)
      val fifo3 = FIFO[T](p)

      Stream.Foreach(N by 1) { i =>
        Foreach(p by 1){j => 
          // Always enq to fifo1
          fifo1.enq(j)
          // Sometimes enq to fifo2
          if (i % 2 == 0) fifo2.enq(j)
        }
        Foreach(0 until p) { _ =>
          val a = fifo1.deq()
          val b = if (i % 2 == 0) fifo2.deq() else 0
          fifo3.enq(a + b)
        }
        mem(0::p) store fifo3
      }

    }

    val result = getMem(mem)
    val gold = Array.tabulate(p){i => if ((args(0).to[Int] - 1) % 2 == 0) i * 2 else i}

    printArray(gold, "Wanted: ")
    printArray(result, "Result: ")
    println(r"Pass: ${gold == result}")
    assert(gold == result)
  }
}
