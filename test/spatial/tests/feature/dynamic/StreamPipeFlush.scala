package spatial.tests.feature.dynamic

import spatial.dsl._

@spatial class SimpleStreamPipeFlush extends SpatialTest {
  override def main(args: Array[String]) = {

    val outputDRAM = DRAM[I32](8)
    Accel {
      val outputSRAM = SRAM[I32](8)

      val break = Reg[Bit](0.to[Bit])
      Stream(breakWhen = break).Foreach(*) {
        _ =>
        val inFIFO = FIFO[I32](8)
        val outFIFO = FIFO[I32](8)
        val flushFIFO = FIFO[I32](8)

        Pipe.Foreach(8 by 1) {
          x => inFIFO.enq(x)
        }

        Pipe.Foreach(*) {
          _ => flushFIFO.enq(I32(-1))
        }

        Pipe.Foreach(*) {
          _ =>
            val v = priorityDeq(inFIFO, flushFIFO)
            // Do some stuff
            if (v != I32(-1)) {
              val r = Reduce(Reg[I32])(4 by 1) {
                i => i + v
              } {
                _ + _
              }
              outFIFO.enq(r)
            }
        }

        Sequential {
          Pipe.Foreach(8 by 1) {
            x =>
              outputSRAM(x) = outFIFO.deq()
          }
          break := true
        }
      }
      outputDRAM store outputSRAM
    }
    val mem = getMem(outputDRAM)
    Foreach(8 by 1) {
      i =>
        val gold = I32(6) + I32(4) * i
        assert(mem(i) == gold, r"Expected: $gold, received ${mem(i)} on iteration $i")
    }
  }
}


@spatial class ComplexStreamPipeFlush extends SpatialTest {
  override def main(args: Array[String]) = {

    val outputDRAM = DRAM[I32](8)
    Accel {
      val outputSRAM = SRAM[I32](8)

      val break = Reg[Bit](0.to[Bit])
      Stream(breakWhen = break).Foreach(*) {
        _ =>
          val inFIFO = FIFO[I32](8)
          val secondInFIFO = FIFO[I32](1)
          val outFIFO = FIFO[I32](8)
          val flushFIFO = FIFO[I32](8)

          Pipe.Foreach(8 by 1) {
            x => inFIFO.enq(x)
          }

          Pipe.Foreach(*) {
            x => secondInFIFO.enq(I32(11) * x)
          }

          Pipe.Foreach(*) {
            _ => flushFIFO.enq(I32(-1))
          }

          Pipe.Foreach(*) {
            _ =>
              val v = priorityDeq(inFIFO, flushFIFO)
              // Do some stuff
              if (v != I32(-1)) {
                val y = secondInFIFO.deq()
                val r = Reduce(Reg[I32])(4 by 1) {
                  i => i + v
                } {
                  _ + _
                }
                outFIFO.enq(r + y)
              }
          }

          Sequential {
            Pipe.Foreach(8 by 1) {
              x =>
                outputSRAM(x) = outFIFO.deq()
            }
            break := true
          }
      }
      outputDRAM store outputSRAM
    }
    val mem = getMem(outputDRAM)
    Foreach(8 by 1) {
      i =>
        val gold = I32(6) + I32(4) * i + I32(11) * i
        assert(mem(i) == gold, r"Expected: $gold, received ${mem(i)} on iteration $i")
    }
  }
}
