package spatial.tests.feature.control.fsm

import spatial.dsl._

@spatial class FSMOuterSwitch extends SpatialTest {

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

      FSM(0)(i => i < 128){i =>
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

    assert(result == gold, "Result (" + result + ") did not equal expected (" + gold + ")")
    println("PASS")
  }
}