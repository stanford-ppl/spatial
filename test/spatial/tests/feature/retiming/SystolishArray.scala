package spatial.tests.feature.retiming

import spatial.dsl._

@spatial class SystolishArray extends SpatialTest {

  def main(args: Array[String]): Void = {
    val a = ArgOut[Int]
    val b = ArgOut[Int]
    val c = ArgOut[Int]
    val d = ArgOut[Int]
    val e = ArgOut[Int]
    val f = ArgOut[Int]
    val g = ArgOut[Int]
    val h = ArgOut[Int]
    val j = ArgOut[Int]
    val k = ArgOut[Int]
    val l = ArgOut[Int]

    Accel {
      val A = Reg[Int](0)
      val B = Reg[Int](0)
      val C = Reg[Int](0)
      val D = Reg[Int](0)
      val E = Reg[Int](0)
      val F = Reg[Int](0)
      val G = Reg[Int](0)
      val H = Reg[Int](0)
      val J = Reg[Int](5)
      val K = Reg[Int](8)
      val L = Reg[Int](8)

      // Inspired by SHA1 with FullDelay of RegRead(E) being incorrect
      Foreach(5 by 1){i =>
        val temp = A + B + C + D + E + 1
        E := D; D := C; C := B; B := A; A := temp
      }
      Foreach(5 by 1){i => 
        F := i
        G := F.value
        H := F.value
      }
      Foreach(5 by 1){i => 
        if (J.value == 3) {
          J := 0 // This write should NOT happen before the read in the next branch condition, but it SHOULD happen before the K-writing branches
        } else if (J.value == 0) { 
          J := 9
        } else {
          J := i
        }

        if (J.value == 0) {
          K := 9 // Should run before this nested branch
          if (K.value == 9) { // TODO: This probably fails if this `if` is put in the else branch below
            L := 9
          }
        } else {
          K := i
          L := i
        }
      }

      a := A
      b := B
      c := C
      d := D
      e := E
      f := F
      g := G
      h := H
      j := J
      k := K
      l := L
    }

    val A_result = getArg(a)
    val B_result = getArg(b)
    val C_result = getArg(c)
    val D_result = getArg(d)
    val E_result = getArg(e)
    val F_result = getArg(f)
    val G_result = getArg(g)
    val H_result = getArg(h)
    val J_result = getArg(j)
    val K_result = getArg(k)
    val L_result = getArg(l)

    val A_gold = 16
    val B_gold = 8
    val C_gold = 4
    val D_gold = 2
    val E_gold = 1
    val F_gold = 4
    val G_gold = 4
    val H_gold = 4
    val J_gold = 0
    val K_gold = 9
    val L_gold = 9

    println(r"Got:    ${L_result} ${K_result} ${J_result} ${H_result} ${G_result} ${F_result} ${E_result} ${D_result} ${C_result} ${B_result} ${A_result}")
    println(r"Wanted: ${L_gold} ${K_gold} ${J_gold} ${H_gold} ${G_gold} ${F_gold} ${E_gold} ${D_gold} ${C_gold} ${B_gold} ${A_gold}")

    assert(A_result == A_gold && B_result == B_gold && C_result == C_gold && D_result == D_gold && E_result == E_gold && F_result == F_gold && G_result == G_gold && H_result == H_gold && J_result == J_gold && H_result == H_gold && L_result == L_gold)
  }
}
