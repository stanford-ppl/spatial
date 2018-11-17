package spatial.tests.feature.dynamic

import spatial.dsl._

@spatial class DummyWorkload extends SpatialTest {
  override def runtimeArgs: Args = "128"

  // Set up properties
  val maxQueue = 256
  val HEAVY = 16
  val MEDIUM = 8
  val LIGHT = 1
  val DONE = -1

  def main(args: Array[String]): Unit = {
    // Set up size of queue
    val queueSize = args(0).to[Int]
    val QS = ArgIn[Int]
    setArg(QS, queueSize)

    // Create "packets", where each "packet" is just an Int 
    //   that roughly represents how long a worker will spend
    //   working on it
    val allPackets = DRAM[Int](QS)
    val initWeights = Array.tabulate(QS){i => if (i % 7 == 0) HEAVY else if (i % 7 == 1) MEDIUM else LIGHT }
    setMem(allPackets, initWeights)
    printArray(initWeights, "Work profile:")

    // Create counters, who will make sure the workers shared their work properly
    val counter1 = HostIO[Int]
    val counter2 = HostIO[Int]
    val counter3 = HostIO[Int]
    setArg(counter1, 0)
    setArg(counter2, 0)
    setArg(counter3, 0)

    // Create status reg
    val status = ArgOut[Int]

    Accel {         
      // Fetch work
      val localQueue = SRAM[Int](maxQueue)
      localQueue load allPackets(0::QS)

      // Create worker queues
      val worker1Queue = FIFO[Int](3)
      val worker2Queue = FIFO[Int](3)
      val worker3Queue = FIFO[Int](3)

      // Create break regs
      val break1 = Reg[Bit](false)
      val break2 = Reg[Bit](false)
      val break3 = Reg[Bit](false)

      // Create done reporters, these are FIFOs of depth 1, used for controlling streams
      val done1 = FIFOReg[Bit]
      val done2 = FIFOReg[Bit]
      val done3 = FIFOReg[Bit]

      Stream {

        'SCHEDULER.Sequential.Foreach(QS by 1){packet => 
          val p = localQueue(packet)

          // Worker estimates
          val workLoads = List.fill(3)(Reg[Int](0))
          if (workLoads(0).value <= workLoads(1).value && workLoads(0).value <= workLoads(2).value) {
            workLoads(0) :+= p; worker1Queue.enq(p)
          }
          else if (workLoads(1).value <= workLoads(0).value && workLoads(1).value <= workLoads(2).value) {
            workLoads(1) :+= p; worker2Queue.enq(p)
          }
          else {
            workLoads(2) :+= p; worker3Queue.enq(p)
          }

          // Decrement workLoads each "cycle" for cheap estimate
          workLoads.foreach{ w => if (w.value > 0) w :-= 1 }

          // If all packets sent, give everyone "done" signal
          // NOTE: Pipe here is to guarantee the enqs above finish
          //       before these enqs start.  Otherwise, retiming
          //       doesn't recognize the dependency
          Pipe{ 
            if (packet == QS.value-1) {
              worker1Queue.enq(done)
              worker2Queue.enq(done)
              worker3Queue.enq(done)
            }
          }
        }

        'WORKER1.Sequential(breakWhen = break1).Foreach(*){_ => 
          val p = worker1Queue.deq()
          // Do "work" on valid packet
          if (p > 0) {
            Foreach(p by 1){i => 
              counter1 :+= 1
            }
          } 
          // Handle done packet
          else if (p == DONE) {
            break1 := true
            done1.enq(true)
          }
        }

        'WORKER2.Sequential(breakWhen = break2).Foreach(*){_ => 
          val p = worker2Queue.deq()
          // Do "work" on valid packet
          if (p > 0) {
            Foreach(p by 1){i => 
              counter2 :+= 1
            }
          } 
          // Handle done packet
          else if (p == DONE) {
            break2 := true
            done2.enq(true)
          }
        }

        'WORKER3.Sequential(breakWhen = break3).Foreach(*){_ => 
          val p = worker3Queue.deq()
          // Do "work" on valid packet
          if (p > 0) {
            Foreach(p by 1){i => 
              counter3 :+= 1
            }
          } 
          // Handle done packet
          else if (p == DONE) {
            break3 := true
            done3.enq(true)
          }
        }

        // When all 3 done FIFORegs are valid, we can finish up
        'FINISHER.Pipe{
          val a = done1.deq()
          val b = done2.deq()
          val c = done3.deq()
          if (a && b && c) status := 1
        }
      }
    }

    println(r"Status = $status")
    println(r"Worker1 processed: ${getArg(counter1)}")
    println(r"Worker2 processed: ${getArg(counter2)}")
    println(r"Worker3 processed: ${getArg(counter3)}")
    println(r"Total work sent: ${initWeights.reduce{_+_}}")
    println(r"Total work done: ${getArg(counter1) + getArg(counter2) + getArg(counter3)}")
    println(r"Pass: ${initWeights.reduce{_+_} == getArg(counter1) + getArg(counter2) + getArg(counter3)}")
    assert(initWeights.reduce{_+_} == getArg(counter1) + getArg(counter2) + getArg(counter3))
    assert(status == 1)

  }
}
