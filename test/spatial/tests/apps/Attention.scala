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
  val N = 16
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

@spatial class StreamAttentionBasic extends SpatialTest {
  override def compileArgs = "--nostreamify"

  type T = Fix[TRUE, _10, _22]
  val D = 4
  val N = 16

  override def main(args: Array[String]): Unit = {
    val qVals = Array.fill[T](N*D)(1) //Array.fill(N*D) { random[T](1) }
    val kVals = Array.fill[T](N*D)(1) //Array.fill(N*D) { random[T](1) }
    //val vVals = Array.fill(N*D) { random[T](1) }

    val qDRAM = DRAM[T](N*D)
    val kDRAM = DRAM[T](N*D)
    //val vDRAM = DRAM[T](N*D)
    val outDRAM = DRAM[T](N) // TODO: N->D

    setMem(qDRAM, qVals)
    setMem(kDRAM, kVals)
    //setMem(vDRAM, vVals)

    Accel {
      // SRAMS
      val Q = SRAM[T](D)
      val K = SRAM[T](N*D)
      //val V = SRAM[T](N*D)

      // FIFO
      val sFIFO = FIFO[T](2)
      val mOldSumFIFO = FIFO[T](2)
      val mNewSumFIFO = FIFO[T](2)
      val sSumFIFO = FIFO[T](2)
      val mOldVFIFO = FIFO[T](2)
      val mNewVFIFO = FIFO[T](2)
      val sVFIFO = FIFO[T](2)

      val output = FIFO[T](N) // Temporary

      // Load data to SRAMs
      Q load qDRAM(0::D)
      K load kDRAM
      //V load vDRAM

      Stream {
        // Multiply Q*KT
        Foreach(0 until N) { i =>
          val accum = Reg[T](0)
          Reduce(accum)(0 until D) { j =>
            Q(j) * K(i*D+j)
          } {_ + _}
          sFIFO.enq(accum.value)
        }

        val RowMaxReg = Reg[T](0)
        Foreach(0 until N){ i =>
          // Get the max until the i-th element
          val si = sFIFO.deq() // i th element
          val m = RowMaxReg.value // Max value until (i-1)th element
          val mNew = if (si > m) si else m // Max value until (i)th element

          // FIFOs to pass down values to Sum Controller
          mOldSumFIFO.enq(m)
          mNewSumFIFO.enq(mNew)
          sSumFIFO.enq(si)

          // FIFOs to pass down values to Sum Controller
          mOldVFIFO.enq(m)
          mNewVFIFO.enq(mNew)
          sVFIFO.enq(si)

          RowMaxReg := mNew // Update the Reg
        }

        // Sumup the row
        Foreach(0 until N){i =>
          // FIFOs to pass down values to Sum Controller
          mOldSumFIFO.deq()
          mNewSumFIFO.deq()
          sSumFIFO.deq()
        }

        // Outer product with V
        Foreach(0 until N){i =>
          // FIFOs to pass down values to Sum Controller
          mOldVFIFO.deq()
          mNewVFIFO.deq()
          output.enq(sVFIFO.deq())
        }
      }
      outDRAM store output
    }
    assert(Bit(true))
    printArray(getMem(outDRAM))
  }
}
/*

@spatial class MemfreeStreamedAttention extends SpatialTest {
  override def compileArgs = "--nostreamify"

  type T = Fix[TRUE, _10, _22]

  // Useful for making IDEs happy about implicits
//  implicit def bits: Bits[T] = implicitly[Bits[T]]
//  implicit def num: Num[T] = implicitly[Num[T]]
  val N = 8//512
  val D = 4
  override def main(args: Array[String]): Unit = {
    val qVals = Array.fill(D) { random[T](1) }
    val kVals = Array.fill(N*D) { random[T](1) }
    val vVals = Array.fill(N*D) { random[T](1) }
    val oVals = Array.fill(D)(0.to[T])

    val qDRAM = DRAM[T](D)
    setMem(qDRAM, qVals)
    val kDRAM = DRAM[T](N*D)
    setMem(kDRAM, kVals)
    val vDRAM = DRAM[T](N*D)
    setMem(vDRAM, vVals)

    val outDRAM = DRAM[T](D)
    setMem(outDRAM, oVals)
    printArray(getMem(outDRAM))
    

    Accel {
      // inputs
      val Q = SRAM[T](D) // let's assume for now we only have 1 row of Q
      val K = SRAM[T](N*D)
      val V = SRAM[T](N*D)
      val O = SRAM[T](D) // Since we'll only generate one row for now

      val RowMax = Reg[T](1)
      val QKRecipSum = FIFO[T](2)
      val P1 = FIFO[T](2)
      val P2 = FIFO[T](2)

      Q load qDRAM
      K load kDRAM
      V load vDRAM
      O load outDRAM

      Stream {
        // Compute exp(QK^T-max)
        Foreach(0 until N) { i =>
          val accum = Reg[T]
          Reduce(accum)(0 until D) { j =>
            Q(j) * K(D*i + j)
          } {_ + _}
          val expValue = exp(accum.value-RowMax.value)
          P1.enq(expValue)
          P2.enq(expValue)
        }

        val RowSum = Reg[T]
        // When we extend this to the whole matrix, we need to add outer Foreach block (0 until N)
        // to iterate through the N rows of P
        Reduce(RowSum)(0 until N) {
          i => P1.deq()
        } {_ + _}
        QKRecipSum.enq(1 / RowSum.value)
      
        
        val RecipSumReg = Reg[T] // Holds the value from QKRecipSum for N*D cycles at a time.
        val PReg = Reg[T] // Holds the value from P2 for D cycles at a time
        // Need to chage into Foreach(0 until N, 0 until N, 0 until D) { (i,j,k)}
        // If we want to get the whole O
        Foreach(0 until N, 0 until D) { (i, j) =>
          val RecipDeqEnable = (i+j) === 0
          RecipSumReg.write(QKRecipSum.deq(en = RecipDeqEnable), RecipDeqEnable)

          val PDeqEnable = j === 0
          PReg.write(P2.deq(en = PDeqEnable), PDeqEnable)
          
          val value = PReg.value * V(D*i+j) / RecipSumReg.value

          O(j) = O(j) + value
        }
      }

      outDRAM store O
    }
    assert(Bit(true))
    printArray(getMem(outDRAM))
  }
}

@spatial class StreamedAttention1 extends SpatialTest {
  override def compileArgs = "--nostreamify"

  type T = Fix[TRUE, _10, _22]

  // Useful for making IDEs happy about implicits
//  implicit def bits: Bits[T] = implicitly[Bits[T]]
//  implicit def num: Num[T] = implicitly[Num[T]]
  val N = 8//512
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
      val K = SRAM[T](N*D)
      val QK1 = FIFO[T](2)
      val QK2 = FIFO[T](N+1)
      val QKRecipSum = FIFO[T](2)
      val QKOut = FIFO[T](2)
      val V = SRAM[T](N*D)
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

@spatial class StreamedAttention2 extends SpatialTest {
  override def compileArgs = "--nostreamify"

  type T = Fix[TRUE, _10, _22]
  type Vec4 = Vector4[T]

  // Useful for making IDEs happy about implicits
//  implicit def bits: Bits[T] = implicitly[Bits[T]]
//  implicit def num: Num[T] = implicitly[Num[T]]
  val N = 8//512
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
      val Q = FIFO[Vec4](N)
      val K = SRAM[Vec4](N)
      
      val QK1 = FIFO[T](2)
      val QK2 = FIFO[T](N+1)
      val QKRecipSum = FIFO[T](2)
      val QKOut = FIFO[T](2)
      val V = SRAM[T](N*D)
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


@spatial class VectorFIFO extends SpatialTest {
  override def compileArgs = "--nostreamify"

  type T = Fix[TRUE, _10, _22]
  val D = 4
  val N = 16

  override def main(args: Array[String]): Unit = {
    val qVals = Array.fill(D*N) { random[T](1) }
    val kVals = Array.fill(N*D) { random[T](1) }
    val qDRAM = DRAM[T](D*N)
    val kDRAM = DRAM[T](N*D)
    setMem(qDRAM, qVals)
    setMem(kDRAM, kVals)

    val outDRAM = DRAM[T](N)

    Accel {
      val Q = FIFO[T](D*N)
      val K = SRAM[T](N*D)
      // Load data
      Q load qDRAM
      K load kDRAM

      val S = FIFO[T](N)

      val QReg = {
        implicit def ev: Bits[Vec[T]] = Vec.bits(D)
        Reg[Vec[T]]
      }
      Stream {
        Foreach(0 until N) { (i) =>
          QReg := Q.deqVec(D)
          val accum = Reg[T](1)
          Reduce(accum)(0 until D) { j =>
            val qregv = QReg.value
            qregv(j) * K(i*N+j)
          } {_ + _}
          S.enq(accum.value)
        }
      }
      outDRAM store S
    }
    assert(Bit(true))
    printArray(getMem(outDRAM))
  }
}
*/