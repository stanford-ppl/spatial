package pcc.test

import pcc.lang._

object StageForeach extends Test {
  def main(): Void = {
    (0::32){i => println("Hi") }
  }
}

object StageMemories extends Test {
  def main(): Void = {
    val dram = DRAM[I32](32)
    val sram = SRAM[I32](16)
    val fifo = FIFO[I32](8)
    val lifo = LIFO[I32](120)
  }
}

object StageUpdate extends Test {
  def main(): Void = {
    val sram = SRAM[I32](16)
    Foreach(0 until 32){i =>
      sram(i) = exp(i)
    }
    Foreach(0 until 32){i =>
      println("i=">i>": ">sram(i))
    }
  }
}

class StagingTests extends Tests {
  "StageForeach" should "compile" in { test(StageForeach) }
  "StageMemories" should "compile" in { test(StageMemories) }
  "StageUpdate" should "compile" in { test(StageUpdate) }
}
