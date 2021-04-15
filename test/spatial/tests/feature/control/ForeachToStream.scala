package spatial.tests.feature.control

import spatial.dsl._
import spatial.metadata.memory._

object ForeachToStream {

  @forge.tags.api def doLotsOfCompute(depth: scala.Int, mod: scala.Int = 31)(x: I32, m: I32): I32 = {
    if (depth <= 0) x
    else doLotsOfCompute(depth - 1)(((x + 1) * m) % mod, m)
  }
}

/*
4,5,6,7,15,19,23,27,3,12,21,30,23,8,24,9,25,19,13,7,5,10,15,20,1,19,6,24,27,29,0,2,7,26,14,2,19,26,2,9,12,9,6,3,19,8,28,17,29,12,26,9,14,24,3,13,18,26,3,11,29,6,14,22
 */

@spatial class ForeachToStream1 extends SpatialTest {

  override def compileArgs = s"--max_cycles=10000 --vv"

  val iters = 16
  val innerIters = 4
  
  val gold = Seq[I32](4,5,6,7,15,19,23,27,3,12,21,30,23,8,24,9,25,19,13,7,5,10,15,20,1,19,6,24,27,29,0,2,7,26,14,2,19,26,2,9,12,9,6,3,19,8,28,17,29,12,26,9,14,24,3,13,18,26,3,11,29,6,14,22)

  def main(args: Array[String]): Unit = {
    val memOut = DRAM[I32](iters, innerIters)
    Accel {
      val mem = SRAM[I32](iters, innerIters)
      // construct a long unitpipe followed by another controller
      'Outer.Pipe.Foreach(iters by 1) {
        i =>
          val k = ForeachToStream.doLotsOfCompute(2)(i, i+1)
          'Inner.Foreach(innerIters by 1) {
            j =>
              // A Few iterations of a deeeeep pipeline
              val tmp = k + j
              mem(i, j) = ForeachToStream.doLotsOfCompute(2)(tmp, i+1)
          }
      }
      memOut store mem
    }

    printMatrix(getMatrix(memOut))
    val recv = getMatrix(memOut)
    val goldArray = Array(gold:_*).reshape(iters, innerIters)
    Foreach(iters by 1, innerIters by 1) {
      (i, j) =>
        assert(goldArray(i, j) == recv(i, j), r"Gold($i, $j) = ${goldArray(i, j)}, Recv($i, $j) = ${recv(i, j)}")
    }
    assert(Bit(true))
  }
}

@spatial class ForeachToStream2 extends SpatialTest {

  override def compileArgs = s"--max_cycles=10000 --vv"

  val iters = 16
  val innerIters = 4

  val gold = Seq[I32](4,5,6,7,15,19,23,27,3,12,21,30,23,8,24,9,25,19,13,7,5,10,15,20,1,19,6,24,27,29,0,2,7,26,14,2,19,26,2,9,12,9,6,3,19,8,28,17,29,12,26,9,14,24,3,13,18,26,3,11,29,6,14,22)

  def main(args: Array[String]): Unit = {
    val memOut = DRAM[I32](iters, innerIters)
    val memOut2 = DRAM[I32](iters, innerIters)
    Accel {
      val mem = SRAM[I32](iters, innerIters)
      val mem2 = SRAM[I32](iters, innerIters)
      // construct a long unitpipe followed by another controller
      'Outer.Pipe.Foreach(iters by 1) {
        i =>
          val k = ForeachToStream.doLotsOfCompute(2)(i, i+1)
          'Inner.Foreach(innerIters by 1) {
            j =>
              // A Few iterations of a deeeeep pipeline
              val tmp = k + j
              mem(i, j) = ForeachToStream.doLotsOfCompute(2)(tmp, i+1)
          }

          'Inner.Foreach(innerIters by 1) {
            j =>
              // A Few iterations of a deeeeep pipeline
              val tmp = k + j
              mem2(i, j) = ForeachToStream.doLotsOfCompute(2)(tmp, i+1) + 1
          }
      }
      Pipe {
        memOut store mem
        memOut2 store mem2
      }
    }

    val recv = getMatrix(memOut)
    val goldArray = Array(gold:_*).reshape(iters, innerIters)
    Foreach(iters by 1, innerIters by 1) {
      (i, j) =>
        assert(goldArray(i, j) == recv(i, j), r"Gold($i, $j) = ${goldArray(i, j)}, Recv($i, $j) = ${recv(i, j)}")
    }
    val recv2 = getMatrix(memOut2)
    Foreach(iters by 1, innerIters by 1) {
      (i, j) =>
        assert(goldArray(i, j) + 1 == recv2(i, j), r"Gold($i, $j) = ${goldArray(i, j) + 1}, Recv($i, $j) = ${recv2(i, j)}")
    }
    assert(Bit(true))
  }
}