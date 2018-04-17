import spatial.dsl._

@spatial object MultiplexedWriteTest { // Regression (Unit) // Args: none

  val tileSize = 16
  val I = 5
  val N = 192

  def multiplexedwrtest(w: Array[I32], i: Array[I32]): Array[I32] = {
    val T = tileSize
    val P = 4
    val weights = DRAM[I32](N)
    val inputs  = DRAM[I32](N)
    val weightsResult = DRAM[I32](N*I)
    setMem(weights,w)
    setMem(inputs,i)
    Accel {
      val wt = SRAM[I32](T)
      val in = SRAM[I32](T)
      Sequential.Foreach(N by T){i =>
        wt load weights(i::i+T par 16)
        in load inputs(i::i+T par 16)

        // Some math nonsense (definitely not a correct implementation of anything)
        Foreach(I by 1){x =>
          val niter = Reg[I32]
          niter := x+1
          MemReduce(wt)(niter by 1){ i =>  // s0 write
            in
          }{_+_}
          weightsResult(i*I+x*T::i*I+x*T+T par 16) store wt //s1 read
        }
      }

    }
    getMem(weightsResult)
  }

  def main(args: Array[String]): Void = {
    val w = Array.tabulate(N){ i => i % 256}
    val i = Array.tabulate(N){ i => i % 256 }

    val result = multiplexedwrtest(w, i)

    val gold: Matrix[Array[I32]] = (0::N/tileSize, 0::I) { (k,j) =>
      val in = Array.tabulate(tileSize) { i => (j)*(k*tileSize + i) }
      val wt = Array.tabulate(tileSize) { i => k*tileSize + i }
      in.zip(wt){_+_}
    }
    printMatrix(gold, "gold: ")
    printArray(result, "result: ")

    val cksum = gold.flatten.flatten.zip(result){_==_}.reduce{_&&_}
    println("PASS: " + cksum  + " (MultiplexedWriteTest)")
  }
}