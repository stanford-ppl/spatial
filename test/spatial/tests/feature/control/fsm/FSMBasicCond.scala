package spatial.tests.feature.control.fsm


import spatial.dsl._


@test class FSMBasicCond extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32)
    Accel {
      val bram = SRAM[Int](32)

      FSM(0){state => state < 32} { state =>
        if (state < 16) {
          bram(31 - state) = state // 16:31 [15, 14, ... 0]
        }
        else {
          bram(state - 16) = state // 0:15 [16, 17, ... 31]
        }
      }{state => state + 1}

      dram store bram
    }
    val result = getMem(dram)
    val gold = Array.tabulate(32){i => if (i < 16) 16 + i else 31 - i }
    printArray(result, "Result")
    printArray(gold, "Gold")
    assert(result == gold)
    println("PASS")
  }
}


@test class FSMBasicCond2 extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32)
    Accel {
      val bram = SRAM[Int](32)
      val reg = Reg[Int](0)
      reg := 16
      FSM(0){state => state < 32} { state =>
        if (state < 16) {
          if (state < 8) {
            bram(31 - state) = state // 16:31 [7, 6, ... 0]
          } else {
            bram(31 - state) = state+1 // 16:31 [16, 15, ... 9]
          }
        }
        else {
          bram(state - 16) = if (state == 16) 17 else if (state == 17) reg.value else state // Test const, regread, and bound Mux1H
        }
      }{state => state + 1}

      dram(0::32 par 16) store bram
    }
    val result = getMem(dram)
    val gold = Array[Int](17, 16, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28,
      29, 30, 31, 16, 15, 14, 13, 12, 11, 10, 9, 7, 6, 5, 4, 3, 2, 1, 0)
    printArray(result, "Result")
    printArray(gold, "Gold")
    assert(result == gold)
  }
}



