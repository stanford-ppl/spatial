package spatial.tests.feature.control

import spatial.dsl._

@spatial class SimpleLock extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val d = 14
    val N = 64
    val P = 1
    val result = DRAM[I32](d)
    val lockDRAM = LockDRAM[I32](d)
    setMem(lockDRAM, Array.tabulate[I32](N){i => -1})
    Accel{
      val lockSRAM = LockSRAM[I32](d).buffer // Buffer because w - w/r accesses in pipeline
      val lockSRAMUnit = Lock[I32](P)
      val lockDRAMUnit = Lock[I32](P)

      Sequential.Foreach(4 by 1 par 1) { i =>

        Foreach(d by 1) { j => lockSRAM(j) = 1 }
        Foreach(N by 1 par P) { j =>
          val addr = j % d
          val id = addr // % 5
          if (i % 2 == 0) {
            val lock = lockSRAMUnit.lock(id)
            val old: I32 = lockSRAM(addr, lock)
            val next: I32 = old + j
            lockSRAM(addr, lock) = next // What if you have the lock on only one or the other here?
          } else {
            val lock = lockDRAMUnit.lock(id)
            val old: I32 = lockDRAM(addr, lock)
            val next: I32 = old + j
            lockDRAM(addr, lock) = next // What if you have the lock on only one or the other here?
          }
        }

      }
      result(0::d) store lockSRAM

    }
    val gold = Array[I32](List.tabulate(N){j => j}.grouped(d).toList.map(_.sum).map(_.to[I32]):_*)

    assert(checkGold(result, gold))
  }
}
