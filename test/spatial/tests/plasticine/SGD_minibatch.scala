package spatial.tests.plasticine

import spatial.dsl._

@spatial class SGD_minibatch extends PlasticineTest { self => // Regression (Dense) // Args: 40 64 0.0001

  val D = 16 // param [64]
  val N = 1024 // param [pmuSize / <D> * 16]
  val E = 1 // param [2]
  val ts = 16 // param [pmuSize / <D>]
  val mp1 = 1 // param [4,8] | <ts> % p == 0
  val mp2 = 1 // param [8,16] | <ts> % p == 0

  val A = 0.0001f
  val ip = 16

  type TM = FixPt[TRUE,_16,_16]
  type TX = FixPt[TRUE,_16,_16]
  val margin = 1

  def sgdminibatch(x_in: Array[TX], y_in: Array[TX], alpha: TM, epochs: Int, nn: Int) = {
    //val E = ArgIn[Int]
    //val N = ArgIn[Int]
    //val A = ArgIn[TM]

    //setArg(E, epochs); bound(E) = self.E
    //setArg(N, nn); bound(N) = self.N
    //setArg(A, alpha)

    val x = DRAM[TX](N,D)
    val y = DRAM[TX](N)
    val result = DRAM[TM](D)

    setMem(x, x_in)
    setMem(y, y_in)

    Accel {
      val y_tile = SRAM[TX](ts)
      val sgdmodel = SRAM[TM](D)
      val x_tile = SRAM[TX](ts,D)
      Foreach(D by 1 par ip) { i => sgdmodel(i) = 0.to[TM] }
      Sequential.Foreach(E by 1, N by ts) { (e,b) =>
        y_tile load y(b::b+ts par ip)
        x_tile load x(b::b+ts, 0::D par ip)
        val y_err = SRAM[TX](ts)
        Foreach(ts by 1 par mp1) { i => 
          val y_hat = Reduce(Reg[TX])(D by 1 par ip){ j => x_tile(i,j) * sgdmodel(j).to[TX] }{_+_}
          y_err(i) = y_tile(i) - y_hat.value
        }
        MemFold(sgdmodel)(ts by 1 par mp2) { i =>
          val row = SRAM[TX](D)
          Foreach(D by 1 par ip) { j => row(j) = x_tile(i,j) * y_err(i) * A }
          row
        } { _ + _ }
      }
      result(0::D par ip) store sgdmodel
    }

    getMem(result)

  }

  def main(args: Array[String]): Unit = {
    val sX = Array.fill(N){ Array.fill(D){ random[TX](3.to[TX]) + 1.to[TX]} }
    val ideal_model = Array.tabulate(D){ i => 2.to[TM] }
    val sY = Array.tabulate(N){i => ideal_model.zip(sX.apply(i)){case (a,b) => a.to[TX]*b}.reduce{_+_}}
    val id = Array.tabulate(D){ i => i }
    val ep = Array.tabulate(E){ i => i }

    val result = sgdminibatch(sX.flatten, sY, A.to[TM], E, N)

    val cksum = ideal_model.zip(result){ case (a,b) => abs(a - b) < margin }.reduce{_&&_}
    printArray(result, "result: ")
    printArray(ideal_model, "gold: ")
    println("PASS: " + cksum  + " (SGD_minibatch)")
    assert(cksum)
  }
}

