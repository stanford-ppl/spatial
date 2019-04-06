package spatial.tests.feature.unrolling

import spatial.dsl._

@spatial class OuterForeachUnrolling extends OuterForeachUnrollingBase {
  // override def backends: Seq[Backend] = Seq(Scala)
}
@spatial class OuterForeachUnrollingPOM extends OuterForeachUnrollingBase {
  override def compileArgs = super.compileArgs and "--pom"
}
@spatial abstract trait OuterForeachUnrollingBase extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](2,16)
    Accel {
      Foreach(32 by 16 par 2){i =>
        val sram = SRAM[Int](16)
        Foreach(16 by 1){ii =>
          sram(ii) = i + ii
        }
        dram(i/16, 0::16) store sram
      }
    }

    val result = getMatrix(dram)
    val golden = (0::2,0::16){(i,j) => j + 16*i}
    assert(result == golden)
  }
}

class OuterForeachUnrolling2 extends OuterForeachUnrolling2Base {
  // override def backends: Seq[Backend] = Seq(Scala)
}
class OuterForeachUnrolling2POM extends OuterForeachUnrolling2Base {
  override def compileArgs = super.compileArgs and "--pom" 
}
@spatial abstract trait OuterForeachUnrolling2Base extends SpatialTest {
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
        Foreach(16 by 1){j => sram2(j) = sram2(j) + i % 2 + 2}
        result((i % 2) + 2, 0::16) store sram2
      }
    }

    val got = getMatrix(result)
    val golden = (0::4, 0::16) {(i,j) => j + i}
    printMatrix(got, "got")
    printMatrix(golden, "golden")
    assert(got == golden)
  }
}

@spatial class OuterReduceUnrolling extends OuterReduceUnrollingBase {
  // override def backends: Seq[Backend] = Seq(Scala)
}
// @spatial class OuterReduceUnrollingPOM extends OuterReduceUnrollingBase {
//   override def compileArgs = super.compileArgs and "--pom" 
// }
@spatial abstract trait OuterReduceUnrollingBase extends SpatialTest {
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


@spatial class OuterReduceUnrolling2 extends OuterReduceUnrolling2Base {
  // override def backends: Seq[Backend] = Seq(Scala)
}
// @spatial class OuterReduceUnrolling2POM extends OuterReduceUnrolling2Base {
//   override def compileArgs = super.compileArgs and "--pom" 
// }

@spatial abstract trait OuterReduceUnrolling2Base extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val out = ArgOut[Int]
    val out2 = ArgOut[Int]
    Accel {
      val accum = Reg[Int]
      Reduce(accum)(32 by 16 par 2){ i =>
        Reduce(Reg[Int])(16 by 1){j => i + j}{_+_}
      }{_+_}

      out := accum

      val accum2 = Reg[Int]
      Reduce(accum2)(4 by 1 par 2){ i =>
        Reduce(Reg[Int])(16 by 1){j => i * 16 + j}{_+_}
      }{_+_}

      out2 := accum2
    }
    val result = getArg(out)
    val result2 = getArg(out2)
    val golden = Array.tabulate(32){i => i }.sum
    val golden2 = Array.tabulate(64){i => i }.sum
    assert(result == golden && result2 == golden2)
  }
}