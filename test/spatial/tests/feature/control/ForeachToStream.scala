package spatial.tests.feature.control

import spatial.dsl._
import spatial.metadata.memory._

object ForeachToStream {

  @forge.tags.api def doLotsOfCompute(depth: scala.Int, mod: scala.Int = 31)(x: I32, m: I32): I32 = {
    if (depth <= 0) x
    else doLotsOfCompute(depth - 1)(((x + 1) * m) % mod, m)
  }

  val gold = Seq(4,5,6,7,15,19,23,27,3,12,21,30,23,8,24,9,25,19,13,7,5,10,15,20,1,19,6,24,27,29,0,2,7,26,14,2,19,26,2,9,12,9,6,3,19,8,28,17,29,12,26,9,14,24,3,13,18,26,3,11,29,6,14,22) map {I32(_)}
}

@spatial class ForeachToStream(numConsumers: scala.Int, ip: scala.Int = 1, op: scala.Int = 1) extends SpatialTest {

  val iters = 16
  val innerIters = 4

  def main(args: Array[String]): Unit = {
    val memOuts = Range(0, numConsumers) map {_ => DRAM[I32](iters, innerIters)}
    Accel {
      val mems = Range(0, numConsumers) map {_ => SRAM[I32](iters, innerIters)}
      // construct a long unitpipe followed by another controller
      'Outer.Pipe.Foreach(iters by 1 par op) {
        i =>
          val k = ForeachToStream.doLotsOfCompute(2)(i, i+1)
          Range(0, numConsumers) foreach {
            n =>
              'Inner.Foreach(innerIters by 1 par ip) {
                j =>
                  // A Few iterations of a deeeeep pipeline
                  val tmp = k + j
                  mems(n)(i, j) = ForeachToStream.doLotsOfCompute(2)(tmp, i+1) + n
              }
          }

      }
      Parallel {
        memOuts zip mems foreach {
          case (d, s) => d store s
        }
      }
    }

    val goldArray = Array(ForeachToStream.gold:_*).reshape(iters, innerIters)
    Range(0, numConsumers) foreach {
      n =>
        val recv = getMatrix(memOuts(n))
        Foreach(iters by 1, innerIters by 1) {
          (i, j) =>
            assert(goldArray(i, j) + n == recv(i, j), r"Gold($i, $j) = ${goldArray(i, j) + n}, Recv($i, $j) = ${recv(i, j)}")
        }
    }

    assert(Bit(true))
  }
}

trait Streamified {
  this: SpatialTest =>
  override def compileArgs: Args = "--streamify --max_cycles=10000"
}

class ForeachToStream1 extends ForeachToStream(1)
class ForeachToStream2 extends ForeachToStream(2)
class ForeachToStream10 extends ForeachToStream(10)

class ForeachToStream1_streamed extends ForeachToStream1 with Streamified
class ForeachToStream2_streamed extends ForeachToStream2 with Streamified
class ForeachToStream10_streamed extends ForeachToStream10 with Streamified

class ForeachToStream1p2 extends ForeachToStream(1, 1, 2)
class ForeachToStream1p2_streamed extends ForeachToStream1p2 with Streamified