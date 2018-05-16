package spatial.tests.feature.buffering


import spatial.dsl._


// TODO: Make this actually check a bubbled NBuf (i.e.- s0 = wr, s2 = wr, s4 =rd, s1s2 = n/a)
// because I think this will break the NBuf SM since it won't detect drain completion properly
@test class BufferedWrite2 extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  val tileSize = 64
  val I = 5
  val N = 192

  def bubbledwrtest(w: Array[Int], i: Array[Int]): Array[Int] = {
    val T = param(tileSize)
    val P = param(4)
    val weights = DRAM[Int](N)
    val inputs  = DRAM[Int](N)
    val weightsResult = DRAM[Int](N*I)
    val dummyWeightsResult = DRAM[Int](T)
    val dummyOut = DRAM[Int](T)
    val dummyOut2 = DRAM[Int](T)
    setMem(weights, w)
    setMem(inputs,i)
    Accel {

      val wt = SRAM[Int](T)
      val in = SRAM[Int](T)
      Sequential.Foreach(N by T){i =>
        wt load weights(i::i+T)
        in load inputs(i::i+T)

        Foreach(I by 1){x =>
          val niter = Reg[Int]
          niter := x+1
          MemFold(wt)(niter by 1){ k =>  // s0 write
            in
          }{_+_}
          val dummyReg1 = Reg[Int]
          val dummyReg2 = Reg[Int]
          val dummyReg3 = Reg[Int]
          Foreach(T by 1) { i => dummyReg1 := in(i)} // s1 do not touch
          Foreach(T by 1) { i => dummyReg2 := wt(i)} // s2 read
          Foreach(T by 1) { i => dummyReg3 := in(i)} // s3 do not touch
          weightsResult(i*I+x*T::i*I+x*T+T) store wt //s4 read
        }
      }

    }
    getMem(weightsResult)
  }


  def main(args: Array[String]): Unit = {
    val w = Array.tabulate(N){ i => i % 256}
    val i = Array.tabulate(N){ i => i % 256 }

    val result = bubbledwrtest(w, i)

    val gold = Array.tabulate(N/tileSize) { k =>
      Array.tabulate(I){ j =>
        Array.tabulate(tileSize) { i =>
          ( 1 + (j+1)*(j+2)/2 ) * (i + k*tileSize)
        }
      }.flatten
    }.flatten
    printArray(gold, "gold: ")
    printArray(result, "result: ")

    val cksum = gold.zip(result){_==_}.reduce{_&&_}
    println("PASS: " + cksum  + " (MultiplexedWriteTest)")
    assert(cksum)
  }
}
