package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class UniqueParallelLoad extends SpatialTest {
  val dim0 = 144
  val dim1 = 96

  def awkwardload[T:Num](src1: Array[T], src2: Array[T]): T = {
    val mat = DRAM[T](dim0, dim1)
    val other = DRAM[T](dim1, dim1)
    val result = ArgOut[T]
    // Transfer data and start accelerator
    setMem(mat, src1)
    setMem(other, src2)

    Accel {
      val s1 = SRAM[T](dim0, dim1)
      val s2 = SRAM[T](dim1, dim1)
      // Parallel{
      s1 load mat(0::dim0, 0::dim1)
      s2 load other(0::dim1, 0::dim1)
      // }

      val accum = Reg[T](0.to[T])
      Reduce(accum)(dim0 by 1, dim1 by 1) { (i,j) =>
        s1(i,j)
      }{_+_}
      val accum2 = Reg[T](0.to[T])
      Reduce(accum2)(dim1 by 1, dim1 by 1) { (i,j) =>
        s2(i,j)
      }{_+_}
      result := accum.value + accum2.value
    }
    getArg(result)
  }


  def main(args: Array[String]): Unit = {
    type T = FixPt[TRUE,_32,_32]
    val srcA = Array.tabulate(dim0) { i => Array.tabulate(dim1){ j => -((j + i*dim1) % 256) }}
    val srcB = Array.tabulate(dim1) { i => Array.tabulate(dim1){ j => ((j + i*dim1) % 256) }}

    val dst = awkwardload(srcA.flatten, srcB.flatten)

    val goldA = srcA.map{ row => row.map{el => el}.reduce{_+_}}.reduce{_+_}
    val goldB = srcB.map{ row => row.map{el => el}.reduce{_+_}}.reduce{_+_}
    val gold = goldA + goldB

    println("Gold: " + gold)
    println("result: " + dst)
    val cksum = gold == dst
    println("PASS: " + cksum + " (UniqueParallelLoad)")
    assert(cksum)
  }
}

