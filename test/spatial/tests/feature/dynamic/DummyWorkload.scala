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

  // Define packet struct
  @struct case class full_packet(payload: Int, idx: Int) 

  def main(args: Array[String]): Unit = {
    // Set up size of queue
    val queueSize = args(0).to[Int]
    val QS = ArgIn[Int]
    assert(queueSize <= maxQueue)
    setArg(QS, queueSize)

    // Create "packets", where each "packet" is just an Int 
    //   that roughly represents how long a worker will spend
    //   working on it
    val allPackets = DRAM[full_packet](QS)
    val initWeights = Array.tabulate(QS){i => 
      if (i % 7 == 0) full_packet(HEAVY, i) 
      else if (i % 7 == 1) full_packet(MEDIUM, i) 
      else full_packet(LIGHT, i) 
    }
    setMem(allPackets, initWeights)
    printArray(initWeights, "Work profile:")

    // Create counters, who will make sure the workers shared their work properly
    val counter1 = HostIO[Int]
    val counter2 = HostIO[Int]
    val counter3 = HostIO[Int]
    setArg(counter1, 0)
    setArg(counter2, 0)
    setArg(counter3, 0)

    // Create memory to track which worker processed which packet
    val history = DRAM[Int](QS + 1)

    // Create status reg
    val status = ArgOut[Int]

    Accel {         
      // Fetch work
      val localQueue = SRAM[full_packet](maxQueue)
      localQueue load allPackets(0::QS)

      // Create worker worklist queues
      val worker1Queue = FIFO[full_packet](3)
      val worker2Queue = FIFO[full_packet](3)
      val worker3Queue = FIFO[full_packet](3)

      // Create worker history queues
      val worker1History = FIFO[Int](maxQueue + 1)
      val worker2History = FIFO[Int](maxQueue + 1)
      val worker3History = FIFO[Int](maxQueue + 1)
      
      // Put one element in each history to guarantee it won't stall the FINISH stage
      worker1History.enq(-1)
      worker2History.enq(-1)
      worker3History.enq(-1)

      // Create break regs
      val break1 = Reg[Bit](false)
      val break2 = Reg[Bit](false)
      val break3 = Reg[Bit](false)

      // Create done reporters, these are FIFOs of depth 1, used for controlling streams
      val done1 = FIFOReg[Bit]
      val done2 = FIFOReg[Bit]
      val done3 = FIFOReg[Bit]

      def schedule(p: full_packet): Unit = {
        // Worker estimates
        val workLoads = List.fill(3)(Reg[Int](0))
        if (workLoads(0).value <= workLoads(1).value && workLoads(0).value <= workLoads(2).value) {
          workLoads(0) :+= p.payload; worker1Queue.enq(p)
        }
        else if (workLoads(1).value <= workLoads(0).value && workLoads(1).value <= workLoads(2).value) {
          workLoads(1) :+= p.payload; worker2Queue.enq(p)
        }
        else {
          workLoads(2) :+= p.payload; worker3Queue.enq(p)
        }

        // Decrement workLoads each "cycle" for cheap estimate
        workLoads.foreach{ w => if (w.value > 5) w :-= 5 }
      }

      def processPacket(p: full_packet, workerHistory: FIFO[Int], counter: Reg[Int]): Unit = {
        // Log packet
        workerHistory.enq(p.idx)
        // Process payload
        Foreach(p.payload by 1){i => 
          counter :+= 1
        }

      }

      Stream {

        'SCHEDULER.Sequential.Foreach(QS by 1){packet => 
          val p: full_packet = localQueue(packet)

          // Issue packet to correct worker, based on schedule rule
          schedule(p)

          // If all packets sent, give everyone "done" signal
          // NOTE: Pipe here is to guarantee the enqs above finish
          //       before these enqs start.  Otherwise, retiming
          //       doesn't recognize the dependency
          Pipe{ 
            if (packet == QS.value-1) {
              worker1Queue.enq(full_packet(DONE,0))
              worker2Queue.enq(full_packet(DONE,0))
              worker3Queue.enq(full_packet(DONE,0))
            }
          }
        }

        // Indicate that controller should execute sequentially,
        //    and provide register who will break the loop
        'WORKER1.Sequential(breakWhen = break1).Foreach(*){_ => 
          val p = worker1Queue.deq()
          // Do "work" on valid packet
          if (p.payload > 0) {
            processPacket(p, worker1History, counter1)
          } 
          // Handle done packet
          else if (p.payload == DONE) {
            break1 := true
            done1.enq(true)
          }
        }

        // Indicate that controller should execute sequentially,
        //    and provide register who will break the loop
        'WORKER2.Sequential(breakWhen = break2).Foreach(*){_ => 
          val p = worker2Queue.deq()
          // Do "work" on valid packet
          if (p.payload > 0) {
            processPacket(p, worker2History, counter2)
          } 
          // Handle done packet
          else if (p.payload == DONE) {
            break2 := true
            done2.enq(true)
          }
        }

        // Indicate that controller should execute sequentially,
        //    and provide register who will break the loop
        'WORKER3.Sequential(breakWhen = break3).Foreach(*){_ => 
          val p = worker3Queue.deq()
          // Do "work" on valid packet
          if (p.payload > 0) {
            processPacket(p, worker3History, counter3)
          } 
          // Handle done packet
          else if (p.payload == DONE) {
            break3 := true
            done3.enq(true)
          }
        }

        // When all 3 done FIFORegs are valid, we can finish up
        'FINISHER.Pipe{
          val a = done1.deq()
          val b = done2.deq()
          val c = done3.deq()
          if (a && b && c) status := 1 // Be sure to use a,b,c to avoid DCE

          // Interleave history
          val aggregatedHistory = FIFO[Int](maxQueue + 4)
          val w1 = Reg[Int]; w1 := worker1History.deq
          val w2 = Reg[Int]; w2 := worker2History.deq
          val w3 = Reg[Int]; w3 := worker3History.deq
          FSM(1)(_ != 0) { _ =>
            if (w1.value <= w2.value && w1.value <= w3.value) {
              aggregatedHistory.enq(1)
              if (worker1History.isEmpty) w1 := maxQueue + 999 else w1 := worker1History.deq
            }
            else if ((worker1History.isEmpty && w2.value <= w3.value) || (w2.value <= w1.value && w1.value <= w3.value)) {
              aggregatedHistory.enq(2)
              if (worker2History.isEmpty) w2 := maxQueue + 999 else w2 := worker2History.deq
            }
            else {
              aggregatedHistory.enq(3)
              if (worker3History.isEmpty) w3 := maxQueue + 999 else w3 := worker3History.deq
            }
          }{whil => mux(worker1History.isEmpty && worker2History.isEmpty && worker3History.isEmpty, 0, 1)}
          
          // Export history
          history store aggregatedHistory
        }
      }
    }

    println(r"Status = $status")
    println("\"Good\" behavior is when each worker processes roughly the same amount.")
    println(r"Worker1 processed: ${getArg(counter1)}")
    println(r"Worker2 processed: ${getArg(counter2)}")
    println(r"Worker3 processed: ${getArg(counter3)}")
    println(r"Total work sent: ${initWeights.map(_.payload).reduce{_+_}}")
    println(r"Total work done: ${getArg(counter1) + getArg(counter2) + getArg(counter3)}")
    // println(r"Pass: ${initWeights.reduce{_+_} == getArg(counter1) + getArg(counter2) + getArg(counter3)}")
    printArray(getMem(history), "Worker responsible for each packet")
    assert(getMem(history).map(_ != 0).reduce{_&&_}, "At least one packet apparently not processed!")
    assert(getArg(counter1) > 0)
    assert(getArg(counter2) > 0)
    assert(getArg(counter3) > 0)
    assert(initWeights.map(_.payload).reduce{_+_} == getArg(counter1) + getArg(counter2) + getArg(counter3))
    assert(status == 1)

  }
}
