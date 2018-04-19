package spatial.tests.feature

import spatial.dsl._

@test class ArgInOut extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val a = ArgIn[I32]
    val b = ArgOut[I32]
    setArg(a, args(0).to[I32])
    Accel {
      b := a + 4
    }
    println("b = a + 4 = " + getArg(b))
    assert(getArg(b) == a + 4)
  }
}

@test class Niter extends SpatialTest {
  override def runtimeArgs: Args = "10"
  val constTileSize = 16

  def nIterTest(len: I32): I32 = {
    val innerPar = 1 (1 -> 1)
    val tileSize = constTileSize (constTileSize -> constTileSize)

    val N = ArgIn[I32]
    val out = ArgOut[I32]
    setArg(N, len)

    Accel {
      Sequential {
        Sequential.Foreach(N by tileSize){ i =>
          val redMax = Reg[I32](999)
          Pipe{ redMax := min(tileSize, N.value-i) }
          val accum = Reduce(Reg[I32](0.to[I32]))(redMax par innerPar){ ii =>
            val m = (i + ii).to[I32]
            println(r"{i:$i,ii:$ii}: $m")
            m
          }{_+_}
          Pipe {
            println(r"{ii:$i}: accum:$accum")
            out := accum
          }
        }
      }
    }

    getArg(out)
  }

  def main(args: Array[String]): Void = {
    val len = args(0).to[I32]

    val result = nIterTest(len)

    val m = (len-1)%constTileSize + 1
    val b1 = m*(m-1)/2
    val gold = b1 + (len - m)*m
    println("expected: " + gold)
    println("result:   " + result)

    val cksum = gold == result
    println(r"PASS: $cksum (Niter)")
    assert(cksum)
  }
}



@test class NestedLoopTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = Accel {
    val x = SRAM[I32](64)
    Foreach(64 by 32){i =>
      Foreach(32 by 1){j =>
        x(i + j) = i + j
      }
      println("Hello!")
    }
  }
}

@test class IfThenElseTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def test[T:Bits](x: T, y: T): Void = {
    val c = random[Bit]
    val a1 = mux(c, 0, x)
    val a2 = mux(c, x, 0)
    val a3 = mux(c, x, y)
    println(r"a1: $a1, a2: $a2, a3: $a3")

    val b1 = if (c) 0 else x
    val b2 = if (c) x else 0
    val b3 = if (c) x else y
    println(r"b1: $b1, b2: $b2, b3: $b3")
  }

  def main(args: Array[String]): Void = {
    Accel {
      val c = random[Bit]
      val i16 = random[I16]
      val i32 = random[I32]
      val i64 = random[I64]

      val x0 = if (c) 1 else 0
      val x1 = if (c) i32 else 0
      val x2 = if (c) 0 else i32
      val x3 = if (c) i32 else i32
      println(r"x0: $x0, x1: $x1, x2: $x2, x3: $x3")

      //val y0 = if (c) 1 else 0.2
      val y1 = if (c) i16 else 0
      val y2 = if (c) 0 else i16
      val y3 = if (c) i16 else i16
      println(r"y0: N/A, y1: $y1, y2: $y2, y3: $y3")

      val m0 = mux(c, 1, 0)
      val m1 = mux(c, i32, 0)
      val m2 = mux(c, 0, i32)
      val m3 = mux(c, i32, i32)
      println(r"m0: $m0, m1: $m1, m2: $m2, m3: $m3")

      //val n0 = mux(c, 1, 0.2)
      val n1 = mux(c, i16, 0)
      val n2 = mux(c, 0, i16)
      val n3 = mux(c, i16, i16)
      println(r"n0: N/A, n1: $n1, n2: $n2, n3: $n3")

      test(i32, i32)
    }
  }
}

@test class NumericTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    Accel {
      val x = random[Int]
      val y = random[Int]
      val z = -x
      val q = z + y
      val m = z + x
      val f = m + y
      println(q)
      println(m)
      println(f)
    }
  }
}

@test class RegTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val in = ArgIn[Int]
    setArg(in, 0)

    Accel {
      val reg = Reg[Int](0)
      val x = reg + in.value
      val y = in.value - reg
      println(x)
      println(y)
    }
  }
}

@test class SRAMTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    Accel {
      val sram = SRAM[Int](1, 16)
      sram(0, 0) = 10
      println(sram(0, 0))
    }
  }
}

@test class MuxTests extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    Accel {
      val x = random[Int]
      val y = random[Int]
      val z = min(x, y)
      val q = max(x, y)
      val m = min(x, 0)
      val n = max(0, y)
      val p = max(0, 5)

      println("" + z + ", " + q + ", " + m + ", " + n + ", " + p)
    }
  }
}


@test class ReduceTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    Accel {
      val sram = SRAM[Int](1, 16)
      val sum = Reduce(0)(16 by 1) { i => sram(0, i) }{(a, b) => a + b }
      println(sum.value)
    }
  }
}

@test class FoldAccumTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    Accel {
      val product = Reg[Int](1)
      Reduce(product)(16 by 1){i => i } {_ * _}
      val sum2 = Reduce(0)(0 :: 1 :: 16 par 2) { i => i } {_ + _}
      println(product.value)
      println(sum2.value)
    }
  }
}

@test class MemReduceTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    Accel {
      val accum = SRAM[Int](32, 32)
      MemReduce(accum)(0 until 32) { i =>
        val inner = SRAM[Int](32, 32)
        Foreach(0 until 32, 0 until 32) { (j, k) => inner(j, k) = j + k }
        inner
      } { (a, b) => a + b }

      println(accum(0, 0))
    }
  }
}

@test class UtilTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val array = Array.tabulate(32){i => random[Int](10) }
    val matrix = (0::4,0::10){(i,j) => random[Int](10) }

    Accel { }

    printArray(array)
    printMatrix(matrix)
  }

}

@test class UntransferredValueTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val x = random[Int]
    val y = ArgOut[Int]
    Accel {
      y := x
    }
    println(getArg(y))
  }
}

@test class DRAMSizeTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val arr = args.map{a => a.to[Int] }
    val x = DRAM[Int](arr.length)
    val N = ArgIn[Int]
    setArg(N, args.length)
    setMem(x, arr)
    val out = ArgOut[Int]
    Accel {
      out := Reduce(0)(N by 5) { i =>
        val sram = SRAM[Int](12)
        sram load x(i :: i + 5)
        Reduce(0)(5 by 1) { j => sram(j) } {_ + _}
      }{_+_}
    }
    println(getArg(out))
  }
}


@test class SimpleSequential extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def simpleSeq(xIn: I32, yIn: I32): I32 = {
    val innerPar = 1 (1 -> 1)
    val tileSize = 64 (64 -> 64)

    val x = ArgIn[I32]
    val y = ArgIn[I32]
    val out = ArgOut[I32]
    setArg(x, xIn)
    setArg(y, yIn)
    Accel {
      val bram = SRAM[I32](tileSize)
      Foreach(tileSize by 1 par innerPar){ ii =>
        bram(ii) = x.value * ii
      }
      out := bram(y.value)
    }
    getArg(out)
  }

  def main(args: Array[String]): Void = {
    val x = args(0).to[I32]
    val y = args(1).to[I32]
    val result = simpleSeq(x, y)

    val a1 = Array.tabulate(64){i => x * i}
    val gold = a1(y)

    println("expected: " + gold)
    println("result:   " + result)
    val chkSum = result == gold
    assert(chkSum)
  }
}
