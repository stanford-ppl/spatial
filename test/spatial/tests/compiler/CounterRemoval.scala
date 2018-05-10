package spatial.tests.compiler

import argon.Block
import spatial.dsl._

@test class CounterRemovalOnRewrite extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    val x = ArgOut[Int]
    Accel {
      Foreach(1 by 1){i => x := i }
    }
    assert(getArg(x) == 0)
  }


  override def checkIR(block: Block[_]): Result = {
    val counters = block.nestedStms.collect{case ctr:Counter[_] => ctr }

    require(counters.isEmpty, "All counters should have been removed from this program")

    super.checkIR(block)
  }

}

@test class CounterRemovalOnUnroll extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](16)
    Accel {
      val sram = SRAM[Int](16)

      Foreach(16 par 16){i => sram(i) = i }

      dram store sram
    }
    val gold = Array.tabulate(16){i => i }
    assert(getMem(dram) == gold)
  }


  override def checkIR(block: Block[_]): Result = {
    val counters = block.nestedStms.collect{case ctr:Counter[_] => ctr }

    require(counters.isEmpty, "All counters should have been removed from this program")

    super.checkIR(block)
  }

}