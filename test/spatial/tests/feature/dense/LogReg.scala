package spatial.tests.feature.dense


import spatial.dsl._


@test class LogReg extends SpatialTest {
  override def runtimeArgs: Args = "20 1024"

  type X = Float //FixPt[TRUE,_16,_16]

  val margin = 5
  val dim = 192
  val D = dim
  val A = 1

  val innerPar = 16
  val outerPar = 10

  val tileSize = 64


  def logreg[T:Num](xIn: Array[T], yIn: Array[T], tt: Array[T], n: Int, it: Int): Array[T] = {
    val iters = ArgIn[Int]
    val N     = ArgIn[Int]
    setArg(iters, it)
    setArg(N, n)

    val BN = tileSize (96 -> 96 -> 9600)
    val PX = 1 (1 -> 1)
    val P1 = innerPar (1 -> 2)
    val P2 = innerPar (1 -> 96)
    val P3 = outerPar (1 -> 96)

    val x = DRAM[T](N, D)
    val y = DRAM[T](N)
    val theta = DRAM[T](D)

    setMem(x, xIn)
    setMem(y, yIn)
    setMem(theta, tt)

    Accel {
      val btheta = SRAM[T](D)

      Sequential.Foreach(iters by 1) { epoch =>

        Sequential.MemReduce(btheta)(1 by 1){ xx =>
          val gradAcc = SRAM[T](D)
          Foreach(N by BN){ i =>
            val logregX = SRAM[T](BN, D)
            val logregY = SRAM[T](BN)
            Parallel {
              logregX load x(i::i+BN, 0::D par P2)
              logregY load y(i::i+BN par P2)
            }
            MemReduce(gradAcc)(BN par P3){ ii =>
              val pipe2Res = Reg[T]
              val subRam   = SRAM[T](D)

              val dotAccum = Reduce(Reg[T])(D par P2){j => logregX(ii,j) * btheta(j) }{_+_}  // read
              Pipe { pipe2Res := (logregY(ii) - sigmoid(dotAccum.value)) }
              Foreach(D par P2) {j => subRam(j) = logregX(ii,j) - pipe2Res.value }
              subRam
            }{_+_}
          }
          gradAcc
        }{(b,g) => b+g*A.to[T]}

        // Flush gradAcc
        //Foreach(D by 1 par P2){i => gradAcc(i) = 0.to[T] }
      }
      theta(0::D par P2) store btheta // read
    }
    getMem(theta)
  }


  def main(args: Array[String]): Unit = {
    val iters = args(0).to[Int]
    val N = args(1).to[Int]

    val sX = Array.fill(N){ Array.fill(D){ random[X](10.to[X])} }
    val sY = Array.tabulate(N){ i => i.to[X]} //fill(N)( random[T](10.0) )
    val theta = Array.fill(D) {random[X](1.to[X]) }

    val result = logreg(sX.flatten,sY, theta, N, iters)

    val gold = Array.empty[X](D)
    val ids = Array.tabulate(D){i => i}
    for (i <- 0 until D) {
      gold(i) = theta(i)
    }
    for (i <- 0 until iters) {
      val next = sX.zip(sY) {case (row, y) =>
        // println("sigmoid for " + y + " is " + sigmoid(row.zip(gold){_*_}.reduce{_+_}))
        val sub = y - sigmoid(row.zip(gold){(a,b) =>
          // println("doing " + a + " * " + b + " on row " + y)
          a*b}.reduce{_+_})
        row.map{a =>
          // println("subtraction for " + y + " is " + (a - sub))
          a - sub}
      }.reduce{(a,b) => a.zip(b){_+_}}
      for (i <- 0 until D) {
        gold(i) = gold(i) + next(i)
      }
      // printArr(gold, "gold now")
    }


    printArray(gold, "gold: ")
    printArray(result, "result: ")

    val cksum = result.zip(gold){ (a,b) => a > b-margin && a < b+margin}.reduce{_&&_}
    println("PASS: " + cksum  + " (LogReg)")
    assert(cksum)

  }

}






