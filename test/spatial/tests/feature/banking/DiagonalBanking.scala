package spatial.tests.feature.banking


import spatial.dsl._


@test class DiagonalBanking extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type T = Int


  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val colpar = 8
    val rowpar = 3
    val M = 8//64
    val N = 3//32
    val x_data = (0::M, 0::N){(i,j) => (i*N + j).to[T]}
    val x = DRAM[T](M,N)
    val s = ArgOut[T]

    setMem(x, x_data)

    Accel {
      val xx = SRAM[T](M,N)
      xx load x(0 :: M, 0 :: N par colpar)
      s := Reduce(Reg[T](0.to[T]))(N by 1, M by 1 par rowpar) { (j,i) =>
        xx(i,j)
      }{_+_}
    }

    // Extract results from accelerator
    val result = getArg(s)

    // Create validation checks and debug code
    val gold = x_data.reduce{_+_}
    println("Result: (gold) " + gold + " =?= " + result)

    assert(gold == result)
  }
}
