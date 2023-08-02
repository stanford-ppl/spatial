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

@spatial class SingleQueryStreamedAttention extends SpatialTest {
  override def compileArgs = "--nostreamify"

  type T = Fix[TRUE, _24, _8] // Int
  val D = 4
  val N = 16

  override def main(args: Array[String]): Unit = {
    val qVals = Array.fill[T](N*D)(1) //Array.fill(N*D) { random[T](1) }
    val kVals = Array.fill[T](N*D)(1) //Array.fill(N*D) { random[T](1) }
    val vVals = Array.fill[T](N*D)(1) //Array.fill(N*D) { random[T](1) }
    val oVals = Array.fill[T](N*D)(0)

    val qDRAM = DRAM[T](N*D)
    val kDRAM = DRAM[T](N*D)
    val vDRAM = DRAM[T](N*D)
    val outDRAM = DRAM[T](D)

    setMem(qDRAM, qVals)
    setMem(kDRAM, kVals)
    setMem(vDRAM, vVals)
    setMem(outDRAM, oVals)

    Accel {
      // SRAMS
      val Q = SRAM[T](D)
      val K = SRAM[T](N*D)
      val V = SRAM[T](N*D)
      val tempO = SRAM[T](D)//.buffer
      val O = SRAM[T](D)

      // FIFO
      val sFIFO = FIFO[T](2)
      val mScaleSumFIFO = FIFO[T](2)
      val expSumFIFO = FIFO[T](2)
      val mScaleVFIFO = FIFO[T](2)
      val expVFIFO = FIFO[T](2)
      // val sumCounterFIFO = FIFO[Boolean](2)
      // val mulVCounterFIFO = FIFO[Boolean](2)
      val doneSumFIFO = FIFO[Boolean](2)
      val doneVFIFO = FIFO[Boolean](2)

      // Load data to SRAMs
      Q load qDRAM(0::D)
      K load kDRAM
      V load vDRAM
      tempO load outDRAM // initializing to 0

      Stream {
        // =============== Multiply Q*KT =============================
        Foreach(0 until N) { i =>
          val accum = Reg[T](0)
          Reduce(accum)(0 until D) { j =>
            Q(j) * K(i*D+j)
          } {_ + _}
          sFIFO.enq(accum.value)
        }


        // =============== Incremental Rowmax ========================
        val RowMaxReg = Reg[T](0)
        Foreach(0 until N){ i =>
          // ------------- Calculate Values -------------
          val si = sFIFO.deq() // i th element
          val m = RowMaxReg.value // Max until (i-1)th element
          val mNew = if (si > m) si else m // Max until (i)th element
          
          val expS = exp(si-mNew) // exponent of the i-th element
          val mScale = if (i === 0) 1 else exp(m-mNew) 
            // |_ Scaling factor for the partial sum in (⊗ V)
          

          // ------------- Enq values in the FIFO ------------- 
          mScaleSumFIFO.enq(mScale) // -> Sum Controller
          expSumFIFO.enq(expS)      // -> Sum Controller
          
          mScaleVFIFO.enq(mScale)   // -> (⊗ V) Controller
          expVFIFO.enq(expS)        // -> (⊗ V) Controller


          // ------------- Update the Row Max Reg -------------
          RowMaxReg := mNew
        }


        // =============== Row Sum ===================================
        val SumReg = Reg[T](0)
        // Foreach Ver
        Foreach(0 until N){ i =>
          SumReg := SumReg.value * mScaleSumFIFO.deq() + expSumFIFO.deq()
          doneSumFIFO.enq(true, i === (N-1))
        }
        // Reduce Ver
        /* Reduce(SumReg)(N by 1){ i =>
          SumReg := SumReg.value * mScaleSumFIFO.deq()
          doneSumFIFO.enq(true, i === (N-1))
          expSumFIFO.deq()
        }{_ + _}
        */
        

        // =============== Outer product with V ===============
        // Foreach ver
        Foreach(0 until N){ i =>
          val mScaleV = mScaleVFIFO.deq()
          val expV = expVFIFO.deq()
          Foreach(D by 1 par D){j =>
            tempO(j) = mScaleV * tempO(j) + expV * V(i*D + j)
              // scale the previously accumulated partial sums
              // + accumulate the new partial sum
          }
          // ------------- Metaprogramming Ver -------------
          /* (0 to D-1).foreach{ j =>
            tempO(j) = mScaleV * tempO(j) + expV * V(i*D + j)
          }*/

          doneVFIFO.enq(true, i === (N-1))
        }
        // MemReduce ver
        /* MemReduce(tempO)(N by 1){
          i =>
            val tmp = SRAM[T](D)
            val mScaleV = mScaleVFIFO.deq()
            val expV = expVFIFO.deq()
            (0 to D-1).foreach{ j =>
              tempO(j) = mScaleV * tempO(j) // scale the previously accumulated partial sums
              tmp(j) = expV * V(i*D + j) // accumulate the new partial sum
            }
            doneVFIFO.enq(true, i === (N-1)) // control logic to inform the start of softmax scaling
            tmp
        }{_+_}*/
        
        
        // =============== Softmax Scaling with Rowsum ===============
        Foreach(0 until 1){ i =>
          doneSumFIFO.deq()
          doneVFIFO.deq()
          Foreach(D by 1 par D){ j =>
            O(j) = tempO(j)/(SumReg.value)
          }
          // ------------- Metaprogramming Ver -------------
          /* (0 to D-1).foreach{ j =>
            O(j) = tempO(j)/(SumReg.value)
          }*/
        }
        
      }
      outDRAM store O
    }
    assert(Bit(true))
    printArray(getMem(outDRAM))
  }
}


@spatial class MultiQueryStreamedAttention extends SpatialTest {
  override def compileArgs = "--nostreamify"

  type T = Int//Fix[TRUE, _24, _8] // Int
  val D = 4
  val N = 4

  override def main(args: Array[String]): Unit = {
    val qVals = Array.fill(N*D) { random[T](5) } //Array.fill[T](N*D)(1)
    val kVals = Array.fill(N*D) { random[T](5) } //Array.fill[T](N*D)(1)
    val vVals = Array.fill(N*D) { random[T](5) } //Array.fill[T](N*D)(1)

    val qDRAM = DRAM[T](N*D)
    val kDRAM = DRAM[T](N*D)
    val vDRAM = DRAM[T](N*D)
    val oDRAM = DRAM[T](N*D)

    setMem(qDRAM, qVals)
    setMem(kDRAM, kVals)
    setMem(vDRAM, vVals)

    printArray(getMem(qDRAM))
    printArray(getMem(kDRAM))
    printArray(getMem(vDRAM))

    val zeroVals = Array.fill[T](N*D)(0)
    val zeroDRAM = DRAM[T](D)
    setMem(zeroDRAM, zeroVals)

    Accel {
      // SRAMS
      val Q = SRAM[T](D)
      val K = SRAM[T](N*D)
      val V = SRAM[T](N*D)
      val tempO = SRAM[T](D)
      val O = SRAM[T](D)

      // FIFO
      val sFIFO = FIFO[T](2)
      val mScaleSumFIFO = FIFO[T](2)
      val expSumFIFO = FIFO[T](2)
      val mScaleVFIFO = FIFO[T](2)
      val expVFIFO = FIFO[T](2)
      val rowSumFIFO = FIFO[T](2)
      val doneVFIFO = FIFO[Boolean](2)

      val loadNewQuery = FIFO[Boolean](2)
      val doneSoftmaxScaleFIFO = FIFO[Boolean](2)

      // Load data to SRAMs
      K load kDRAM
      V load vDRAM
      tempO load zeroDRAM // initializing to 0

      Stream {
        // =============== Multiply Q*KT =============================
        // Load query
        Foreach(0 until N) { i =>
          Q load qDRAM(i*D::i*D+D)
          loadNewQuery.enq(true)
        }

        Foreach(0 until N, 0 until N) { (i, j) =>
          val startNewQuery = j === 0
          loadNewQuery.deq(en = startNewQuery)

          val accum = Reg[T](0)
          Reduce(accum)(0 until D) { k =>
            Q(k) * K(j*D+k)
          } {_ + _}
          sFIFO.enq(accum.value)
        }

        
        // =============== Incremental Rowmax ========================
        Foreach(0 until N){ i =>
          val RowMaxReg = Reg[T](0) // TODO: Change to the min val for T
          Foreach(0 until N){ j =>
            // ------------- Calculate Values -------------
            val si = sFIFO.deq() // i th element
            val m = RowMaxReg.value // Max until (i-1)th element
            val mNew = if (si > m) si else m // Max until (i)th element
            
            val expS = exp(si-mNew) // exponent of the i-th element
            val mScale = if (j === 0) 0 else exp(m-mNew) 
              // |_ Scaling factor for the partial sum in (⊗ V)

            // ------------- Enq values in the FIFO ------------- 
            mScaleSumFIFO.enq(mScale) // -> Sum Controller
            expSumFIFO.enq(expS)      // -> Sum Controller
            
            mScaleVFIFO.enq(mScale)   // -> (⊗ V) Controller
            expVFIFO.enq(expS)        // -> (⊗ V) Controller

            // ------------- Update the Row Max Reg -------------
            RowMaxReg := mNew
          }
        }

        
        // =============== Row Sum ===================================
        // Foreach Ver
        Foreach(0 until N){ i =>
          val SumReg = Reg[T](0)
          Foreach(0 until N){ j =>
            SumReg := SumReg.value * mScaleSumFIFO.deq() + expSumFIFO.deq()
            rowSumFIFO.enq(SumReg.value, j === (N-1))
          }
        }
        
        
        // =============== Outer product with V ===============
        // Foreach ver
        Foreach(0 until N){ i =>
          Foreach(0 until N){ j =>
            val mScaleV = mScaleVFIFO.deq()
            val expV = expVFIFO.deq()
            Foreach(D by 1 par D){k =>
              tempO(k) = mScaleV * tempO(k) + expV * V(j*D + k)
                // scale the previously accumulated partial sums
                // + accumulate the new partial sum
            }
            doneVFIFO.enq(true, j === (N-1))
          }
        }
        
        // =============== Softmax Scaling with Rowsum ===============
        Foreach(0 until N){ i =>
          val rowSumVal = rowSumFIFO.deq()
          doneVFIFO.deq()
          Foreach(D by 1 par D){ j =>
            O(j) = tempO(j)/rowSumVal
          }
          doneSoftmaxScaleFIFO.enq(true)
        }

        Foreach(0 until N){ i =>
          doneSoftmaxScaleFIFO.deq()
          oDRAM(i*D::i*D+D) store O
        }
      }
    }
    assert(Bit(true))
    printArray(getMem(oDRAM))
  }
}

@spatial class FlashierAttention extends SpatialTest {
  override def compileArgs = "--nostreamify"

  type T = Int//Fix[TRUE, _24, _8] // Int
  val D = 8
  val N = 16
  val tile = 4

  override def main(args: Array[String]): Unit = {
    val qVals = Array.fill(N*D) { random[T](5) } //Array.fill[T](N*D)(1)
    val kVals = Array.fill(N*D) { random[T](5) } //Array.fill[T](N*D)(1)
    val vVals = Array.fill(N*D) { random[T](5) } //Array.fill[T](N*D)(1)

    val qDRAM = DRAM[T](N*D)
    val kDRAM = DRAM[T](N*D)
    val vDRAM = DRAM[T](N*D)
    val oDRAM = DRAM[T](N*D)

    setMem(qDRAM, qVals)
    setMem(kDRAM, kVals)
    setMem(vDRAM, vVals)

    printArray(getMem(qDRAM))
    printArray(getMem(kDRAM))
    printArray(getMem(vDRAM))

    val zeroVals = Array.fill[T](N*D)(0)
    val zeroDRAM = DRAM[T](D)
    setMem(zeroDRAM, zeroVals)

    Accel {
      // SRAMS
      val Q = SRAM[T](N*D)
      val K = SRAM[T](N*D)
      val V = SRAM[T](N*D)
      val tempO = SRAM[T](D)
      val O = SRAM[T](D)

      // FIFO
      val sFIFO = FIFO[T](2)
      val mScaleSumFIFO = FIFO[T](2)
      val expSumFIFO = FIFO[T](2)
      val mScaleVFIFO = FIFO[T](2)
      val expVFIFO = FIFO[T](2)
      val rowSumFIFO = FIFO[T](2)
      val doneVFIFO = FIFO[Boolean](2)

      val loadNewQuery = FIFO[Boolean](2)
      val doneSoftmaxScaleFIFO = FIFO[Boolean](2)

      // Load data to SRAMs
      K load kDRAM
      V load vDRAM
      tempO load zeroDRAM // initializing to 0

      Stream {
        // =============== Multiply Q*KT =============================
        // Load query
        Foreach(0 until N) { i =>
          Q load qDRAM(i*D::i*D+D)
          loadNewQuery.enq(true)
        }

        Foreach(0 until N, 0 until N) { (i, j) =>
          val startNewQuery = j === 0
          loadNewQuery.deq(en = startNewQuery)

          val accum = Reg[T](0)
          Reduce(accum)(0 until D) { k =>
            Q(k) * K(j*D+k)
          } {_ + _}
          sFIFO.enq(accum.value)
        }

        
        // =============== Incremental Rowmax ========================
        Foreach(0 until N){ i =>
          val RowMaxReg = Reg[T](0) // TODO: Change to the min val for T
          Foreach(0 until N){ j =>
            // ------------- Calculate Values -------------
            val si = sFIFO.deq() // i th element
            val m = RowMaxReg.value // Max until (i-1)th element
            val mNew = if (si > m) si else m // Max until (i)th element
            
            val expS = exp(si-mNew) // exponent of the i-th element
            val mScale = if (j === 0) 0 else exp(m-mNew) 
              // |_ Scaling factor for the partial sum in (⊗ V)

            // ------------- Enq values in the FIFO ------------- 
            mScaleSumFIFO.enq(mScale) // -> Sum Controller
            expSumFIFO.enq(expS)      // -> Sum Controller
            
            mScaleVFIFO.enq(mScale)   // -> (⊗ V) Controller
            expVFIFO.enq(expS)        // -> (⊗ V) Controller

            // ------------- Update the Row Max Reg -------------
            RowMaxReg := mNew
          }
        }

        
        // =============== Row Sum ===================================
        // Foreach Ver
        Foreach(0 until N){ i =>
          val SumReg = Reg[T](0)
          Foreach(0 until N){ j =>
            SumReg := SumReg.value * mScaleSumFIFO.deq() + expSumFIFO.deq()
            rowSumFIFO.enq(SumReg.value, j === (N-1))
          }
        }
        
        
        // =============== Outer product with V ===============
        // Foreach ver
        Foreach(0 until N){ i =>
          Foreach(0 until N){ j =>
            val mScaleV = mScaleVFIFO.deq()
            val expV = expVFIFO.deq()
            Foreach(D by 1 par D){k =>
              tempO(k) = mScaleV * tempO(k) + expV * V(j*D + k)
                // scale the previously accumulated partial sums
                // + accumulate the new partial sum
            }
            doneVFIFO.enq(true, j === (N-1))
          }
        }
        
        // =============== Softmax Scaling with Rowsum ===============
        Foreach(0 until N){ i =>
          val rowSumVal = rowSumFIFO.deq()
          doneVFIFO.deq()
          Foreach(D by 1 par D){ j =>
            O(j) = tempO(j)/rowSumVal
          }
          doneSoftmaxScaleFIFO.enq(true)
        }

        Foreach(0 until N){ i =>
          doneSoftmaxScaleFIFO.deq()
          oDRAM(i*D::i*D+D) store O
        }
      }
    }
    assert(Bit(true))
    printArray(getMem(oDRAM))
  }
}

