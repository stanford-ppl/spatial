package spatial.tests.feature.unit

import spatial.dsl._

@test class MultilevelPar extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  val rowpar = 2
  val colpar = 1


  def main(args: Array[String]): Unit = {
    val d = DRAM[Int](16,16)
    Accel {
      val s = SRAM[Int](16,16)
      Foreach(16 by 1 par 2, 16 by 1 par 4){(i,j) => s(i,j) = i+j}
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
