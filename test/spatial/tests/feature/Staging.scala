package spatial.tests.feature

import spatial.dsl._

@test class StageArgs extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val x = args(0).to[I32]
    println(r"x: $x")
  }
}

@test class StageForeach extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = Accel {
    Foreach(0::32){i => println("Hi") }
  }
}

@test class StageMemories extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = Accel {
    val dram = DRAM[I32](32)
    val sram = SRAM[I32](16)
    val fifo = FIFO[I32](8)
    val lifo = LIFO[I32](120)
  }
}

@test class StageUpdate extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

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

