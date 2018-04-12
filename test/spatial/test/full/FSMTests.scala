package spatial.test.full

import spatial.dsl._
import spatial.test.Testbench


@spatial object BasicFSM  {

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
    val gold = Array.tabulate(result.length){i => i}
    printArray(result, "result")
    printArray(gold, "gold")
    println(r"PASS: ${result == gold}")
  }
}

@spatial object BasicCondFSM   {

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32)
    Accel {
      val bram = SRAM[Int](32)

      FSM(0){state => state < 32}{state =>
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
    println(r"PASS: ${result == gold}")
  }
}

@spatial object DotProductFSM  {

  def main(args: Array[String]): Unit = {
    val vectorA = Array.fill(128){ random[Int](10) }
    val vectorB = Array.fill(128){ random[Int](10) }

    val vecA = DRAM[Int](128)
    val vecB = DRAM[Int](128)
    val out  = ArgOut[Int]

    setMem(vecA, vectorA)
    setMem(vecB, vectorB)

    Accel {
      val sum = Reg[Int](0)
      FSM(0){i => i < 128 }{i =>
        val a = SRAM[Int](16)
        val b = SRAM[Int](16)
        Parallel {
          a load vecA(i::i+16)
          b load vecB(i::i+16)
        }
        sum := sum + Reduce(0)(0 until 16){i => a(i) * b(i) }{_+_}
      }{i => i + 16 }
      out := sum
    }

    val result = getArg(out)
    val gold = vectorA.zip(vectorB){_*_}.reduce{_+_}

    println(r"result: $result, gold: $gold")
    println(r"PASS: ${result == gold}")
  }
}

@spatial object OuterSwitchFSMTest   {

  def main(args: Array[String]): Unit = {
    val vectorA = Array.fill(128){ random[Int](10) }
    val vectorB = Array.fill(128){ random[Int](10) + 1 }

    val vecA = DRAM[Int](128)
    val vecB = DRAM[Int](128)
    val out  = ArgOut[Int]

    setMem(vecA, vectorA)
    setMem(vecB, vectorB)

    Accel {
      val sum = Reg[Int](0)
      val product = Reg[Int](1)

      FSM(0){i => i < 128 }{i =>
        val sram = SRAM[Int](16)

        if (i < 64) {
          sram load vecA(i::i+16)
          sum := sum + Reduce(0)(0 until 16){i => sram(i) }{_+_}
        }
        else {
          sram load vecB(i::i+16)
          product := product * Reduce(0)(0 until 16){i => sram(i) }{_*_}
        }
      }{i => i + 16 }

      out := sum + product
    }

    val result = getArg(out)
    val gold = Array.tabulate(64){i => vectorA(i) }.reduce{_+_} +
      Array.tabulate(64){i => vectorB(i+64) }.reduce{_*_}

    println(r"result: $result, gold: $gold")
    println(r"PASS: ${result == gold}")
  }
}


class FSMTests extends Testbench {
  test(BasicFSM)
  test(BasicCondFSM)
  test(DotProductFSM)
  test(OuterSwitchFSMTest)
}
