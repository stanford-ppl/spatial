package spatial.tests.feature.dense

import spatial.dsl._
import com.typesafe.config.ConfigFactory
import pureconfig._
import spatial.util.spatialConfig
import spatial.metadata.params._
import scala.reflect.ClassTag

@spatial class DotProduct extends SpatialTest {
  override def runtimeArgs: Args = "640"
  type X = FixPt[TRUE,_32,_0]

  def dotproduct[T:Num](aIn: Array[T], bIn: Array[T]): T = {
    // Can be overwritten using --param-path=fileName at command line
    // val ip = loadParam("ip", default=696 (1 -> 192))
    // val op = loadParam("op", default=2 (1 -> 6))
    // val ts  = loadParam("ts", default=32 (32 -> 64 -> 19200))
    // val loadPar = loadParam("loadPar", default=4 (1 -> 1 -> 16))
    val ip = loadParam("ip", default=696 (192 -> 192))
    val op = loadParam("op", default=2 (5 -> 6))
    val ts  = loadParam("ts", default=32 (19200 -> 64 -> 19200))
    val loadPar = loadParam("loadPar", default=4 (16 -> 1 -> 16))

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

    val cksum = gold == result
    println("PASS: " + cksum + " (DotProduct)")
    assert(cksum)
  }
}


@spatial class dsetest extends SpatialTest {
  override def runtimeArgs: Args = "640"
  type X = FixPt[TRUE,_32,_0]

  def dotproduct[T:Num](aIn: Array[T], bIn: Array[T]): T = {
    // Can be overwritten using --param-path=fileName at command line
    val ip = loadParam("ip", default=2 (1 -> 1))
    val op = loadParam("op", default=2 (6 -> 6))
    val ts  = loadParam("ts", default=32 (32 -> 64 -> 32))
    val loadPar = loadParam("loadPar", default=4 (1 -> 1 -> 16))

    val B = ts
    val P1 = op
    val P2 = ip
    val P3 = loadPar

    //saveParams(s"$SPATIAL_HOME/saved.param") // Store used params to file

    val size = aIn.length; bound(size) = 1920

    val N = ArgIn[Int]
    setArg(N, size)

    val a = DRAM[T](N)
    val b = DRAM[T](N)
    val out = ArgOut[T]
    setMem(a, aIn)
    setMem(b, bIn)

    Accel {
      val accO = Reg[T](0.to[T])
      out := Sequential.Reduce(accO)(N by B par P1){i =>
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

    val cksum = gold == result
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
