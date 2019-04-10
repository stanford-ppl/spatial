package spatial.tests.compiler

import spatial.dsl._

@spatial class Broadcast extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val M = 16
    val N = 16
    val K = 16
    val MP = 1
    val NP = 8
    val KP = 4
    val alpha = 1
    val beta = 0

    val dram1 = DRAM[Int](M,K)
    val dram2 = DRAM[Int](K, N)
    val dram3 = DRAM[Int](M,N)
    val data1 = (0::M,0::K){(i,j) => random[Int](32) }
    val data2 = (0::K,0::N){(i,j) => random[Int](32) }

    setMem(dram1, data1)
    setMem(dram2, data2)

    Accel {
      val a = SRAM[Int](M,K) // Should have 1 copy
      val b = SRAM[Int](K,N) // Should have 1 copy
      val y = SRAM[Int](M,N)
      a load dram1
      b load dram2

      Foreach(M par MP, N par NP){(i,j) =>
        val prod = Reduce(Reg[Int])(K by 1 par KP){k => a(i,k) * b(k,j) }{_+_}
        val out = prod.value*alpha + beta
        y(i,j) = out
      }

      dram3 store y
    }

    val result_y = getMatrix(dram3)

    val golden_y = (0::M,0::N){(i,j) =>
      (0::K){k => data1(i,k) * data2(k,j) }.reduce{_+_}
    }

    printMatrix(result_y, "Result")
    printMatrix(golden_y, "Golden")
    println(r"Pass: ${golden_y == result_y}")
    assert(golden_y == result_y)
  }

}

@spatial class BroadcastStressTest extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val A = ArgIn[Int]
    val B = ArgIn[Int]
    val C = ArgIn[Int]
    val S = ArgIn[Int]
    val dram = DRAM[Int](32)
    setMem(dram, Array.fill[Int](32)(0))
    setArg(A, 8)
    setArg(B, 32)
    setArg(C, 3)
    setArg(S, 1)
    val Y1 = ArgOut[Int]
    val Y2 = ArgOut[Int]
    val Y3 = ArgOut[Int]
    val Y4 = ArgOut[Int]
    val Y5 = ArgOut[Int]
    val Y6 = ArgOut[Int]
    val P3 = 3
    val P2 = 2

    // App should have 30 SRAMNew
    Accel {
      val accum = SRAM[Int](8,32,3,3)
      val sram1 = SRAM[Int](9999) // Should have 3 duplicates, each broadcast to 2 readers (NOT 3*2 duplicates)
      val reg1 = Reg[Int](0)
      val sram2 = SRAM[Int](9999) // Should have 3 duplicates, each broadcast to 2 readers (NOT 3*2 duplicates)
      val reg2 = Reg[Int](0)
      val sram3 = SRAM[Int](9999) // Should have 3 duplicates, each broadcast to 2 readers (NOT 3*2 duplicates)
      val reg3 = Reg[Int](0)
      val sram4 = SRAM[Int](9999) // Should have 3 duplicates, each broadcast to 2 readers (NOT 3*2 duplicates)
      val reg4 = Reg[Int](0)
      val sram5 = SRAM[Int](9999) // Should have 3 duplicates, each broadcast to 2 readers (NOT 3*2 duplicates)
      val reg5 = Reg[Int](0)
      val sram6 = SRAM[Int](9999) // Should have 6 duplicates
      val reg6 = Reg[Int](0)

      Foreach(9999 by 1){i => sram1(i) = i % 20; sram2(i) = i % 20;sram3(i) = i % 20; sram4(i) = i % 20; sram5(i) = i % 20; sram6(i) = i % 20}

      Foreach(A by 1, C by 1 par P3, B by 1 par P2, C by 1){case List(a,c1,b,c2) => // Should broadcast with 3 duplicates (did not prior to issue #200)
        val x = sram1(a * A + mux(S == 1, c2, c2*2)*C + mux(S == 1, c1, c1*2)) 
        accum(a,b,c1,c2) = x
      }

      Y1 := Reduce(Reg[Int](0))(A by 1, B by 1, C by 1, C by 1){case List(a,b,c1,c2) => accum(a,b,c1,c2)}{_+_}

      Foreach(A by 1, C by 1 par P3, C by 1, B by 1 par P2){case List(a,c1,c2,b) => // Should broadcast with 3 duplicates
        val x = sram2(a * A + mux(S == 1, c2, c2*2)*C + mux(S == 1, c1, c1*2)) 
        accum(a,b,c1,c2) = x
      }

      Y2 := Reduce(Reg[Int](0))(A by 1, B by 1, C by 1, C by 1){case List(a,b,c1,c2) => accum(a,b,c1,c2)}{_+_}

      Foreach(A by 1){a => 
        Foreach(C by 1 par P3, C by 1, B by 1 par P2){(c1,c2,b) =>                  // Should broadcast with 3 duplicates
          val x = sram3(a * A + mux(S == 1, c2, c2*2)*C + mux(S == 1, c1, c1*2)) 
          accum(a,b,c1,c2) = x
        }
      }

      Y3 := Reduce(Reg[Int](0))(A by 1, B by 1, C by 1, C by 1){case List(a,b,c1,c2) => accum(a,b,c1,c2)}{_+_}

      Foreach(C by 1 par P3){c1 => 
        Foreach(A by 1, C by 1, B by 1 par P2){(a,c2,b) =>                         // Should broadcast with 3 duplicates
          val x = sram4(a * A + mux(S == 1, c2, c2*2)*C + mux(S == 1, c1, c1*2)) 
          accum(a,b,c1,c2) = x
        }
      }

      Y4 := Reduce(Reg[Int](0))(A by 1, B by 1, C by 1, C by 1){case List(a,b,c1,c2) => accum(a,b,c1,c2)}{_+_}

      Foreach(B by 1 par P2){b => 
        val dummy = SRAM[Int](32)
        dummy load dram
        Foreach(A by 1, C by 1, C by 1 par P3){(a,c2,c1) =>                         // Should broadcast with 3 duplicates
          val x = sram5(a * A + mux(S == 1, c2, c2*2)*C + mux(S == 1, c1, c1*2)) 
          accum(a,b,c1,c2) = x
        }
      }

      Y5 := Reduce(Reg[Int](0))(A by 1, B by 1, C by 1, C by 1){case List(a,b,c1,c2) => accum(a,b,c1,c2)}{_+_}

      Pipe.POM.Foreach(B by 1 par P2){b => 
        val dummy = SRAM[Int](32)
        dummy load dram
        Foreach(A by 1, C by 1, C by 1 par P3){(a,c2,c1) =>                         // Should NOT broadcast because lockstepness broken in b loop
          val x = sram6(a * A + mux(S == 1, c2, c2*2)*C + mux(S == 1, c1, c1*2)) 
          accum(a,b,c1,c2) = x
        }
      }

      Y6 := Reduce(Reg[Int](0))(A by 1, B by 1, C by 1, C by 1){case List(a,b,c1,c2) => accum(a,b,c1,c2)}{_+_}
    }

    val y1 = getArg(Y1)
    val y2 = getArg(Y2)
    val y3 = getArg(Y3)
    val y4 = getArg(Y4)
    val y5 = getArg(Y5)
    val y6 = getArg(Y6)
    val gold = 20608
    println(r"Got1 $y1, wanted $gold")
    println(r"Got2 $y2, wanted $gold")
    println(r"Got3 $y3, wanted $gold")
    println(r"Got4 $y4, wanted $gold")
    println(r"Got5 $y5, wanted $gold")
    println(r"Got6 $y6, wanted $gold")
    assert(y1 == gold && y2 == gold && y3 == gold && y4 == gold && y5 == gold && y6 == gold)
  }

}
