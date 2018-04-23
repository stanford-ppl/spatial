package spatial.tests.apps

import spatial.dsl._

@test class SGD_minibatch extends SpatialTest {
  override def runtimeArgs: Args = "40 64 0.0001"

  type TM = FixPt[TRUE,_16,_16]
  type TX = FixPt[TRUE,_16,_16]
  val modelSize = 16
  val tileSize = 16
  val innerPar = 4
  val outerPar = 1 // Not used right now?
  val margin = 1


  def sgdminibatch(x_in: Array[TX], y_in: Array[TX], alpha: TM, epochs: Int, nn: Int): Array[TM] = {
    val E = ArgIn[Int]
    val N = ArgIn[Int]
    val A = ArgIn[TM]
    val D = modelSize

    val ip = innerPar (1 -> 1)
    val op = outerPar (1 -> 1)

    setArg(E, epochs)
    setArg(N, nn)
    setArg(A, alpha)

    val x = DRAM[TX](N,D)
    val y = DRAM[TX](N)
    val result = DRAM[TM](D)

    setMem(x, x_in)
    setMem(y, y_in)

    Accel {
      val y_tile = SRAM[TX](tileSize)
      val sgdmodel = SRAM[TM](D)
      val x_tile = SRAM[TX](tileSize,D)
      Foreach(D by 1){i => sgdmodel(i) = 0.to[TM]}
      Sequential.Foreach(E by 1) { e =>

        Sequential.Foreach (N by tileSize) { b =>
          y_tile load y(b::b+tileSize par ip)
          x_tile load x(b::b+tileSize, 0::D)
          val y_err = SRAM[TX](tileSize)
          Foreach(tileSize by 1) {i =>
            val y_hat = Reg[TX]
            Reduce(y_hat)(D by 1 par ip){ j => x_tile(i,j) * sgdmodel(j).to[TX] }{_+_}
            y_err(i) = y_tile(i) - y_hat.value
          }
          Foreach(D by 1) { i =>
            val raw_update = Reg[TX]
            Reduce(raw_update)(tileSize by 1 par ip){ j => x_tile(j,i) * y_err(j) }{_+_}
            sgdmodel(i) = sgdmodel(i) + raw_update.value.to[TM]*A
          }
        }
      }
      result(0::D par ip) store sgdmodel

    }

    getMem(result)
  }


  def main(args: Array[String]): Unit = {
    val E = args(0).to[Int]
    val N = args(1).to[Int]
    val A = args(2).to[TM] // Should be somewhere around 0.0001 for point-wise sgd
    val D = modelSize

    val sX = Array.fill(N){ Array.fill(D){ random[TX](3.to[TX]) + 1.to[TX]} }
    val ideal_model = Array.tabulate(D){ i => 2.to[TM] }
    val sY = Array.tabulate(N){i => ideal_model.zip(sX.apply(i)){case (a,b) => a.to[TX]*b}.reduce{_+_}}
    val id = Array.tabulate(D){ i => i }
    val ep = Array.tabulate(E){ i => i }

    val result = sgdminibatch(sX.flatten, sY, A, E, N)

    val cksum = ideal_model.zip(result){ case (a,b) => abs(a - b) < margin }.reduce{_&&_}
    printArray(result, "result: ")
    printArray(ideal_model, "gold: ")
    println("PASS: " + cksum  + " (SGD_minibatch)")
    assert(cksum)
  }
}