package spatial.tests.dse

import spatial.dsl._
import com.typesafe.config.ConfigFactory
import pureconfig._
import spatial.util.spatialConfig
import spatial.metadata.params._
import scala.reflect.ClassTag

class BasicParamsV1 extends BasicParams(1,4,64,8,true)
class BasicParamsV2 extends BasicParams(1,4,64,8,false)
class BasicParamsV3 extends BasicParams(2,4,64,8,true)


@spatial abstract class BasicParams(val op: scala.Int, val ip: scala.Int, val bs: scala.Int, val lp: scala.Int, pipeline: scala.Boolean) extends SpatialTest {
  override def dseModelArgs: Args = "640"
  override def finalModelArgs: Args = "640"
  override def runtimeArgs: Args = "640"
  type X = FixPt[TRUE,_32,_0]

  def dotproduct[T:Num](aIn: Array[T], bIn: Array[T]): T = {
    // Can be overwritten using --param-path=fileName at command line
    val OP = op (1 -> 3)
    val IP = ip (2 -> 2 -> 8)
    val B  = bs (32 -> 32 -> 192)
    val LP = lp (1, 2, 4, 8, 16)

    //saveParams(s"$SPATIAL_HOME/saved.param") // Store used params to file

    val size = aIn.length
    val sizePlus1 = size + 1; bound(sizePlus1) = 640

    val N = ArgIn[Int]
    val tileSizeAsArg = ArgIn[Int]
    setArg(N, size + 1)
    setArg(tileSizeAsArg, B)

    val a = DRAM[T](N)
    val b = DRAM[T](N)
    val out0 = ArgOut[T]
    setMem(a, aIn)
    setMem(b, bIn)

    Accel {
      val accO = Reg[T](0.to[T])
      if (pipeline) {
        out0 := Reduce(accO)(N by B par OP){i =>
          val aBlk = SRAM[T](B)
          val bBlk = SRAM[T](B)
          Parallel {
            Pipe{if (size > 0) aBlk load a(i::i+tileSizeAsArg par LP)}
            bBlk load b(i::i+B par LP)
          }
          val accI = Reg[T](0.to[T])
          Reduce(accI)(B par IP){ii => aBlk(ii) * bBlk(ii) }{_+_}
        }{_+_}
      }
      else {
        out0 := Sequential.Reduce(accO)(N by B par OP){i =>
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
    println("PASS: " + cksum + " (BasicParams)")
    assert(cksum)
  }
}
