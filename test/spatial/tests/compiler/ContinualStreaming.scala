package spatial.tests.compiler
import spatial.dsl._
import spatial.metadata.memory._

@spatial class ContinualStreaming extends SpatialTest {
  override def compileArgs = super.compileArgs + "--nostreamify"
  def main(args: Array[String]): Unit = {

    val outerIters = 4

    val outDRAM = DRAM[I32](outerIters)
    Accel {
      val sram = SRAM[I32](2, outerIters).fullybanked
      sram.explicitName = "InternalMem"

      val outSRAM = SRAM[I32](outerIters)
      outSRAM.explicitName = "OutSRAM"
      Stream {
        val commFIFO = FIFO[I32](32)
        commFIFO.explicitName = "CommFIFO"

        // Slow controller, which causes starves on the receive side
        'Producer.Pipe.II(32).Foreach(outerIters by 1) {
          x =>
            sram(x & 1, x) = x + (x & 1) * outerIters
            retimeGate()
            commFIFO.enq(x)
        }

        'Consumer.Foreach(2*outerIters by 1) {
          j =>
            val en = (j&1).to[Bit]
            val y = commFIFO.deq(en)
            val x = j >> 1
            // x should be the same as y
            assertIf(Set(en), x == y, None)
            // On every other cycle, we read two values, but only use the first. This forces banking to happen.
            val read = sram.read(Seq(y & 1, x), Set(en))
            val read2 = sram.read(Seq(1 - (y & 1), x), Set(en))
            val muxed = mux(en, read, read2)
            outSRAM.write(muxed, Seq(x), Set(en))
        }
      }
      outDRAM store outSRAM
    }
    val ref = Array.tabulate(outerIters) {
      i => if ((i & 1) === 0) { i } else { i + outerIters }
    }
    val res = checkGold(outDRAM, ref)
    assert(res)
  }
}
