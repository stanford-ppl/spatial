package spatial.tests.feature.banking

import spatial.dsl._
import argon.Block

@spatial class ReadBroadcast extends SpatialTest {

  def main(args: Array[String]): Void = {
    val d = DRAM[Int](16,16)

    Accel {

      val s = SRAM[Int](16)
      Foreach(16 by 1){i => s(i) = i}
      val s2 = SRAM[Int](16,16)
      Foreach(16 by 1, 16 by 1 par 8) {(i,j) => s2(i,j) = s(i) * s(j)}
      d store s2
  
    }

    printMatrix(getMatrix(d), "result: ")
    val gold = (0::16,0::16){(i,j) => i * j}
    printMatrix(gold, "gold: ")
    assert(gold == getMatrix(d))

  }
  override def checkIR(block: Block[_]): Result = {
    val srams = block.nestedStms.collect{case p:spatial.node.SRAMNew[_,_] => p }

    assert(srams.length == 3, r"There should (probably) only be 3 SRAMNews in this app, found ${srams.length}")

    super.checkIR(block)
  }

}
