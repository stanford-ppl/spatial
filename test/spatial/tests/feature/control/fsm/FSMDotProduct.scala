package spatial.tests.feature.control.fsm

import spatial.dsl._

@spatial class FSMDotProduct extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val vectorA = Array.fill(128) { random[Int](10) }
    val vectorB = Array.fill(128) { random[Int](10) }

    val vecA = DRAM[Int](128)
    val vecB = DRAM[Int](128)
    val out = ArgOut[Int]

    setMem(vecA, vectorA)
    setMem(vecB, vectorB)

    Accel {
      FSM(0)(i => i < 128) { i =>
        val a = SRAM[Int](16)
        val b = SRAM[Int](16)
        Parallel {
          a load vecA(i :: i + 16)
          b load vecB(i :: i + 16)
        }
        out := out.value + Reduce(0)(0 until 16) { i => a(i) * b(i) } {_+_}
      } { i => i + 16 }
    }

    val result = getArg(out)
    val gold = vectorA.zip(vectorB) {_*_}.reduce {_+_}

    assert(result == gold, "Result (" + result + ") did not equal expected (" + gold + ")")
    println("PASS")
  }
}

