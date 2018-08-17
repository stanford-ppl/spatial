package spatial.tests.compiler

import spatial.dsl._

@spatial class Broadcast extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val M = 16
    val N = 16
    val K = 16
    val MP = 1
    val NP = 8
    val KP = 4
    val alpha = 1
    val beta = 0

    val dram1 = DRAM[Int](M,K)
    val dram2 = DRAM[Int](K, N)
    val dram3 = DRAM[Int](M,N)
    val data1 = (0::M,0::K){(i,j) => random[Int](32) }
    val data2 = (0::K,0::N){(i,j) => random[Int](32) }

    setMem(dram1, data1)
    setMem(dram2, data2)

    Accel {
      val a = SRAM[Int](M,K)
      val b = SRAM[Int](K,N)
      val y = SRAM[Int](M,N)
      a load dram1
      b load dram2

      Foreach(M par MP, N par NP){(i,j) =>
        val prod = Reduce(Reg[Int])(K by 1 par KP){k => a(i,k) * b(k,j) }{_+_}
        val out = prod.value*alpha + beta
        y(i,j) = out
      }

      dram3 store y
    }

    val result_y = getMatrix(dram3)

    val golden_y = (0::M,0::N){(i,j) =>
      (0::K){k => data1(i,k) * data2(k,j) }.reduce{_+_}
    }

    printMatrix(result_y, "Result")
    printMatrix(golden_y, "Golden")
    println(r"Pass: ${golden_y == result_y}")
    assert(golden_y == result_y)
  }

}
