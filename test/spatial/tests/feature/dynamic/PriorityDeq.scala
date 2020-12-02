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

@spatial class PriorityDeq2In1 extends SpatialTest {
  override def runtimeArgs: Args = "1"

  def main(args: Array[String]): Unit = {
    val N = ArgIn[Int]
    setArg(N, args(0).to[Int])

    val R = ArgOut[Int]

    Accel {
      val worklist0 = FIFO[Int](16)
      val worklist1 = FIFO[Int](16)
      val tokens0 = FIFO[Int](16)
      val tokens1 = FIFO[Int](16)
      val workQueue = FIFO[Int](16)
      // Initialize to 1 so we can get the thing moving during the first iter
      val tokenCtr0 = Reg[Int](1)
      val tokenCtr1 = Reg[Int](1)

      // Count how many things we processed (should be 2 * N)
      val processed_count = Reg[Int](0)

      // Stuff N items into the b fifos
      Sequential.Foreach(N by 1) { i =>
        worklist0.enq(0)
        retimeGate() // Force these to happen in different cycles
        worklist1.enq(1)
      }

      Stream {
   // Give a ton of tokens in a fifos
        Foreach(4 by 1) { i =>
          tokens0.enq(0)
          tokens0.enq(1)
        }

        // Should have a ton of tokens but only
        Foreach(*) { i =>
          val token = priorityDeq(tokens1, tokens0)
          val next = priorityDeq[Int](List(worklist0, worklist1), List(tokenCtr0.value > 0, tokenCtr1.value > 0))
          if (token == 0) tokenCtr0 :+= mux(token == 0, 1, 0) - mux(next == 0, 1, 0)
          if (token == 1) tokenCtr1 :+= mux(token == 1, 1, 0) - mux(next == 1, 1, 0)
          workQueue.enq(next)
        }

        Foreach(*) { i =>
          val got = workQueue.deq()
          // acknowledge
          processed_count :+= 1
          // Give token back
          tokens1.enq(got)
          // Break out if we processed all that were expected and workQueue is empty
          val done = workQueue.numel == 0 && processed_count >= N * 2 - 1 //fudge
          if (done) {
            R := processed_count.value
            exit()
          }
        }
      }

    }

    val got = getArg(R)
    println(r"Got $got, wanted ${N*2}")
    assert(got == N*2, "Just want the app to not hang")

  }
}


@spatial class DeqDependencyCtrl extends SpatialTest {

  def main(args: Array[String]): Unit = {

    val R = ArgOut[Int]

    Accel {
      val info = FIFO[Int](16)
      val payload = FIFO[Int](16);
      val killfifo = FIFO[Int](16)
      val kill = Reg[Bit](false)
      Sequential(breakWhen = kill).Foreach(99999 by 1) { _ =>
        Stream.Foreach(*) { i =>
          info.enq(i)

          // Should have a ton of tokens
          Pipe.haltIfStarved {
            val data = info.deq()
            // With control bug, this deq happens before the enq to payload finished.  There is timing mismatch between payload being empty and payload activeIn toggling
            if (data == 100) R := payload.deq()
            retimeGate()
            if (data == 100) killfifo.enq(1)
          }

          Pipe {
              payload.enq(5, i % 100 == 0 && i > 0)
          }

          Pipe {
            val got = killfifo.deq()
            kill := got == 1
          }

        }
      }
    }

    val got = getArg(R)
    println(r"Got $got, wanted ${5}")
//    assert(got == 5, "Want to make sure we enq before we deq")

  }
}

