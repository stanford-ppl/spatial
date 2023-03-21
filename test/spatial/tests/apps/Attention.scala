package spatial.tests.apps

import spatial.dsl._
@spatial class LoopedAttention extends SpatialTest {
  override def compileArgs = "--nostreamify"

  type T = Fix[TRUE, _10, _22]
  // Useful for making IDEs happy about implicits
//  implicit def bits: Bits[T] = implicitly[Bits[T]]
//
//  implicit def num: Num[T] = implicitly[Num[T]]

  val N = 512
  override def main(args: Array[String]): Unit = {
    val qVals = Array.fill(N) { random[T](1) }
    val kVals = Array.fill(N) { random[T](1) }
    val vVals = Array.fill(N) { random[T](1) }

    val qDRAM = DRAM[T](N)
    setMem(qDRAM, qVals)
    val kDRAM = DRAM[T](N)
    setMem(kDRAM, kVals)
    val vDRAM = DRAM[T](N)
    setMem(vDRAM, vVals)

    val outDRAM = DRAM[T](N)

    Accel {
      val qSRAM = SRAM[T](N)
      val kSRAM = SRAM[T](N)
      val vSRAM = SRAM[T](N)

      qSRAM load qDRAM
      kSRAM load kDRAM
      vSRAM load vDRAM

      val QKT = SRAM[T](N, N)
      Foreach(0 until N, 0 until N) { (i, j) =>
        QKT(i, j) = exp(qSRAM(i) * kSRAM(j))
      }

      val QKSums = SRAM[T](N)
      Foreach(0 until N) { i =>
        QKSums(i) = Reduce(Reg[T](0))(0 until N) { j =>
          QKT(i, j)
        }{_ + _}
      }

      val output = SRAM[T](N)
      Foreach(0 until N) { i =>
        output(i) = Reduce(Reg[T](0))(0 until N) { j =>
          QKT(i, j) * vSRAM(j)
        }{_ + _} / QKSums(i)
      }

      outDRAM store output
    }
    assert(Bit(true))
    printArray(getMem(outDRAM))
  }
}

@spatial class StreamedAttention extends SpatialTest {
  override def compileArgs = "--nostreamify"

  type T = Fix[TRUE, _10, _22]

  // Useful for making IDEs happy about implicits
//  implicit def bits: Bits[T] = implicitly[Bits[T]]
//  implicit def num: Num[T] = implicitly[Num[T]]
  val N = 512
  override def main(args: Array[String]): Unit = {
    val qVals = Array.fill(N) { random[T](1) }
    val kVals = Array.fill(N) { random[T](1) }
    val vVals = Array.fill(N) { random[T](1) }

    val qDRAM = DRAM[T](N)
    setMem(qDRAM, qVals)
    val kDRAM = DRAM[T](N)
    setMem(kDRAM, kVals)
    val vDRAM = DRAM[T](N)
    setMem(vDRAM, vVals)

    val outDRAM = DRAM[T](N)

    Accel {
      val Q = FIFO[T](N)
      val K = SRAM[T](N)
      val QK1 = FIFO[T](2)
      val QK2 = FIFO[T](N+1)
      val QKRecipSum = FIFO[T](2)
      val QKOut = FIFO[T](2)
      val V = SRAM[T](N)
      val output = FIFO[T](N)

      Q load qDRAM
      K load kDRAM
      V load vDRAM

      println(r"Q size: ${Q.numel}")

      Stream {
        // Compute exp(QK^T)
        val QReg = Reg[T] // Holds the value from Q for M cycles at a time.
        Foreach(0 until N, 0 until N) { (i, j) =>
          val QDeqEnable = j === 0
          QReg.write(Q.deq(en = QDeqEnable), QDeqEnable)
          val value = QReg.value * K(j)
          val expValue = exp(value)
          QK1.enq(expValue)
          QK2.enq(expValue)
        }

        // Compute sum over QK1
        Foreach(0 until N) { i =>
          val accum = Reg[T]
          Reduce(accum)(0 until N) {
            j => QK1.deq()
          } {_ + _}
          QKRecipSum.enq(1 / accum.value)
        }

        // Perform division step in softmax
        val SReg = Reg[T] // Holds the sum value for division
        Foreach(0 until N, 0 until N) { (i, j) =>
          val recipDeqEnable = j === 0
          SReg.write(QKRecipSum.deq(en = recipDeqEnable), recipDeqEnable)
          QKOut.enq(QK2.deq() * SReg.value)
        }

        // Compute Matrix-vector product
        Foreach(0 until N) { i =>
          val accum = Reg[T]
          Reduce(accum)(0 until N) { j =>
            QKOut.deq() * V(j)
          } {_ + _}
          output.enq(accum.value)
        }
      }

      outDRAM store output
    }
    assert(Bit(true))
    printArray(getMem(outDRAM))
  }
}
