package spatial.tests.feature.dense

import spatial.dsl._
import com.typesafe.config.ConfigFactory
import pureconfig._
import spatial.util.spatialConfig
import spatial.metadata.params._
import scala.reflect.ClassTag

@spatial class BasicParams extends SpatialTest {
  override def dseModelArgs: Args = "640"
  override def finalModelArgs: Args = "640"
  override def runtimeArgs: Args = "640"
  type X = FixPt[TRUE,_32,_0]

  def dotproduct[T:Num](aIn: Array[T], bIn: Array[T]): T = {
    // Can be overwritten using --param-path=fileName at command line
    val OP = 1 (1 -> 2)
    val IP = 2 (2 -> 2 -> 8)
    val B  = 32 (32 -> 64 -> 192)
    val LP = 4 (1 -> 4)

    //saveParams(s"$SPATIAL_HOME/saved.param") // Store used params to file

    val size = aIn.length; bound(size) = 19200

    val N = ArgIn[Int]
    setArg(N, size)

    val a = DRAM[T](N)
    val b = DRAM[T](N)
    val out0 = ArgOut[T]
    setMem(a, aIn)
    setMem(b, bIn)

    Accel {
      val accO = Reg[T](0.to[T])
      out0 := Reduce(accO)(N by B par OP){i =>
        val aBlk = SRAM[T](B)
        val bBlk = SRAM[T](B)
        Parallel {
          aBlk load a(i::i+B par LP)
          bBlk load b(i::i+B par LP)
        }
        val accI = Reg[T](0.to[T])
        Reduce(accI)(B par IP){ii => aBlk(ii) * bBlk(ii) }{_+_}
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
