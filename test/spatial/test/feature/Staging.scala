package spatial.test.feature

import spatial.dsl._
import spatial.test.Testbench


@spatial object StageArgs {
  def main(args: Array[String]): Void = {
    val x = args(0).to[I32]
    println(r"x: $x")
  }
}

@spatial object StageForeach {
  def main(args: Array[String]): Void = Accel {
    Foreach(0::32){i => println("Hi") }
  }
}

@spatial object StageMemories {
  def main(args: Array[String]): Void = Accel {
    val dram = DRAM[I32](32)
    val sram = SRAM[I32](16)
    val fifo = FIFO[I32](8)
    val lifo = LIFO[I32](120)
  }
}

@spatial object StageUpdate {
  def main(args: Array[String]): Void = Accel {
    val sram = SRAM[I32](16)
    Foreach(0 until 32){i =>
      sram(i) = exp(i)
    }
    Foreach(0 until 32){i =>
      println("i=" ++ i ++ ": " ++ sram(i))
    }
  }
}

class Staging extends Testbench {
  test(StageArgs)
  test(StageForeach)
  test(StageMemories)
  test(StageUpdate)
}
