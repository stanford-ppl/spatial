package spatial.test

import spatial.dsl._
import utest._

@spatial object StageArgs {
  def main(): Void = {
    val x = args(0).to[I32]
    println(r"x: $x")
  }
}

@spatial object StageForeach {
  def main(): Void = Accel {
    Foreach(0::32){i => println("Hi") }
  }
}

@spatial object StageMemories {
  def main(): Void = Accel {
    val dram = DRAM[I32](32)
    val sram = SRAM[I32](16)
    val fifo = FIFO[I32](8)
    val lifo = LIFO[I32](120)
  }
}

@spatial object StageUpdate {
  def main(): Void = Accel {
    val sram = SRAM[I32](16)
    Foreach(0 until 32){i =>
      sram(i) = exp(i)
    }
    Foreach(0 until 32){i =>
      println("i=" ++ i ++ ": " ++ sram(i))
    }
  }
}

object StagingTests extends Testbench { val tests = Tests {
  'StageArgs     - test(StageArgs)
  'StageForeach  - test(StageForeach)
  'StageMemories - test(StageMemories)
  'StageUpdate   - test(StageUpdate)
}}
