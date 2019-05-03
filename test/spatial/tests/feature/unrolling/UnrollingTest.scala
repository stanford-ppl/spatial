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
    val cksum = (o == 32)
    println("PASS: " + cksum + " (UnrollingTest)")
    assert(cksum)
  }

}

@spatial class MemReduceToUnitPipe extends SpatialTest {

  def main(args: Array[String]): Unit = {
    type T = Int64

    val tile = 8
    val memsize = 64
    val mapIters = 4

    val dram1 = DRAM[T](memsize)
    val dram2 = DRAM[T](memsize)
    val dram3 = DRAM[T](memsize)
    val dram4 = DRAM[T](memsize)
    val dram5 = DRAM[T](memsize)

    setMem(dram1, Array.tabulate[T](memsize){i => i.to[T]})

    Accel {
      val x = SRAM[T](memsize)
      x load dram1
      // unroll map and reduce
      val x1 = SRAM[T](tile)
      'UNROLLmr.Foreach(memsize by tile){i => 
        MemReduce(x1(0::tile par tile))(mapIters by 1 par mapIters){j => x(i::i+tile)}{_+_}
        dram2(i::i+tile) store x1
      }

      // unroll reduce only
      val x2 = SRAM[T](tile)
      'UNROLLr.Foreach(memsize by tile){i => 
        MemReduce(x2(0::tile par tile))(mapIters by 1){j => x(i::i+tile)}{_+_}
        dram3(i::i+tile) store x2
      }

      // unroll map only
      val x3 = SRAM[T](tile)
      'UNROLLm.Foreach(memsize by tile){i => 
        MemReduce(x3)(mapIters by 1 par mapIters){j => x(i::i+tile)}{_+_}
        dram4(i::i+tile) store x3
      }

      // no unrolling
      val x4 = SRAM[T](tile)
      'UNROLLnone.Foreach(memsize by tile){i => 
        MemReduce(x4)(mapIters by 1){j => x(i::i+tile)}{_+_}
        dram5(i::i+tile) store x4
      }


    }

    val gold = Array.tabulate[T](memsize){i => mapIters*i.to[T]}
    printArray(gold, "gold")
    printArray(getMem(dram2), r"unroll both. Pass: ${getMem(dram2) == gold}")
    printArray(getMem(dram3), r"unroll reduce. Pass: ${getMem(dram3) == gold}")
    printArray(getMem(dram4), r"unroll map. Pass: ${getMem(dram4) == gold}")
    printArray(getMem(dram5), r"no unroll. Pass: ${getMem(dram5) == gold}")
    assert(gold == getMem(dram2))
    assert(gold == getMem(dram3))
    assert(gold == getMem(dram4))
    assert(gold == getMem(dram5))
  }
}


@spatial class SwitchCondReuse extends SpatialTest {
  override def dseModelArgs: Args = "100"
  override def finalModelArgs: Args = "100"
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
