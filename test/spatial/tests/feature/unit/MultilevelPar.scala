package spatial.tests.feature.unit

import spatial.dsl._

@spatial class MultilevelPar extends SpatialTest {
  val rowpar = 2
  val colpar = 3

  def main(args: Array[String]): Unit = {
    val d = DRAM[Int](16,16)
    val d2 = DRAM[Int](16,16)
    Accel {
      val s = SRAM[Int](16,16)
      Foreach(16 by 1 par rowpar, 16 by 1 par colpar){(i,j) => s(i,j) = i+j}
      d store s

      val s2 = SRAM[Int](16,16)
      Foreach(16 by 1 par rowpar, 16 by 1 par colpar){(i,j) => 
        Foreach(8 by 1){k => s2(i,j) = i+k+j}
      }
      d2 store s2
    }

    val result = getMatrix(d)
    val gold = (0::16, 0::16){(i,j) => i+j}
    printMatrix(result, "Result:")
    printMatrix(gold, "Wanted:")
    val result2 = getMatrix(d2)
    val gold2 = (0::16, 0::16){(i,j) => i+j+7}
    printMatrix(result2, "Result2:")
    printMatrix(gold2, "Wanted2:")
    println(r"Pass? ${result == gold && result2 == gold2}")
    assert(result == gold && result2 == gold2)
  } 

}

