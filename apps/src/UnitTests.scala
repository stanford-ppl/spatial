import spatial.dsl._

@spatial object ArgInOut {

  def main(): Void = {
    val a = ArgIn[I32]
    val b = ArgOut[I32]
    setArg(a, args(0).to[I32])
    Accel {
      b := a + 4
    }
    println("b = a + 4 = " + getArg(b))
    println(r"PASS: ${getArg(b) == a + 4} (ArgInOut)")
  }
}

@spatial object Niter { // Regression (Unit) // Args: 100
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

  def main(): Void = {
    val len = args(0).to[I32]

    val result = nIterTest(len)

    val m = (len-1)%constTileSize + 1
    val b1 = m*(m-1)/2
    val gold = b1 + (len - m)*m
    println("expected: " + gold)
    println("result:   " + result)

    val cksum = gold == result
    println(r"PASS: $cksum (Niter)")
  }
}

@spatial object MemTest1D { // Regression (Unit) // Args: 7

  def main(): Void = {

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


@spatial object MemTest2D { // Regression (Unit) // Args: 7


  def main(): Void = {

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
    //println("PASS: " + cksum + " (MemTest2D)")
  }
}
