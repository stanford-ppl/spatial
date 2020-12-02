package spatial.tests.feature.dynamic
import spatial.dsl._
import spatial.metadata.memory._

@spatial class PseudoCoprocTest extends SpatialTest {

  // Deadlock can be produced by having a later stage

  val INITIAL_CREDITS = 4
  val FIFOS = 2
  val TASKS = 32
  val II = 10

  override def compileArgs = s"--max_cycles=${FIFOS * TASKS * II}"

  def getID(x: I32) = x % I32(FIFOS)

  override def main(args: Array[String]) = {

    val outputDRAM = DRAM[I32](TASKS)
    Accel {
      val outputMem = RegFile[I32](TASKS)
      val kill = Reg[Bit](false)
      Stream(breakWhen = kill).Foreach(*) {
        _ =>
          val CreditFIFOs = Range(0, FIFOS) map {x => FIFO[I32](8)}
          val initCreditFIFOs = FIFO[I32](8)
          val creditRegs = Range(0, FIFOS) map {
            x =>
              val r = Reg[I32](I32(0))
              r.explicitName = f"CreditReg$x"
              r
          }
          val inputFIFOs = Range(0, FIFOS) map {x => FIFO[I32](I32(10))}
          val outputFIFOs = Range(0, FIFOS) map {x => FIFO[I32](I32(10))}
          val centralFIFO = FIFO[I32](I32(10))

          'CreditInitializer.Pipe.Foreach(INITIAL_CREDITS by 1) {
            _ =>
              Foreach(FIFOS by 1) {
                i => initCreditFIFOs.enq(i)
              }
          }

          inputFIFOs.zipWithIndex foreach {
            case (fifo, ind) =>
              'TaskMaster.Pipe.Foreach(TASKS by 1) {
                task =>
                  // Guarantee that getID(value) === ind
                  fifo.enq(task * FIFOS + ind)
              }
          }



          Sequential {
            creditRegs foreach { r => r := 4 }
            'Arbiter.Pipe.Foreach(*) {
              _ =>
                val creditEnables = creditRegs map {_ > 0}
                val nextTask = priorityDeq(inputFIFOs.toList, creditEnables.toList)
                centralFIFO.enq(nextTask)

                // compute credit update
                val newCredit = priorityDeq((CreditFIFOs ++ Seq(initCreditFIFOs)):_*)
                creditRegs.zipWithIndex foreach {
                  case (reg, ind) =>
                    val decrement = getID(nextTask) == I32(ind)
                    val increment = newCredit == I32(ind)
                    reg := reg - decrement.to[I32] + increment.to[I32]
                }
            }
          }

          'Processor.Pipe.Foreach(*) {
            _ =>
              val task = centralFIFO.deq()
              val outputID = getID(task)

              outputFIFOs.zipWithIndex foreach {
                case (fifo, ind) =>
                  fifo.enq(task, I32(ind) == outputID)
              }
          }

          Sequential {
            'Writer.Stream {
              Pipe.II(II).Foreach(TASKS by 1) {
                loc =>
                  val parts = outputFIFOs map {_.deq()}
                  CreditFIFOs.zipWithIndex foreach {
                    case (credfifo, ind) => credfifo.enq(ind)
                  }
                  outputMem(loc) = parts.reduceTree { _ + _ }
              }
            }
            kill := true
          }
      }
      outputDRAM store outputMem
    }

    val output = getMem(outputDRAM)
    Foreach(TASKS by 1) {
      ind =>
        val out = output(ind)
        val golden = ind * FIFOS * FIFOS + (FIFOS - 1) * FIFOS / 2
        assert(out == golden, r"Expected $golden, received $out at index $ind")
    }
  }
}
