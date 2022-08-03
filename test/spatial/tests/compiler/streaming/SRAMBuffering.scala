package spatial.tests.compiler.streaming
import spatial.dsl._

@spatial class SRAMBufferingSimple extends SpatialTest {
  override def compileArgs = "--max_cycles=3000"

  val outerIters = 4
  val innerIters = 2

  override def main(args: Array[String]) = {
//    implicitly[argon.State].config.stop = 50
    val output = DRAM[I32](outerIters, innerIters)
//    val output2 = DRAM[I32](outerIters, innerIters)
    Accel {
      val outputSR = SRAM[I32](outerIters, innerIters)
//      val outputSR2 = SRAM[I32](outerIters, innerIters)
      Foreach(0 until outerIters by 1 par 1) {
        outer =>
          val sr = SRAM[I32](innerIters)
          'Producer.Foreach(0 until innerIters by 1) {
            inner =>
              sr(inner) = inner + outer
          }

//          Parallel {
            'Consumer.Foreach(0 until innerIters by 1) {
              inner =>
                outputSR(outer, inner) = sr(inner)
            }

//            'Consumer2.Foreach(0 until innerIters by 1) {
//              inner =>
//                outputSR2(outer, inner) = sr(inner) + 1
//            }
//          }
      }
      output store outputSR(0::outerIters, 0::innerIters)
//      output2 store outputSR2(0::outerIters, 0::innerIters)
    }
    printMatrix(getMatrix(output))
    val reference = Matrix.tabulate(outerIters, innerIters) {
      (outer, inner) => outer + inner
    }
    assert(checkGold(output, reference))
    assert(Bit(true))
  }
}

class SRAMBufferingSimpleNoStream extends SRAMBufferingSimple {
  override def compileArgs = super.compileArgs + "--nostreamify"
}

class SRAMTransfer extends SpatialTest {
  val transferSize = 32
  override def compileArgs = "--max_cycles=1000"
  override def main(args: Array[String]): Void = {
    val input = DRAM[I32](transferSize)
    val output = DRAM[I32](transferSize)
    val gold = Array.tabulate(transferSize) {i => i}
    setMem(input, gold)
    Accel {
      val sr = SRAM[I32](transferSize)
      sr load input
      output store sr
    }
    assert(checkGold(output, gold))
  }
}

class SRAMTransfer2 extends SRAMTransfer {
  override def compileArgs = "--max_cycles=500"
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

//class SRAMTransferWithIntermediate extends SpatialTest {
//  val transferSize = 32
//  override def compileArgs = "--max_cycles=5000"
//  override def main(args: Array[String]): Void = {
//    val input = DRAM[I32](transferSize)
//    val output = DRAM[I32](transferSize)
//    val gold = Array.tabulate(transferSize) {i => i}
//    setMem(input, gold)
//    println(r"Set Mem Finished")
//    Accel {
//      val sr = SRAM[I32](transferSize)
//      val sr2 = SRAM[I32](transferSize)
//      sr load input
//      Foreach(0 until transferSize) {
//        i => sr2(i) = sr(i)
//      }
//      output store sr2
//    }
//    assert(checkGold(output, gold))
//  }
//}
//
//class TransferWithFIFOs extends SpatialTest {
//  val transferSize = 32
//  override def compileArgs = "--max_cycles=5000"
//  override def main(args: Array[String]): Void = {
//    val input = DRAM[I32](transferSize)
//    val output = DRAM[I32](transferSize)
//    val gold = Array.tabulate(transferSize) {i => i}
//    setMem(input, gold)
//    println(r"Set Mem Finished")
//    Accel {
//      val fifo = FIFO[I32](transferSize)
//      fifo load input
//      output store fifo
//    }
//    assert(checkGold(output, gold))
//  }
//}
