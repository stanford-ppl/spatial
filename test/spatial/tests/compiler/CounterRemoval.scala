package spatial.tests.compiler

import argon.Block
import spatial.dsl._

@spatial class CounterRemovalOnRewrite extends SpatialTest {

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

@spatial class CounterRemovalOnUnroll extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](16)
    val x = ArgIn[Int]
    val y = ArgOut[Int]
    setArg(x, 15)
    Accel {
      val sram = SRAM[Int](16)

      Foreach(16 par 16){i => sram(i) = i }

      y := sram(x)
    }
    
    assert(getArg(y) == 15)
  }


  override def checkIR(block: Block[_]): Result = {
    val counters = block.nestedStms.collect{case ctr:Counter[_] => ctr }

    require(counters.isEmpty, "All counters should have been removed from this program")

    super.checkIR(block)
  }

}