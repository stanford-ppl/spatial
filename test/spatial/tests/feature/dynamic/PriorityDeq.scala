package spatial.tests.feature.dynamic

import spatial.dsl._

class PriorityDeqStatic extends PriorityDeq(false)
class PriorityDeqDynamic extends PriorityDeq(true)

@spatial abstract class PriorityDeq(dyn: scala.Boolean) extends SpatialTest {
  override def runtimeArgs: Args = "48"

  def main(args: Array[String]): Unit = {
    val N = ArgIn[Int]
    setArg(N, args(0).to[Int])

    val result = DRAM[Int](N*3)
    setMem(result, Array.fill(N*3)(-1.to[Int]))

    Accel {
      // Create worker worklist queues
      val worker1Queue = FIFO[Int](16)
      val worker2Queue = FIFO[Int](16)
      val worker3Queue = FIFO[Int](16)
      val doneQueue = FIFO[Int](2)

      val outQueue = FIFO[Int](128)


      if (!dyn) {
        Stream {
          // Producer
          Foreach(N by 1) { i =>
            val cmd = random[Int](6)
            if (cmd % 3 == 0)
              worker1Queue.enq(i)
            if (cmd % 3 == 1)
              worker2Queue.enq(i)
            if (cmd % 3 == 2)
              worker3Queue.enq(i)
          }

          // Consumer
          Foreach(N by 1) { i =>
            val id = priorityDeq(worker1Queue, worker2Queue, worker3Queue)
            outQueue.enq(id)
          }
        }
      } else {
        val kill = Reg[Bit](false)
        Stream(breakWhen = kill).Foreach(*) { _ =>
          // Producer
          Pipe { // Pipe this off to prevent strangeness with the N.value + 1 orphaned primitive
            Foreach(N.value + 1 by 1) { i =>
              if (i < N) {
                // Every i gets enq'ed either ONCE ore 3 TIMES
                val cmd = random[Int](6)
                if (cmd % 3 == 0)
                  worker1Queue.enq(i)
                if (cmd % 3 == 1 || cmd == 0)
                  worker2Queue.enq(i)
                if (cmd % 3 == 2 || cmd == 0)
                  worker3Queue.enq(i)
              }
              retimeGate()
              doneQueue.enq(-1, i >= N.value)
            }
          }

          // Consumer
          Foreach(*) { i =>
            val id = priorityDeq(worker1Queue, worker2Queue, worker3Queue, doneQueue)
            if (id != -1) outQueue.enq(id)
            else kill := Bit(true)
          }
        }
      }

      result(0::outQueue.numel) store outQueue
    }

    val got = getMem(result)
    printArray(got, "Got data:")
    for (i <- 0 until N) {
      val count = got.map{x => if (x == i) 1 else 0}.reduce{_+_}
      if (count == 3) println(r"Found triple enq for $i! :D")
      assert(count == 1 || count == 3, r"Incorrect count for packet $i!  Count = $count")
    }

  }
}

class MultiPriorityDeqStatic extends MultiPriorityDeq(false)
class MultiPriorityDeqDynamic extends MultiPriorityDeq(true)
@spatial abstract class MultiPriorityDeq(dyn: scala.Boolean) extends SpatialTest {
  override def runtimeArgs: Args = "48"

  def main(args: Array[String]): Unit = {
    val N = ArgIn[Int]
    setArg(N, args(0).to[Int])

    val result = DRAM[Int](N*3)
    setMem(result, Array.fill(N*3)(-1.to[Int]))

    Accel {
      // Create worker worklist queues.
      // The priorityDeq should not deq from a lane unless the payload queue also has data, or else this causes a hang!
      val worker1Queue = FIFO[Int](16)
      val worker1PayloadQueue = FIFO[Int](16)
      val worker2Queue = FIFO[Int](16)
      val worker2PayloadQueue = FIFO[Int](16)
      val worker3Queue = FIFO[Int](16)
      val worker3PayloadQueue = FIFO[Int](16)
      val doneQueue = FIFO[Int](2)

      val outQueue = FIFO[Int](128)


      if (!dyn) {
        Stream {
          // Producer
          Foreach(N by 1) { i =>
            val cmd = random[Int](6)
            if (cmd % 3 == 0) {
              worker1Queue.enq(0)
              Foreach(20 by 1) { j =>
                if (j == 19) worker1PayloadQueue.enq(j)
              }
            }
            if (cmd % 3 == 1) {
              worker2Queue.enq(1)
              Foreach(20 by 1) { j =>
                if (j == 19) worker1PayloadQueue.enq(j)
              }
            }
            if (cmd % 3 == 2) {
              worker3Queue.enq(2)
              Foreach(20 by 1) { j =>
                if (j == 19) worker1PayloadQueue.enq(j)
              }
            }
          }

          // Consumer
          Foreach(N by 1) { i =>
            val id = priorityDeq(List(worker1Queue, worker2Queue, worker3Queue), List(!worker1PayloadQueue.isEmpty, !worker2PayloadQueue.isEmpty, !worker3PayloadQueue.isEmpty))
            val payload = if (id == 0) worker1PayloadQueue.deq() else if (id == 1) worker2PayloadQueue.deq() else if (id == 2) worker3PayloadQueue.deq() else 0.to[Int]
            outQueue.enq(id + payload)
          }
        }
      } else {
        val kill = Reg[Bit](false)
        Stream(breakWhen = kill).Foreach(*) { _ =>
          // Producer
          Pipe { // Pipe this off to prevent strangeness with the N.value + 1 orphaned primitive
            Foreach(N.value + 1 by 1) { i =>
              if (i < N) {
                // Every i gets enq'ed either ONCE ore 3 TIMES
                val cmd = random[Int](6)
                if (cmd % 3 == 0) {
                  worker1Queue.enq(i)
                  Foreach(20 by 1) { j =>
                    if (j == 19) worker1PayloadQueue.enq(j)
                  }
                }
                if (cmd % 3 == 1 || cmd == 0) {
                  worker2Queue.enq(i)
                  Foreach(20 by 1) { j =>
                    if (j == 19) worker1PayloadQueue.enq(j)
                  }
                }
                if (cmd % 3 == 2 || cmd == 0) {
                  worker3Queue.enq(i)
                  Foreach(20 by 1) { j =>
                    if (j == 19) worker1PayloadQueue.enq(j)
                  }
                }
              }
              retimeGate()
              doneQueue.enq(-1, i >= N.value)
            }
          }

          // Consumer
          Foreach(*) { i =>
            val id = priorityDeq(List(worker1Queue, worker2Queue, worker3Queue, doneQueue), List(!worker1PayloadQueue.isEmpty, !worker2PayloadQueue.isEmpty, !worker3PayloadQueue.isEmpty, Bit(true)))
            val payload = if (id == 0) worker1PayloadQueue.deq() else if (id == 1) worker2PayloadQueue.deq() else if (id == 2) worker3PayloadQueue.deq() else 0.to[Int]
            if (id != -1) outQueue.enq(id + payload)
            else kill := Bit(true)
          }
        }
      }

      result(0::outQueue.numel) store outQueue
    }

    val got = getMem(result)
    printArray(got, "Got data:")
    assert(true, "Just want the app to not hang")

  }
}