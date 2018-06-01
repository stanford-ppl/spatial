package spatial.tests.feature.banking

import spatial.dsl._

@test class MismatchedDiagonalBanking extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type T = Int

  def main(args: Array[String]): Unit = {
    val M = 64
    val N = 32
    val data = (0::M, 0::N){(i,j) => (i*N + j).to[T]}
    val x = DRAM[T](M,N)
    val y = DRAM[T](N,M)

    setMem(x, data)

    Accel {
      val in  = SRAM[T](M,N)
      val out = SRAM[T](N,M)
      in load x(0 :: M, 0 :: N par 16)
      Foreach(N by 1, M par 8){(i,j) => out(i,j) = in(j,i) }
      y store out
    }


    // Extract results from accelerator
    val result = getMatrix(y)
    val gold = data.t

    // Create validation checks and debug code
    assert(gold == result)
  }
}
