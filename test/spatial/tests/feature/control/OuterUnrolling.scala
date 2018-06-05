package spatial.tests.feature.control

import spatial.dsl._

@test class OuterForeachUnrolling extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  override def backends: Seq[Backend] = Seq(Scala)

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32)
    Accel {
      Foreach(32 by 16 par 2){i =>
        val sram = SRAM[Int](16)
        Foreach(16 by 1){ii =>
          sram(ii) = i + ii
        }
        dram(i::i+16) store sram
      }
    }

    val result = getMem(dram)
    val golden = Array.tabulate(32){i => i }
    assert(result == golden)
  }
}

@test class OuterReduceUnrolling extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  override def backends: Seq[Backend] = Seq(Scala)

  def main(args: Array[String]): Unit = {
    val out = ArgOut[Int]
    Accel {
      val accum = Reg[Int]
      Reduce(accum)(32 by 16 par 2){i =>
        val sram = SRAM[Int](16)
        Foreach(16 by 1){ii =>
          sram(ii) = ii + i
        }
        sram(5)
      }{_+_}
      out := accum
    }
    val result = getArg(out)
    val golden = 26
    assert(result == golden)
  }
}

@test class OuterReduceUnrolling2 extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  override def backends: Seq[Backend] = Seq(Scala)

  def main(args: Array[String]): Unit = {
    val out = ArgOut[Int]
    Accel {
      val accum = Reg[Int]
      Reduce(accum)(32 by 16 par 2){ i =>
        Reduce(Reg[Int])(16 by 1){j => i + j}{_+_}
      }{_+_}

      out := accum
    }
    val result = getArg(out)
    val golden = Array.tabulate(32){i => i }.sum
    assert(result == golden)
  }
}



