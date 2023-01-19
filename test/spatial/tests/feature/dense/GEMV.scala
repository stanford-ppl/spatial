package spatial.tests.feature.dense

import spatial.dsl._

@spatial class GEMV extends SpatialTest {
    override def dseModelArgs: Args = "512 512"
    override def finalModelArgs: Args = "512 512"
    override def runtimeArgs: Args = "512 512"
    override def compileArgs: Args = super.compileArgs and "--noBindParallels"
   def main(args: Array[String]): Unit = {
    val M = ArgIn[Int] //8192
    setArg(M, args(0).to[Int])
    val N = ArgIn[Int] //8192
    setArg(N, args(1).to[Int])

    val op = loadParam("op", 1)
    val mp = loadParam("mp", 1)
    val ip = loadParam("ip", 1)
    val B = loadParam("B",64) //(1 -> 1 -> 1024)    // Parameter [1, 1024] with step of 64
    val T = loadParam("T", 64) //(1 -> 1 -> 1024)
    val Pblk = loadParam("Pblk",1) //(1 -> 1 -> 32)
    val Pelm = loadParam("Pelm",1) //(1 -> 1 -> 16)

    val hostMatrix: Matrix[Int] = (0::M,0::N){(i,j) => random[Int] }
    val hostVector: Array[Int] = Array.tabulate(N){i => random[Int] }

    val matrix = DRAM[Int](M,N)
    val vector = DRAM[Int](N)
    val output = DRAM[Int](M)

    setMem(matrix, hostMatrix)
    setMem(vector, hostVector)

    val M_reg = ArgIn[Int]
    val N_reg = ArgIn[Int]

    setArg(M_reg, M)
    setArg(N_reg, N)

    Accel {
      // over rows
      Foreach(M_reg by T par op) { i =>
        val outBlk = SRAM[Int](B)

        Foreach(T by 1 par mp) { ii =>
          val element = Reduce(0)(N_reg by B par Pblk) { j =>

            val rowBlk = SRAM[Int](B)
            val vecBlk = SRAM[Int](B)

            rowBlk load matrix(i+ii, j :: j + B par ip)
            vecBlk load vector(j :: j + B par ip)

            val res = Reg[Int]
            //Foreach(B by 1 par Pelm){jj => res :+= rowBlk(jj)*vecBlk(jj) }
            Reduce(0)(B by 1 par Pelm){jj => rowBlk(jj) * vecBlk(jj) }{(a,b) => a + b}
          }{(a,b) => a + b}

          outBlk(ii) = element
        }

        output(i::i+B par ip) store outBlk
      }
    }

    val gold = Array.tabulate(M){i =>
      Array.tabulate(N){j => hostMatrix(i,j)*hostVector(j) }.reduce(_+_)
    }

    val outHost: Array[Int] = getMem(output)

    assert(gold == outHost, "Gold and out did not match!")

    printArray(outHost)
    println(r"Pass: ${gold == outHost}")
  }
}


@spatial class StudentGEMV extends SpatialTest {

  type T = FixPt[TRUE,_10,_22]

  val N = 128  // A is NxN, and x is N wide.

  def main(args: Array[String]): Unit = {

    // These are on the HOST
    val x_host = loadCSV1D[T](s"$DATA/vector.csv")
    val A_host = loadCSV2D[T](s"$DATA/matrix.csv")
    val A_dram = DRAM[T](N, N)
    val x_dram = DRAM[T](N)
    setMem(A_dram, A_host)
    setMem(x_dram, x_host)
    val out_host = DRAM[T](N)
    Accel {
      val A_sram = SRAM[T](N, N)
      val x_sram = SRAM[T](N)

      A_sram load A_dram
      x_sram load x_dram

      val out_sram = SRAM[T](N)
      Foreach(0 until N) {
        i =>
          val accum = Reg[T]
          Reduce(accum)(0 until N) {
            j => A_sram(i, j) * x_sram(j)
          } {_ + _}
          out_sram(i) = accum.value
      }
      out_host store out_sram
    }

    writeCSV1D(getMem(out_host), s"$DATA/output.csv")
    assert(Bit(true))
  }
}
