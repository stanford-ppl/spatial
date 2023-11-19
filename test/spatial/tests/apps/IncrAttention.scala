package spatial.tests.apps

import spatial.dsl._
@spatial class Seq2 extends SpatialTest {
  override def compileArgs = "--nostreamify"

  type T = Fix[TRUE, _10, _22]

  // Useful for making IDEs happy about implicits
//  implicit def bits: Bits[T] = implicitly[Bits[T]]
//  implicit def num: Num[T] = implicitly[Num[T]]
  val N = 64
  override def main(args: Array[String]): Unit = {val qVals = Array.fill(N) { random[T](1) }
    val kVals = Array.fill(N) { random[T](1) }

    val qDRAM = DRAM[T](N)
    setMem(qDRAM, qVals)
    val kDRAM = DRAM[T](N)
    setMem(kDRAM, kVals)

    val outDRAM1 = DRAM[T](N*N)
    val outDRAM2 = DRAM[T](N)

    Accel {
      val Q = FIFO[T](N)
      val K = SRAM[T](N)
      val QK1 = FIFO[T](3)
      val QK2 = FIFO[T](N*N)
      val output = FIFO[T](N)

      Q load qDRAM
      K load kDRAM

      //println(r"Q size: ${Q.numel}")

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

        val accumReg = Reg[T]
        accumReg := 0
        Foreach(0 until N, 0 until N) { (i,j) =>
          val newVal = QK1.deq()
          val updateVal = accumReg.value + newVal
          val resetVal = newVal
          val newAccum = if (j === 0) resetVal else updateVal
          accumReg.write(newAccum)
          output.enq(1/accumReg.value, j === (N-1))
        }

      }

      outDRAM1 store QK2
      outDRAM2 store output
    }
    assert(Bit(true))
  }
}


@spatial class Seq3 extends SpatialTest {
  override def compileArgs = "--nostreamify"

  type T = Fix[TRUE, _10, _22]

  // Useful for making IDEs happy about implicits
//  implicit def bits: Bits[T] = implicitly[Bits[T]]
//  implicit def num: Num[T] = implicitly[Num[T]]
  val N = 256
  override def main(args: Array[String]): Unit = {val qVals = Array.fill(N) { random[T](1) }
    val kVals = Array.fill(N) { random[T](1) }

    val qDRAM = DRAM[T](N)
    setMem(qDRAM, qVals)
    val kDRAM = DRAM[T](N)
    setMem(kDRAM, kVals)

    val outDRAM1 = DRAM[T](N*N)

    Accel {
      val Q = FIFO[T](N)
      val K = SRAM[T](N)
      val QK1 = FIFO[T](3)
      val QK2 = FIFO[T](N+24)
      val QKRecipSum = FIFO[T](2)
      val QKOut = FIFO[T](N*N)

      Q load qDRAM
      K load kDRAM

      //println(r"Q size: ${Q.numel}")

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

        val accumReg = Reg[T]
        accumReg := 0
        Foreach(0 until N, 0 until N) { (i,j) =>
          val newVal = QK1.deq()
          val updateVal = accumReg.value + newVal
          val resetVal = newVal
          val newAccum = if (j === 0) resetVal else updateVal
          accumReg.write(newAccum)
          QKRecipSum.enq(1/accumReg.value, j === (N-1))
        }

        val SReg = Reg[T] // Holds the sum value for division
        Foreach(0 until N, 0 until N) { (i, j) =>
          val recipDeqEnable = j === 0
          SReg.write(QKRecipSum.deq(en = recipDeqEnable), recipDeqEnable)
          QKOut.enq(QK2.deq() * SReg.value)
        }

      }

      outDRAM1 store QKOut
    }
    assert(Bit(true))
  }
}