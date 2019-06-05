package spatial.tests.feature.banking

import argon.Block
import spatial.dsl._

/** Simple test case for using a single SRAM (x) across two stages of a metapipeline
  * where the SRAM itself does not need to be buffered for the pipeline.
  */
@spatial class BankNonbuffered extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Float](32)
    val array = Array.tabulate(32){i => random[Float] }
    setMem(dram, array)

    val out_ceil  = DRAM[Float](32)
    val out_floor = DRAM[Float](32)

    Accel {
      val x = SRAM[Float](32)
      val y = SRAM[Float](32)
      val z = SRAM[Float](32)
      x load dram

      Foreach(0 until 32){i =>
        Pipe{ y(i) = ceil(x(i)) }   // These reads on x should either be broadcasted
        Pipe{ z(i) = floor(x(i)) }  // or correspond to 2 separate instances
      }

      out_ceil store y
      out_floor store z
    }

    val gold_ceil  = array.map{x => ceil(x) }
    val gold_floor = array.map{x => floor(x) }
    val cksum = (gold_ceil == getMem(out_ceil)) && (gold_floor == getMem(out_floor))
    println("PASS: " + cksum + " (BankNonbuffered)")
    assert(cksum)
  }

  override def checkIR(block: Block[_]): Result = {
    import spatial.metadata.memory._
    val xs = LocalMemories.all.filter(_.name.exists{_.startsWith("x")})
    xs.forall(_.instance.depth == 1) &&
      xs.forall{x => x.readers.map{read => read.port.muxPort }.size == 1 }
  }
}
