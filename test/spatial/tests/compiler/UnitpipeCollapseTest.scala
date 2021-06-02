package spatial.tests.compiler

import spatial.dsl._

@spatial class UnitpipeCollapseTest extends SpatialTest {

  val N = 32

  def main(args: Array[String]): Unit = {
    val dram = DRAM[I32](N)
    Accel {
      val sram = SRAM[I32](N)
      Foreach(N by 1) {
        i =>
          val reg = Reg[I32]
          Pipe {
            reg := i * 3
          }
          Pipe {
            sram(i) = reg
          }
      }
      dram store sram
    }

    val gold = Array.tabulate(N){ i => i*3 }
    assert(checkGold(dram, gold))
  }
}
