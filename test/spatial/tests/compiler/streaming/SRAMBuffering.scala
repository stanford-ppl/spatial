package spatial.tests.compiler.streaming
import spatial.dsl._

@spatial class SRAMBufferingSimple extends SpatialTest {
  override def compileArgs = "--max_cycles=100000"

  val outerIters = 4
  val innerIters = 128

  override def main(args: Array[String]) = {
//    implicitly[argon.State].config.stop = 50
    val output = DRAM[I32](outerIters, innerIters)
    val output2 = DRAM[I32](outerIters, innerIters)
    Accel {
      val outputSR = SRAM[I32](outerIters, innerIters)
      val outputSR2 = SRAM[I32](outerIters, innerIters)
      Foreach(0 until outerIters by 1 par 2) {
        outer =>
          val sr = SRAM[I32](innerIters)
          'Producer.Foreach(0 until innerIters by 1) {
            inner =>
              sr(inner) = inner + outer
          }

          Parallel {
            'Consumer.Foreach(0 until innerIters by 1) {
              inner =>
                outputSR(outer, inner) = sr(inner)
            }

            'Consumer2.Foreach(0 until innerIters by 1) {
              inner =>
                outputSR2(outer, inner) = sr(inner) + 1
            }
          }
      }
      output store outputSR(0::outerIters, 0::innerIters)
      output2 store outputSR2(0::outerIters, 0::innerIters)
    }
    printMatrix(getMatrix(output))
    val reference = Matrix.tabulate(outerIters, innerIters) {
      (outer, inner) => outer + inner
    }
    assert(checkGold(output, reference))
    printMatrix(getMatrix(output2))
    assert(Bit(true))
  }
}

class SRAMBufferingSimpleNoStream extends SRAMBufferingSimple {
  override def compileArgs = super.compileArgs + "--nostreamify"
}

class SRAMTransfer extends SpatialTest {
  val transferSize = 32
  override def compileArgs = "--max_cycles=500"
  override def main(args: Array[String]): Void = {
    val input = DRAM[I32](transferSize)
    val output = DRAM[I32](transferSize)
    val gold = Array.tabulate(transferSize) {i => i}
    setMem(input, gold)
    Accel {
      val sr = SRAM[I32](transferSize)
      Pipe {sr load input}
      Pipe {output store sr}
    }
    assert(checkGold(output, gold))
    assert(Bit(true))
  }
}

class SRAMTransferNS extends SRAMTransfer {
  override def compileArgs = super.compileArgs + "--nostreamify"
}

class SRAMStore extends SpatialTest {
  override def compileArgs = "--max_cycles=500"
  override def main(args: Array[String]): Void = {
    val output = DRAM[I32](32)
    Accel {
      val sr = SRAM[I32](32)
      Foreach(0 until 32) {
        i => sr(i) = i
      }
      output store sr
    }
    val gold = Array.tabulate(32) {i => i}
    assert(checkGold(output, gold))
  }
}

class SRAMLoad extends SpatialTest {
  override def compileArgs = "--max_cycles=1000"
  override def main(args: Array[String]): Void = {
    val input = DRAM[I32](32)
    val data = Array.tabulate(32) { i => i }
    setMem(input, data)
    val output = ArgOut[I32]
    Accel {
      val sr = SRAM[I32](32)
      sr load input
      val tmp = Reg[I32](0)
      Foreach(0 until 32) {
        i => tmp := tmp + sr(i)
      }
      output := tmp
    }
    val gold = data.reduce { case (i, j) => i + j }
    assert(checkGold(output, gold))
  }
}

class SRAMParFactor(ip: scala.Int, op: scala.Int) extends SpatialTest {
  override def compileArgs = "--max_cycles=2500"

  override def main(args: Array[String]): Void = {
    val output = DRAM[I32](32, 32)
    Accel {
      Foreach(0 until 1) { x =>
        val sr = SRAM[I32](32, 32)
        Foreach(0 until 32 par op) {
          i =>
            Foreach(0 until 32 par ip) {
              j => sr(i, j) = i * j + 1
            }
        }
        output store sr(0 :: 32, 0 :: 32)
      }
    }
    val gold = Matrix.tabulate(32, 32) { (i, j) => i*j + 1 }
    assert(checkGold(output, gold))
  }
}

class SRAMParFactorNS(ip: scala.Int, op: scala.Int) extends SRAMParFactor(ip, op) {
  override def compileArgs = super.compileArgs + "--nostreamify"
}

class SRAMParFactorIP extends SRAMParFactor(4, 1)
class SRAMParFactorIPNS extends SRAMParFactorNS(4, 1)

class SRAMParFactorOP extends SRAMParFactor(1, 4)
class SRAMParFactorOPNS extends SRAMParFactorNS(1, 4)

class SRAMParFactorNoPar extends SRAMParFactor(1, 1)

class SRAMParFactorAllPar extends SRAMParFactor(8, 8)
class SRAMParFactorAllParNS extends SRAMParFactorNS(8, 8)
