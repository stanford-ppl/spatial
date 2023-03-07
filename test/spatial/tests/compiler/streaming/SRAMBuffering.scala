package spatial.tests.compiler.streaming
import spatial.dsl._

trait NoStream extends SpatialTest {
  override def compileArgs = super.compileArgs + "--nostreamify"
}

@spatial class SRAMBufferingSimple extends SpatialTest {
  override def compileArgs = "--max_cycles=20000"

  val outerIters = 16
  val innerIters = 16

  val numConsumers = 4

  override def main(args: Array[String]) = {
//    implicitly[argon.State].config.setV(3)
    val outputs = Seq.fill(numConsumers){DRAM[I32](outerIters, innerIters)}
    Accel {
      val outputSRs = Seq.fill(numConsumers) {SRAM[I32](outerIters, innerIters)}
      Foreach(0 until outerIters by 1) {
        outer =>
          val sr = SRAM[I32](innerIters)
          'Producer.Foreach(0 until innerIters by 1) {
            inner =>
              sr(inner) = inner + outer
          }

          Parallel {
            Seq.tabulate(numConsumers) {
              consumer =>
                'Consumer.Foreach(0 until innerIters by 1) {
                  inner =>
                    outputSRs(consumer)(outer, inner) = sr(inner) + consumer
                }
            }
          }
      }
      (outputs zip outputSRs).foreach {
        case (dr, sr) => dr store sr
      }
    }

    outputs.zipWithIndex.foreach {
      case (output, ind) =>
        val reference = Matrix.tabulate(outerIters, innerIters) {
          (outer, inner) => outer + inner + ind
        }
        assert(checkGold(output, reference))
    }
    assert(Bit(true))
  }
}

class SRAMBufferingSimpleNoStream extends SRAMBufferingSimple {
  override def compileArgs = super.compileArgs + "--nostreamify"
}

class SRAMTransfer extends SpatialTest {
  val transferSize = 128
  override def compileArgs = "--max_cycles=4000"
  override def main(args: Array[String]): Void = {
    val input = DRAM[I32](transferSize)
    val output = DRAM[I32](transferSize)
    val gold = Array.tabulate(transferSize) {i => i}
    setMem(input, gold)
    Accel {
      val sr = SRAM[I32](transferSize)
      Pipe {
        sr load input
      }
      Pipe {
        output store sr
      }
    }
    assert(checkGold(output, gold))
    assert(Bit(true))
  }
}

class SRAMTransferNS extends SRAMTransfer {
  override def compileArgs = super.compileArgs + "--nostreamify"
}

class SRAMRMW extends SpatialTest {
  val transferSize = 128
  override def compileArgs = "--max_cycles=4000"
  override def main(args: Array[String]): Void = {
    val input = DRAM[I32](transferSize)
    val output = DRAM[I32](transferSize)
    val inVals = Array.tabulate(transferSize){
      i => i
    }

    setMem(input, inVals)
    Accel {
      val sr = SRAM[I32](transferSize).conflictable
      sr load input
      Foreach(0 until transferSize) {
        i => sr(i) = sr(i) + 8
      }
      output store sr
    }
    val gold = Array.tabulate(transferSize) {i => i + 8}
    assert(checkGold(output, gold))
    assert(Bit(true))
  }
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
      val sr = SRAM[I32](32, 32)
      Foreach(0 until 32 par op) {
        i =>
          Foreach(0 until 32 par ip) {
            j => sr(i, j) = i * j + 1
          }
      }
      output store sr(0 :: 32, 0 :: 32)
    }
    val gold = Matrix.tabulate(32, 32) { (i, j) => i*j + 1 }
    assert(checkGold(output, gold))
  }
}

class SRAMParFactorIP extends SRAMParFactor(4, 1)
class SRAMParFactorIPNS extends SRAMParFactorIP with NoStream

class SRAMParFactorOP extends SRAMParFactor(1, 4)
class SRAMParFactorOPNS extends SRAMParFactorOP with NoStream

class SRAMParFactorAllPar extends SRAMParFactor(8, 8)
class SRAMParFactorAllParNS extends SRAMParFactorAllPar with NoStream

class SRAMReduceBase(p: scala.Int) extends SpatialTest {
  override def compileArgs = "--max_cycles=5000"

  override def main(args: Array[String]): Void = {
    val output = ArgOut[I32]

    Accel {
      val out = Reg[I32]

      Reduce(out)(0 until 32 par p) {
        i => i * 3
      } {_ + _}

      output := out
    }

    val gold = 31 * 32 / 2 * 3
    assert(checkGold(output, I32(gold)))
  }
}

class SRAMReduce extends SRAMReduceBase(16)
class SRAMReduceNS extends SRAMReduce with NoStream
