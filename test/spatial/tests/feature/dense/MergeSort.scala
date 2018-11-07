package spatial.tests.feature.unit

import spatial.dsl._
import spatial.lib.Sort

@spatial class MergeSort extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    type T = I32

    val n = 4096
    val mem = List.fill(2) { DRAM[T](n) }
    val data = Array.tabulate(n) { i => random[T](n) }
    setMem(mem(0), data)

    Accel {
      Sort.mergeSort(mem(0), mem(1), 16, 4, n)
    }

    val result = getMem(mem(1))
    val result2 = getMem(mem(0))
    val cksum1 = Array.tabulate(n-1){i => result(i+1) >= result(i)}.reduce{_&&_}
    val cksum2 = Array.tabulate(n-1){i => result2(i+1) >= result2(i)}.reduce{_&&_}

    printArray(result, r"Result: (Sorted? $cksum1)")
    printArray(result2, r"Result2: (Sorted? $cksum2)")
    println("One buffer must be sorted to pass")
    assert(cksum1 || cksum2)
  }
}

