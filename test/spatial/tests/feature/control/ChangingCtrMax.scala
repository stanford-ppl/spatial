package spatial.tests.feature.control

import spatial.dsl._

@spatial class ChangingCtrMax extends SpatialTest {
  val tileSize = 16
  val N = 5

  def changingctrmax[T:Num](): Array[T] = {
    val result = DRAM[T](16)
    Accel {
      val rMem = SRAM[T](16)
      Sequential.Foreach(16 by 1) { i =>
        val accum = Reduce(0)(i by 1){ j => j }{_+_}
        rMem(i) = accum.value.to[T]
      }
      result(0::16 par 16) store rMem
    }
    getMem(result)
  }


  def main(args: Array[String]): Unit = {

    val result = changingctrmax[Int]()

    // Use strange if (i==0) b/c iter1: 0 by 1 and iter2: 1 by 1 both reduce to 0
    val gold = Array.tabulate(tileSize) { i => if (i==0) 0 else (i-1)*i/2}

    printArray(gold, "gold: ")
    printArray(result, "result: ")
    assert(result == gold)
  }
}