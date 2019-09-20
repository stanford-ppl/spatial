package spatial.tests.feature.memories.fifo

import spatial.dsl._

@spatial class FIFOParRead extends SpatialTest {
  override def runtimeArgs: Args = "384"

  val N = 64

  def main(args: Array[String]): Unit = {
    val in = (0 until N) { i => i }
    val dramIn = DRAM[Int](N)
    val dramOut = DRAM[Int](N)
    Accel {
      val fifoIn = FIFO[Int](N)
      val fifoOut = FIFO[Int](N)
      fifoIn load dramIn(0::0+N)
      Foreach(N by 1 par 2) { j =>
        val v = fifoIn.deq
        val sum = Reg[Int]
        Reduce(sum)(0 until 16) { k => v } { _ + _ }
        fifoOut.enq(sum.value)
      }
      dramOut(0::N) store fifoOut
    }
    val gold = in.map { _ * 16 }
    val cksum = checkGold(dramOut, gold)
    assert(cksum)
  }
}

