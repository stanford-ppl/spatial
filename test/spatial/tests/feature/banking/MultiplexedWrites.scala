package spatial.tests.feature.banking

import spatial.dsl._

@spatial class MultiplexedWrites extends SpatialTest {
  val tileSize = 16
  val I = 5
  val N = 192

  def multiplexedwrtest[W:Num](w: Array[W], i: Array[W]): Array[W] = {
    val T = param(tileSize)
    val P = param(4)
    val weights = DRAM[W](N)
    val inputs  = DRAM[W](N)
    val weightsResult = DRAM[W](N*I)
    setMem(weights, w)
    setMem(inputs,i)
    Accel {
      val wt = SRAM[W](T)
      val in = SRAM[W](T)
      Sequential.Foreach(N by T){i =>
        wt load weights(i::i+T par 16)
        in load inputs(i::i+T par 16)

        // Some math nonsense (definitely not a correct implementation of anything)
        Foreach(I by 1){x =>
          val niter = Reg[Int]
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


  def main(args: Array[String]): Unit = {
    val w = Array.tabulate(N){ i => i % 256}
    val i = Array.tabulate(N){ i => i % 256 }

    val result = multiplexedwrtest(w, i)

    val gold = Array.tabulate(N/tileSize) { k =>
      Array.tabulate(I){ j =>
        val in = Array.tabulate(tileSize) { i => (j)*(k*tileSize + i) }
        val wt = Array.tabulate(tileSize) { i => k*tileSize + i }
        in.zip(wt){_+_}
      }.flatten
    }.flatten
    printArray(gold, "gold: ");
    printArray(result, "result: ");
    assert(gold == result)
  }
}
