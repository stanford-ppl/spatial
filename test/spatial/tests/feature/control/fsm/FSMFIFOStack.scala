package spatial.tests.feature.control.fsm

import spatial.dsl._

@spatial class FSMFIFOStack extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val size = 128
    val fifo_sum = ArgOut[Int]
    val fifo_sum_almost = ArgOut[Int]
    val fifo_last = ArgOut[Int]
    val stack_sum = ArgOut[Int]
    val stack_sum_almost = ArgOut[Int]
    val stack_last = ArgOut[Int]
    val init = 0
    val fill = 1
    val drain = 2
    val done = 3

    Accel {
      val fifo = FIFO[Int](size)
      val fifo_accum = Reg[Int](0)
      // Using done/empty
      FSM(0)(state => state != done) { state =>
        if (state == init || state == fill) {
          fifo.enq(fifo.numel)
        } else {
          Pipe{
            val f = fifo.deq()
            fifo_accum := fifo_accum + f
            fifo_last := f
          }
        }
      } { state => mux(state == 0, fill, mux(fifo.isFull && state == fill, drain, mux(fifo.isEmpty && state == drain, done, state))) }
      fifo_sum := fifo_accum

      // Using almostDone/almostEmpty, skips last 2 elements
      val fifo_almost = FIFO[Int](size)
      val fifo_accum_almost = Reg[Int](0)
      FSM(0)(state => state != done) { state =>
        if (state == init || state == fill) {
          fifo_almost.enq(fifo_almost.numel)
        } else {
          Pipe{
            fifo_accum_almost := fifo_accum_almost + fifo_almost.deq()
          }
        }
      } { state => mux(state == 0, fill, mux(fifo_almost.isAlmostFull && state == fill, drain, mux(fifo_almost.isAlmostEmpty && state == drain, done, state))) }
      fifo_sum_almost := fifo_accum_almost

      val stack = LIFO[Int](size)
      val stack_accum = Reg[Int](0)
      // Using done/empty
      FSM(0)(state => state != done) { state =>
        if (state == init || state == fill) {
          stack.push(stack.numel)
        } else {
          Pipe{
            val f = stack.pop()
            stack_accum := stack_accum + f
            stack_last := f
          }
        }
      } { state => mux(state == 0, fill, mux(stack.isFull && state == fill, drain, mux(stack.isEmpty && state == drain, done, state))) }
      stack_sum := stack_accum

      // Using almostDone/almostEmpty, skips last element
      val stack_almost = LIFO[Int](size)
      val stack_accum_almost = Reg[Int](0)
      FSM(0)(state => state != done) { state =>
        if (state == init || state == fill) {
          stack_almost.push(stack_almost.numel)
        } else {
          Pipe{
            Pipe{
              val x = stack_almost.peek
              stack_accum_almost := stack_accum_almost + x
            }
            Pipe{stack_almost.pop()}
          }
        }
      } { state => mux(state == 0, fill, mux(stack_almost.isAlmostFull && state == fill, drain, mux(stack_almost.isAlmostEmpty && state == drain, done, state))) }
      stack_sum_almost := stack_accum_almost

    }

    val fifo_sum_res = getArg(fifo_sum)
    val fifo_sum_gold = Array.tabulate(size) {i => i}.reduce{_+_}
    val fifo_sum_almost_res = getArg(fifo_sum_almost)
    val fifo_sum_almost_gold = Array.tabulate(size-2) {i => i}.reduce{_+_}
    val fifo_last_res = getArg(fifo_last)
    val fifo_last_gold = size-1
    val stack_sum_res = getArg(stack_sum)
    val stack_sum_gold = Array.tabulate(size) {i => i}.reduce{_+_}
    val stack_last_res = getArg(stack_last)
    val stack_last_gold = 0
    val stack_sum_almost_res = getArg(stack_sum_almost)
    val stack_sum_almost_gold = Array.tabulate(size-1) {i => i}.reduce{_+_}

    println("FIFO: Sum-")
    println("  Expected " + fifo_sum_gold)
    println("       Got " + fifo_sum_res)
    println("FIFO: Alternate Sum-")
    println("  Expected " + fifo_sum_almost_gold)
    println("       Got " + fifo_sum_almost_res)
    println("FIFO: Last out-")
    println("  Expected " + fifo_last_gold)
    println("       Got " + fifo_last_res)
    println("")
    println("Stack: Sum-")
    println("  Expected " + stack_sum_gold)
    println("       Got " + stack_sum_res)
    println("Stack: Alternate Sum-")
    println("  Expected " + stack_sum_almost_gold)
    println("       Got " + stack_sum_almost_res)
    println("Stack: Last out-")
    println("  Expected " + stack_last_gold)
    println("       Got " + stack_last_res)

    val cksum1 = fifo_sum_gold == fifo_sum_res
    val cksum2 = fifo_last_gold == fifo_last_res
    val cksum3 = stack_sum_gold == stack_sum_res
    val cksum4 = stack_last_gold == stack_last_res
    val cksum5 = fifo_sum_almost_gold == fifo_sum_almost_res
    val cksum6 = stack_sum_almost_gold == stack_sum_almost_res
    val cksum = cksum1 && cksum2 && cksum3 && cksum4 && cksum5 && cksum6
    println("PASS: " + cksum + " (FifoStackFSM)")
    assert(cksum)
  }
}
