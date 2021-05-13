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
            printMatrix(recv, header = s"Recv: $n")
            assert(goldArray(i, j) + n == recv(i, j), r"Gold($i, $j) = ${goldArray(i, j) + n}, Recv($i, $j) = ${recv(i, j)}")
        }
    }

    assert(Bit(true))
  }
}

@spatial class ForeachToStreamSRAM(numConsumers: scala.Int, ip: scala.Int = 1, op: scala.Int = 1, bufferDepth: Option[scala.Int] = Some(1)) extends SpatialTest {

  val iters = 16
  val innerIters = 4

  def main(args: Array[String]): Unit = {
    val memOuts = Range(0, numConsumers) map {_ => DRAM[I32](iters, innerIters)}
    Accel {
      val mems = Range(0, numConsumers) map {_ => SRAM[I32](iters, innerIters)}
      // construct a long unitpipe followed by another controller
      'Outer.Pipe.Foreach(iters by 1 par op) {
        i =>
          val sram = SRAM[I32](innerIters)
          bufferDepth match {
            case Some(v) => sram.bufferAmount = v
            case None =>
          }

          'Producer.Foreach(innerIters by 1 par ip) {
            j =>
              sram(j) = i + j
          }

          Range(0, numConsumers) foreach {
            n =>
              'Inner.Foreach(innerIters by 1 par ip) {
                j =>
                  mems(n)(i, j) = sram(j) + 2*j + 7*n
              }
          }
      }
      Parallel {
        memOuts zip mems foreach {
          case (d, s) => d store s
        }
      }
    }

    Range(0, numConsumers) foreach {
      n =>
        val recv = getMatrix(memOuts(n))
        printMatrix(recv, header = s"Recv: $n")
        Foreach(iters by 1, innerIters by 1) {
          (i, j) =>
            val fetched = recv(i, j)
            val gold = i + 3*j + 7*n
            assert(fetched == gold, r"($n, $i, $j), Fetched: $fetched, Gold: $gold")
        }
    }
    assert(Bit(true))
  }
}

trait Streamified {
  this: SpatialTest =>
  override def compileArgs: Args = "--streamify --max_cycles=10000 --fdot"
}

class ForeachToStream1 extends ForeachToStream(1)
class ForeachToStream2 extends ForeachToStream(2)

class ForeachToStream1_streamed extends ForeachToStream1 with Streamified
class ForeachToStream2_streamed extends ForeachToStream2 with Streamified

class ForeachToStream1op2 extends ForeachToStream(1, 1, 2)
class ForeachToStream1op2_streamed extends ForeachToStream1op2 with Streamified

class ForeachToStream1ip2 extends ForeachToStream(1, 2, 1)
class ForeachToStream1ip2_streamed extends ForeachToStream1ip2 with Streamified

class ForeachToStream1ip4 extends ForeachToStream(1, 4, 1)
class ForeachToStream1ip4_streamed extends ForeachToStream1ip4 with Streamified

class ForeachToStreamExp extends ForeachToStream(2, 2, 2)
class ForeachToStreamExp_streamed extends ForeachToStreamExp with Streamified

class ForeachToStreamSRAM1 extends ForeachToStreamSRAM(1)
class ForeachToStreamSRAM2 extends ForeachToStreamSRAM(2)

class ForeachToStreamSRAM1_streamed extends ForeachToStreamSRAM1 with Streamified
class ForeachToStreamSRAM2_streamed extends ForeachToStreamSRAM2 with Streamified

class ForeachToStreamSRAM1op2 extends ForeachToStreamSRAM(1, 1, 2)
class ForeachToStreamSRAM1op2_streamed extends ForeachToStreamSRAM1op2 with Streamified

class ForeachToStreamSRAM1ip2 extends ForeachToStreamSRAM(1, 2, 1)
class ForeachToStreamSRAM1ip2_streamed extends ForeachToStreamSRAM1ip2 with Streamified

class ForeachToStreamSRAM1ip4 extends ForeachToStreamSRAM(1, 4, 1)
class ForeachToStreamSRAM1ip4_streamed extends ForeachToStreamSRAM1ip4 with Streamified

class ForeachToStreamSRAMBuffered(bufferDepth: scala.Int) extends ForeachToStreamSRAM(2, 2, 2, Some(bufferDepth))
class ForeachToStreamSRAMBuffered1 extends ForeachToStreamSRAMBuffered(1)
class ForeachToStreamSRAMBuffered2 extends ForeachToStreamSRAMBuffered(2)
class ForeachToStreamSRAMBuffered4 extends ForeachToStreamSRAMBuffered(4)
class ForeachToStreamSRAMBuffered8 extends ForeachToStreamSRAMBuffered(8)

class ForeachToStreamSRAMBufferedStreamed(bufferDepth: scala.Int) extends ForeachToStreamSRAM(2, 2, 2, Some(bufferDepth)) with Streamified
class ForeachToStreamSRAMBufferedStreamed1 extends ForeachToStreamSRAMBufferedStreamed(1)
class ForeachToStreamSRAMBufferedStreamed2 extends ForeachToStreamSRAMBufferedStreamed(2)
class ForeachToStreamSRAMBufferedStreamed4 extends ForeachToStreamSRAMBufferedStreamed(4)
class ForeachToStreamSRAMBufferedStreamed8 extends ForeachToStreamSRAMBufferedStreamed(8)

class ForeachToStreamSRAMBufferedStreamed3 extends ForeachToStreamSRAMBufferedStreamed(3)

class ForeachToStreamSRAMBufferedStreamedAuto extends ForeachToStreamSRAM(2, 2, 2, None) with Streamified