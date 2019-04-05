package spatial.tests.feature.unrolling

import spatial.dsl._

@spatial class OuterForeachUnrolling extends SpatialTest {
  // override def backends: Seq[Backend] = Seq(Scala)

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

@spatial class OuterForeachUnrolling2 extends SpatialTest {
  // override def backends: Seq[Backend] = Seq(Scala)

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](16)
    setMem(dram, Array.tabulate(16){i => i})
    val result = DRAM[Int](4,16)
    Accel {
      Foreach(32 by 1 par 2){i =>
        val sram = SRAM[Int](16).buffer
        sram load dram
        Foreach(16 by 1){j => sram(j) = sram(j) + i % 2}
        result(i % 2, 0::16) store sram
      }

      Foreach(2 by 1 par 2){i =>
        val sram2 = SRAM[Int](16).buffer
        sram2 load dram
        Foreach(16 by 1){j => sram2(j) = sram2(j) + i % 2}
        result((i % 2) + 2, 0::16) store sram2
      }
    }

    val got = getMatrix(result)
    val golden = (0::4, 0::16) {(i,j) => if (i < 2) j + i else j + i - 2}
    assert(got == golden)
  }
}

@spatial class OuterReduceUnrolling2 extends SpatialTest {
  // override def backends: Seq[Backend] = Seq(Scala)

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

@spatial class OuterReduceUnrolling extends SpatialTest {
  // override def backends: Seq[Backend] = Seq(Scala)

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

@spatial class OuterReduceUnrolling2POM extends SpatialTest {
  override def compileArgs = super.compileArgs and "--pom"
  // override def backends: Seq[Backend] = Seq(Scala)

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

@spatial class OuterForeachUnrollingPOM extends SpatialTest {
  override def compileArgs = super.compileArgs and "--pom"
  // override def backends: Seq[Backend] = Seq(Scala)

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

@spatial class OuterForeachUnrolling2POM extends SpatialTest {
  override def compileArgs = super.compileArgs and "--pom"
  // override def backends: Seq[Backend] = Seq(Scala)

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](16)
    setMem(dram, Array.tabulate(16){i => i})
    val result = DRAM[Int](2,16)
    Accel {
      Foreach(32 by 1 par 2){i =>
        val sram = SRAM[Int](16).buffer
        sram load dram
        Foreach(16 by 1){j => sram(j) = sram(j) + i % 2}
        result(i % 2, 0::16) store sram
      }
    }

    val got = getMatrix(result)
    val golden = (0::2, 0::16) {(i,j) => j + i}
    assert(got == golden)
  }
}


@spatial class OuterReduceUnrollingPOM extends SpatialTest {
  override def compileArgs = super.compileArgs and "--pom"
  // override def backends: Seq[Backend] = Seq(Scala)

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




