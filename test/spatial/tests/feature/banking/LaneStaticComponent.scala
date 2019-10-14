package spatial.tests.feature.banking

import spatial.node._
import spatial.dsl._
import argon.Block
import argon.Op

@spatial class LaneStaticComponent extends SpatialTest {
 def main(args: Array[String]): Unit = {
     val N = ArgIn[Int]
     setArg(N, 4)
     val Y = ArgOut[Int]
     val addr = ArgIn[Int]
     setArg(addr, 7)

     Accel {
      val sram = SRAM[Int](32)
      Foreach(32 by 4) { i =>
        Foreach(N by 1 par 4) { j => sram(i + j % 4) = i + j }
      }
      Y := sram(addr)
     }
     println(r"Got ${getArg(Y)}, wanted 7")
     assert(getArg(Y) == 7)
  }
  override def checkIR(block: Block[_]): Result = {
    val sram1_count = block.nestedStms.collect{case  x@Op(sram:SRAMNew[_,_]) => sram }.size

    require(sram1_count ==  1, "Should only have 1 duplicate of sram")

    super.checkIR(block)
  }
}