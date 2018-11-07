package spatial.tests.feature.unrolling

import spatial.dsl._

@spatial class UnrollingTest extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val o = ArgOut[Int]

    Accel {
      Foreach(32*16 by 16 par 2){i =>
        val x = SRAM[Int](32)

        Pipe {
          println(x(16))
        }

      }

      o := 32
    }

    println(getArg(o))
    assert(o == 32)
  }

}

@spatial class MemReduceToUnitPipe extends SpatialTest {

  def main(args: Array[String]): Unit = {
    type T = Int64

    val dram1 = DRAM[T](8)
    val dram2 = DRAM[T](8)
    val dram3 = DRAM[T](8)
    val dram4 = DRAM[T](8)
    val dram5 = DRAM[T](8)
    val tile = 2

    setMem(dram1, Array.tabulate[T](8){i => i.to[T]})

    Accel {
      val x = SRAM[T](8)
      x load dram1
      // // unroll map and reduce
      // val x1 = SRAM[T](tile,1)
      // Foreach(8 by tile){i => 
      //   x1 load dram1(i::i+tile)
      //   MemReduce(x1(0::tile par tile))(2 by 1 par 2){j => x1}{_+_}
      //   dram2(i::i+tile) store x1
      // }

      // unroll reduce only
      val x2 = SRAM[T](tile)
      Foreach(8 by tile){i => 
        x2 load dram1(i::i+tile)
        MemReduce(x2(0::tile par tile))(2 by 1){j => x(i::i+tile)}{_+_}
        dram3(i::i+tile) store x2
      }

      // // unroll reduce only
      // val x3 = SRAM[T](tile,1)
      // Foreach(8 by tile){i => 
      //   x3 load dram1(i::i+tile)
      //   MemReduce(x3)(2 by 1 par 2){j => x3}{_+_}
      //   dram4(i::i+tile) store x3
      // }

      // no unrolling
      val x4 = SRAM[T](tile)
      Foreach(8 by tile){i => 
        x4 load dram1(i::i+tile)
        MemReduce(x4)(2 by 1){j => x(i::i+tile)}{_+_}
        dram4(i::i+tile) store x4
      }


    }

    val gold = Array.tabulate[T](8){i => 2*i.to[T]}
    printArray(gold, "Dumb Load SRC")
    printArray(getMem(dram2), "Dumb Load (unroll both) DST1")
    printArray(getMem(dram3), "Dumb Load (unroll reduce) DST1")
    printArray(getMem(dram4), "Dumb Load (unroll map) DST1")
    printArray(getMem(dram5), "Dumb Load (no unroll) DST1")
    assert(gold == getMem(dram2))
    assert(gold == getMem(dram3))
    assert(gold == getMem(dram4))
    assert(gold == getMem(dram5))
  }
}


@spatial class SwitchCondReuse extends SpatialTest {
  override def runtimeArgs: Args = "1"

  def main(args: Array[String]): Unit = {
    val x = ArgIn[Int]
    val in = args(0).to[Int]
    setArg(x,in)
    val out = DRAM[Int](16)
    assert(in < 5)

    Accel {
      val sram = SRAM[Int](16)
      if (x.value < 5) {
        Sequential.Foreach(16 by 1 par 4){i => 
          Pipe{sram(i) = i}
          Pipe{sram(i) = i}
        }
      }
      out store sram
    }

    val gold = Array.tabulate(16){i => i}
    val got = getMem(out)
    printArray(gold, "Gold")
    printArray(got, "Got")
    assert(gold == got)
    println(r"PASS: ${gold == got}")
  }

}
