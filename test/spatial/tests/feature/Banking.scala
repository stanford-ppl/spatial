package spatial.tests.feature

import spatial.dsl._

@test class SimpleParTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    Accel {
      val sram = SRAM[I32](64)
      Foreach(64 par 16){i => sram(i) = i + 1  }
      Foreach(64 par 16){i => println(sram(i)) }
    }
  }
}

@test class FixedOffsetTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val x = ArgIn[I32]
    Accel {
      val sram = SRAM[I32](64)
      Foreach(64 par 16){i => sram(i + x.value) = i + x.value }
      Foreach(64 par 16){i => println(sram(i)) }
    }
  }
}

@test class RandomOffsetTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    Accel {
      val sram = SRAM[I32](64)
      val reg = Reg[I32](0)
      Foreach(64 par 16){i => sram(i) = i }
      Foreach(64 par 16){i =>
        val x = sram(i + reg.value)
        reg := x
        println(x)
      }
    }
  }
}

@test class RandomOffsetTestWrite extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    Accel {
      val sram = SRAM[I32](64)
      val reg = Reg[I32](0)
      Foreach(64 par 16){i =>
        sram(i + reg.value) = i
        reg := (reg.value+1)*5
      }
      Foreach(64 par 16){i => println(sram(i)) }
    }
  }
}

@test class TwoDuplicatesSimple extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val dram = DRAM[Int](32)
    val dram2 = DRAM[Int](32)

    Accel {
      val sram = SRAM[Int](32)

      Foreach(32 by 16){i =>
        Foreach(16 par 16){ii =>
          sram(ii) = i + ii
        }

        dram(i::i+16 par 16) store sram
        dram2(i::i+16 par 16) store sram
      }
    }
    val result = getMem(dram)
    val gold = Array.tabulate(32){i => i}

    printArray(result, "result")
    printArray(gold, "gold")

    gold.foreach{i => assert(result(i) == i) }
  }
}

// Nonsensical app, just to get structure there.
@test class TwoDuplicatesPachinko extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val dram = DRAM[Int](512)
    val out = ArgOut[Int]

    Accel {
      Foreach(16 by 1){i =>

        val sram = SRAM[Int](16)

        sram load dram(i::i+16)

        Foreach(32 by 16 par 2){j =>
          val accum = Reg[Int]
          Reduce(accum)(16 par 16){k => sram(k) }{_+_}
          out := accum.value
        }
      }
    }
  }
}



@test class LegalFIFOParallelization extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val out = ArgOut[Int]

    Accel {
      val fifo = FIFO[Int](32)

      Foreach(16 by 1) {i =>
        Foreach(8 par 8){j => fifo.enq(j) }
        Foreach(8 par 1){j => out := fifo.deq() }
      }
    }

  }
}

@test class RegCoalesceTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val out1 = ArgOut[Int]
    val out2 = ArgOut[Int]
    Accel {
      Foreach(16 by 1) {i =>
        val reg = Reg[Int]
        Pipe { reg := i }
        Pipe { out1 := reg.value }
        Pipe { out2 := reg.value }
      }
    }
  }
}

@test class SRAMCoalesceTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    Accel {
      val sram = SRAM[Int](32)
      val out1 = ArgOut[Int]
      val out2 = ArgOut[Int]
      Foreach(16 by 1){i =>
        Foreach(16 by 1){j => sram(j) = i*j }
        val sum = Reduce(0)(16 par 2){j => sram(j) }{_+_}
        val product = Reduce(0)(16 par 3){j => sram(j) }{_*_}
        out1 := sum
        out2 := product
      }
    }
  }
}

@test class LinearWriteRandomRead extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    Accel {
      val sram = SRAM[Int](16)
      val addr = SRAM[Int](16)
      val out1 = ArgOut[Int]
      val out2 = ArgOut[Int]
      Foreach(16 by 1){i =>
        Foreach(16 by 1 par 2){j =>
          sram(j) = i*j
          addr(j) = 16 - j
        }
        val sum = Reduce(0)(16 par 5){j => sram(addr(j)) }{_+_}
        out1 := sum
      }
    }
  }
}


@test class MemTest1D extends SpatialTest {
  override def runtimeArgs: Args = "7"

  def main(args: Array[String]): Void = {

    // Declare SW-HW interface vals
    val x = ArgIn[I32]
    val y = ArgOut[I32]
    val N = args(0).to[I32]

    // Connect SW vals to HW vals
    setArg(x, N)

    // Create HW accelerator
    Accel {
      val mem = SRAM[I32](384)
      Sequential.Foreach(384 by 1) { i =>
        mem(i) = x + i.to[I32]
      }
      Pipe { y := mem(383) }
    }


    // Extract results from accelerator
    val result = getArg(y)

    // Create validation checks and debug code
    val gold = N+383
    println("expected: " + gold)
    println("result: " + result)

    val cksum = gold == result
    println(r"PASS: $cksum (MemTest1D)")
  }
}


@test class MemTest2D extends SpatialTest {
  override def runtimeArgs: Args = "7"

  def main(args: Array[String]): Void = {
    // Declare SW-HW interface vals
    val x = ArgIn[I32]
    val y = ArgOut[I32]
    val N = args(0).to[I32]

    // Connect SW vals to HW vals
    setArg(x, N)

    // Create HW accelerator
    Accel {
      val mem = SRAM[I32](64, 128)
      Sequential.Foreach(64 by 1, 128 by 1) { (i,j) =>
        mem(i,j) = x + (i.to[I32]*128+j.to[I32]).to[I32]
      }
      Pipe { y := mem(63,127) }
    }
    // Extract results from accelerator
    val result = getArg(y)

    // Create validation checks and debug code
    val gold = N+63*128+127
    println("expected: " + gold)
    println("result: " + result)

    val cksum = gold == result
    println("PASS: " + cksum + " (MemTest2D)")
    assert(cksum)
  }
}
