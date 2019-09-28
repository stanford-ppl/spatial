package spatial.tests.feature.control

import spatial.dsl._

@spatial class SimpleLock extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val d = 14
    val N = 64
    val P = 16
    val result = DRAM[I32](d)
    Accel{
      val lockSRAM = LockSRAM[I32](d)
      val lockSRAMUnit = Lock[I32](P)

      Foreach(4 by 1 par 1) { i =>

        Foreach(d by 1) { j => lockSRAM(j) = 0 }
        Foreach(N by 1 par P) { j =>
          val addr = j % d
          val id = addr // % 5

          val lock = lockSRAMUnit.lock(id)
          val old: I32 = lockSRAM(addr, lock)
          val next: I32 = old + j
          lockSRAM(addr, lock) = next // What if you have the lock on only one or the other here?

        }


      }

    }
    val got = getMem(result)
    val gold = Array[I32](List.tabulate(N){j => j}.grouped(d).toList.map(_.sum).map(_.to[I32]):_*)

    printArray(gold, "gold: ")
    printArray(got, "got: ")
    assert(result == gold)
  }
}
