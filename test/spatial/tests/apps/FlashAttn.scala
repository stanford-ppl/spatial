
package spatial.tests.apps

import spatial.dsl._
@spatial class Flashattn extends SpatialTest {
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
      val V = SRAM[T](N)
      val output = FIFO[T](N)

      val sFIFO = FIFO[T](2)

      val mScaleSumFIFO = FIFO[T](2)
      val expSumFIFO = FIFO[T](2)
      val mScaleVFIFO = FIFO[T](2)
      val expVFIFO = FIFO[T](2)
      
      val outerPFIFO = FIFO[T](2)
      val rowSumFIFO = FIFO[T](2)


      Q load qDRAM
      K load kDRAM
      V load vDRAM

      //println(r"Q size: ${Q.numel}")

      Stream {
        // Compute exp(QK^T)
        val QReg = Reg[T] // Holds the value from Q for M cycles at a time.
        Foreach(0 until N, 0 until N) { (i, j) =>
          val QDeqEnable = j === 0
          QReg.write(Q.deq(en = QDeqEnable), QDeqEnable)
          val value = QReg.value * K(j)
          sFIFO.enq(value)
        }

        val RowMaxReg = Reg[T]
        RowMaxReg := 0
        Foreach(0 until N, 0 until N) { (i, j) =>
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

        val SumReg = Reg[T](0)
        // Foreach Ver
        Foreach(0 until N, 0 until N) { (i, j) =>
          val resetVal = expSumFIFO.deq()
          val newScale = mScaleSumFIFO.deq()
          val updateVal = SumReg.value * newScale + resetVal
          val updatedSum = if (j === 0) resetVal else updateVal
          rowSumFIFO.enq(updatedSum, j === (N-1))
          SumReg := updatedSum
        }

        val OutReg = Reg[T](0)
        Foreach(0 until N, 0 until N) { (i, j) =>
          val newScale = mScaleVFIFO.deq()
          val resetVal = expVFIFO.deq() * V(j)
          val updateVal = OutReg.value * newScale + resetVal
          val updatedSum = if (j === 0) resetVal else updateVal
          outerPFIFO.enq(updatedSum, j === (N-1))
          OutReg := updatedSum
        }

        Foreach(0 until N){ i =>
          val rowsumVal = rowSumFIFO.deq()
          val outerPVal = outerPFIFO.deq()
          output.enq(outerPVal/outerPVal)
        }

      }

      outDRAM store output
    }
    assert(Bit(true))
  }
}