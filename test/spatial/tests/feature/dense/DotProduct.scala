package spatial.tests.feature.dense

import spatial.dsl._

@spatial class DotProduct extends SpatialTest {
  override def dseModelArgs: Args = "640"
  override def finalModelArgs: Args = "640"
  override def runtimeArgs: Args = "640"
  override def compileArgs: Args = super.compileArgs and "--noBindParallels"
  type X = FixPt[TRUE,_32,_0]

  def dotproduct[T:Num](aIn: Array[T], bIn: Array[T]): T = {
    // Can be overwritten using --param-path=fileName at command line
    val ip = 4 (1 -> 192)
    val op = 2 (1 -> 6)
    val ts  = 32 (32 -> 64 -> 19200)
    val loadPar = 4 (1 -> 1 -> 16)

    val B = ts
    val P1 = op
    val P2 = ip
    val P3 = loadPar

    //saveParams(s"$SPATIAL_HOME/saved.param") // Store used params to file

    val size = aIn.length; bound(size) = 1920000

    val N = ArgIn[Int]
    setArg(N, size)

    val a = DRAM[T](N)
    val b = DRAM[T](N)
    val out0 = ArgOut[T]
    setMem(a, aIn)
    setMem(b, bIn)

    Accel {
      val accO = Reg[T](0.to[T])
      out0 := Reduce(accO)(N by B par P1){i =>
        //val ts = Reg[Int](0)
        //ts := min(B, N-i)
        val aBlk = SRAM[T](B)
        val bBlk = SRAM[T](B)
        //aBlk load a(i::i+ts.value par P3)
        //bBlk load b(i::i+ts.value par P3)
        aBlk load a(i::i+B par P3)
        bBlk load b(i::i+B par P3)
        val accI = Reg[T](0.to[T])
        Reduce(accI)(B par P2){ii => aBlk(ii) * bBlk(ii) }{_+_}
      }{_+_}
    }
    getArg(out0)
  }


  def main(args: Array[String]): Unit = {
    val N = args(0).to[Int]
    val a = Array.fill(N){ random[X](4) }
    val b = Array.fill(N){ random[X](4) }

    val result0 = dotproduct(a, b)
    val gold = a.zip(b){_*_}.reduce{_+_}

    println("expected: " + gold)
    println("result0: " + result0)

    val cksum = gold == result0
    println("PASS: " + cksum + " (DotProduct)")
    assert(cksum)
  }
}

@spatial class DotProductStream extends SpatialTest {
  override def runtimeArgs: Args = "1280 256"
  type T = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {
    val ip = loadParam("ip", default=4 (1 -> 192))
    val op = loadParam("op", default=1 (1 -> 6))
    val ts  = loadParam("ts", default=32 (32 -> 64 -> 19200))
    val loadPar = loadParam("loadPar", default=4 (1 -> 1 -> 16))

    val BS = ts
    val P1 = op
    val P2 = ip
    val P3 = loadPar

    val N = args(0).to[Int]
    val dynB = args(1).to[Int]
    val A = Array.fill(N){ random[T](4) }
    val B = Array.fill(N){ random[T](4) }

    val a = DRAM[T](N)
    val b = DRAM[T](N)
    val out0 = ArgOut[T]
    // val out1 = ArgOut[T]
    val n = ArgIn[Int]
    val dynBlockSize = ArgIn[Int]
    setArg(dynBlockSize, dynB)
    setArg(n, N)
    setMem(a, A)
    setMem(b, B)

    Accel {
      // Set up accumulator and FIFOs
      val accO = Reg[T](0.to[T])

      // Create stream controller
      //  Choose to block because it is possible for DRAM
      //  controller to attempt to service one load more
      //  than the other, but we need both to be serviced
      //  equally to avoid stalling the Stream controller
      Stream{
        val aBlk = FIFO[T](BS)
        val bBlk = FIFO[T](BS)


        // Load data
        Foreach(n by dynBlockSize){blk => 
          val thisblk = min(dynBlockSize.value, n-blk)
          Parallel {
            // Handle edge case
            aBlk load a(blk::blk + thisblk par P3)
            bBlk load b(blk::blk + thisblk par P3)
          }
        }

        // Greedily consume data
        Reduce(accO)(n by 1 par P1){i => aBlk.deq() * bBlk.deq()}{_+_}
      }
      // Copy result out
      out0 := accO

    }

    val result0 = getArg(out0)
    // val result1 = getArg(out1)
    val gold = A.zip(B){_*_}.reduce{_+_}

    println("expected: " + gold)
    println("result0: " + result0)
    // println("result1: " + result1)

    val cksum = gold == result0// && gold == result1
    println("PASS: " + cksum + " (DotProduct)")
    assert(cksum)
  }
}

@spatial class DotProductFlt extends SpatialTest {
  override def runtimeArgs: Args = "640"
  type X = Float //FixPt[TRUE,_32,_0]

  val innerPar = 4
  val outerPar = 1
  val tileSize = 32
  val margin = 0.3f

  def dotproduct[T:Num](aIn: Array[T], bIn: Array[T]): T = {
    val B  = tileSize (32 -> 64 -> 19200)
    val P1 = outerPar (1 -> 6)
    val P2 = innerPar (1 -> 192)
    val P3 = innerPar (1 -> 192)

    val size = aIn.length; bound(size) = 1920000

    val N = ArgIn[Int]
    setArg(N, size)

    val a = DRAM[T](N)
    val b = DRAM[T](N)
    val out = ArgOut[T]
    setMem(a, aIn)
    setMem(b, bIn)

    Accel {
      val accO = Reg[T](0.to[T])
      out := Reduce(accO)(N by B par P1){i =>
        //val ts = Reg[Int](0)
        //ts := min(B, N-i)
        val aBlk = SRAM[T](B)
        val bBlk = SRAM[T](B)
        Parallel {
          //aBlk load a(i::i+ts.value par P3)
          //bBlk load b(i::i+ts.value par P3)
          aBlk load a(i::i+B par P3)
          bBlk load b(i::i+B par P3)
        }
        val accI = Reg[T](0.to[T])
        Reduce(accI)(B par P2){ii => aBlk(ii) * bBlk(ii) }{_+_}
      }{_+_}
    }
    getArg(out)
  }


  def main(args: Array[String]): Unit = {
    val N = args(0).to[Int]
    val a = Array.fill(N){ random[X](4) }
    val b = Array.fill(N){ random[X](4) }

    val result = dotproduct(a, b)
    val gold = a.zip(b){_*_}.reduce{_+_}

    println("expected: " + gold)
    println("result: " + result)

    val cksum = abs(gold - result) < margin
    println("PASS: " + cksum + " (DotProduct)")
    assert(cksum)
  }
}

import utils.io.files._
//class DotProductSynthDSE_test extends DotProductSynthDSE("/home/mattfel/sp_fixes/spatial/test_cfg.csv")
@spatial abstract class DotProductSynthDSE(params_file: java.lang.String) extends SpatialTest {
  override def runtimeArgs: Args = "640"
  type X = FixPt[TRUE,_32,_0]

  def main(args: Array[String]): Unit = {
    val size = args(0).to[Int]
    val aData = Array.fill(size){ random[X](4) }
    val bData = Array.fill(size){ random[X](4) }

    val config = loadCSVNow[java.lang.String](params_file, ","){x => x}

    val P2 = config(0).toInt
    val P1 = config(1).toInt
    val B = config(2).toInt
    val P3 = config(3).toInt
    val schedule = config(4).toBoolean

    //saveParams(s"$SPATIAL_HOME/saved.param") // Store used params to file

    val N = ArgIn[Int]
    setArg(N, size)

    val a = DRAM[X](N)
    val b = DRAM[X](N)
    val out0 = ArgOut[X]
    setMem(a, aData)
    setMem(b, bData)

    Accel {
      val accO = Reg[X](0.to[X])
      val sum = if (schedule == true) {
        Reduce(accO)(N by B par P1){i =>
          //ts := min(B, N-i)
          val aBlk = SRAM[X](B)
          val bBlk = SRAM[X](B)
          aBlk load a(i::i+B par P3)
          bBlk load b(i::i+B par P3)
          val accI = Reg[X](0.to[X])
          Reduce(accI)(B par P2){ii => aBlk(ii) * bBlk(ii) }{_+_}
        }{_+_}
      } else {
        Sequential.Reduce(accO)(N by B par P1){i =>
          //ts := min(B, N-i)
          val aBlk = SRAM[X](B)
          val bBlk = SRAM[X](B)
          aBlk load a(i::i+B par P3)
          bBlk load b(i::i+B par P3)
          val accI = Reg[X](0.to[X])
          Reduce(accI)(B par P2){ii => aBlk(ii) * bBlk(ii) }{_+_}
        }{_+_}
      }
      out0 := sum
    }

    val result0 = getArg(out0)
    val gold = aData.zip(bData){_*_}.reduce{_+_}

    println("expected: " + gold)
    println("result0: " + result0)

    val cksum = gold == result0
    println("PASS: " + cksum + " (DotProduct)")
    assert(cksum)
  }
}