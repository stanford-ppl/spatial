package pcc.test

import pcc.lang._

object StageForeach extends Testbench {
  def main(): Void = {
    (0::32){i => println("Hi") }
  }
}

object StageMemories extends Testbench {
  def main(): Void = {
    val dram = DRAM[I32](32)
    val sram = SRAM[I32](16)
    val fifo = FIFO[I32](8)
    val lifo = LIFO[I32](120)
  }
}

class StagingTests extends Tests {
  "StageForeach" should "compile" in { test(StageForeach) }
  "StageMemories" should "compile" in { test(StageMemories) }
}
