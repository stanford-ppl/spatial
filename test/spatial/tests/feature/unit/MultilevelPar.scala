package spatial.tests.feature.unit

import spatial.dsl._

@spatial class MultilevelPar extends SpatialTest {
  val rowpar = 2
  val colpar = 3

  def main(args: Array[String]): Unit = {
    val d = DRAM[Int](16,16)
    Accel {
      val s = SRAM[Int](16,16)
      Foreach(16 by 1 par rowpar, 16 by 1 par colpar){(i,j) => s(i,j) = i+j}
      d store s
    }

    val result = getMatrix(d)
    val gold = (0::16, 0::16){(i,j) => i+j}
    printMatrix(result, "Result:")
    printMatrix(gold, "Wanted:")
    println(r"Pass? ${result == gold}")
    assert(result == gold)
  } 

}

