package spatial.tests.feature.retiming

import spatial.dsl._

@spatial class SystolishArray extends SpatialTest {

  def main(args: Array[String]): Void = {
    val a = ArgOut[Int]
    val b = ArgOut[Int]
    val c = ArgOut[Int]
    val d = ArgOut[Int]
    val e = ArgOut[Int]

    Accel {
      val A = Reg[Int](0)
      val B = Reg[Int](0)
      val C = Reg[Int](0)
      val D = Reg[Int](0)
      val E = Reg[Int](0)

      // Inspired by SHA1 with FullDelay of RegRead(E) being incorrect
      Foreach(5 by 1){i =>
        val temp = A + B + C + D + E + 1
        E := D; D := C; C := B; B := A; A := temp
      }

      a := A
      b := B
      c := C
      d := D
      e := E
    }

    val A_result = getArg(a)
    val B_result = getArg(b)
    val C_result = getArg(c)
    val D_result = getArg(d)
    val E_result = getArg(e)

    val A_gold = 16
    val B_gold = 8
    val C_gold = 4
    val D_gold = 2
    val E_gold = 1

    println(r"Got:    ${E_result} ${D_result} ${C_result} ${B_result} ${A_result}")
    println(r"Wanted: ${E_gold} ${D_gold} ${C_gold} ${B_gold} ${A_gold}")

    assert(A_result == A_gold && B_result == B_gold && C_result == C_gold && D_result == D_gold && E_result == E_gold)
  }
}
