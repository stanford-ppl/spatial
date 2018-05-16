package spatial.tests.feature.banking

import spatial.dsl._

@test class Bank2DSimple extends SpatialTest {
  def runtimeArgs: Args = NoArgs

  val R = 32; val C = 16
  val P = 1;  val Q = 4


  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](R,C)

    Accel {
      val x = SRAM[Int](R,C)

      Foreach(0 until R, 0 until C par Q){(i,j) =>
        x(i,j) = i + j
      }
      dram store x
    }

    val gold = (0::R,0::C){(i,j) => i + j}
    val data = getMatrix(dram)
    printMatrix(data, "data")
    printMatrix(gold, "gold")
    assert(data == gold)
  }
}
