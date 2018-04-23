package spatial.tests.feature.control.fsm


import spatial.dsl._


@test class FSMBasic extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32)
    Accel {
      val bram = SRAM[Int](32)

      FSM(0){state => state < 32}{state =>
        bram(state) = state
      }{state => state + 1}

      dram store bram
    }
    val result = getMem(dram)
    for(i <- 0 until 32) { assert(result(i) == i, "Incorrect at index " + i) }
    println("PASS")
  }
}


@test class FSMBasic2 extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32)
    val out = ArgOut[Int]
    Accel {
      val bram = SRAM[Int](32)

      FSM(0){state => state < 32}{state =>
        bram(state) = state
      }{state => state + 1}

      val y = Reg[Int](0)
      FSM(true)(x => x){x =>
        y :+= 1
      }{x => mux(y < 5, true, false)}

      out := y
      dram(0::32 par 16) store bram
    }
    val gold = Array.tabulate(32){i => i}

    val result = getMem(dram)
    val argresult = getArg(out)
    printArray(result, "Result")
    printArray(gold, "Gold")
    println("Arg is " + argresult + " =?= 5")
    assert(argresult == 5.to[Int])
    assert(gold == result)
  }
}


