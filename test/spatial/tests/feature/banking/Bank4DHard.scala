package spatial.tests.feature.banking

import spatial.dsl._
import argon.Block

@spatial class Bank4DHard extends SpatialTest {
  override def compileArgs: Args = super.compileArgs and "--forceBanking"

  // Not really "hard", but used to keep ExhaustiveBanking churning for a crazy long time
  val I = 16; val R = 3; val C = 3; val O = 16
  val PI = 2; val PR = 3;val PC= 3; val PO = 2

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](I,R,C,O)
    val data = (0::I,0::R,0::C,0::O){(i,j,k,l) => (i+j+k+l) % 5}
    setMem(dram,data)
    val out = ArgOut[Int]
    Accel {
      val x = SRAM[Int](I,R,C,O)
      x load dram

      out := Reduce(Reg[Int])(0 until I par PI, 0 until R par PR, 0 until C par PC, 0 until O par PO){case List(i,r,c,o) =>
        x(i,r,c,o)
      }{_+_}
    }

    val gold = data.reduce{_+_}
    val got = getArg(out)
    println(r"Got $got, expected $gold")
    println(r"PASS: ${got == gold}")
    assert(got == gold)
  }
}

@spatial class BroadcastRandom extends SpatialTest {
  override def compileArgs: Args = super.compileArgs and "--forceBanking"

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](64)
    val data = Array.tabulate(64){i => i % 5}
    setMem(dram,data)
    val out = DRAM[Int](3,3,64)
    Accel {
      val x = SRAM[Int](64) // Assert there is only 2 copies of this (or 1 copy if k is par 1)
      x load dram
      val y = SRAM[Int](3,3,64)
      val z = SRAM[Int](3,3)
      Foreach(3 by 1, 3 by 1){(i,j) => z(i,j) = i+j}

      Foreach(10 by 1){i => 
        Foreach(3 by 1, 3 by 1 par 2 /*1*/, 64 by 1 par 16){ (j,k,l) => 
          val cond1 = i % 2 == 0
          val cond2 = i % 3 == 0
          y(j,k,l) = x(mux(cond1, j, j*2) + mux(cond2, k, k*2)) + z(j,k)
        }
      }

      out store y
    }

    printTensor3(getTensor3(out), "Got:")
    val gold = (0::3, 0::3, 0::64){(j,k,l) => 
      val a = j*2
      val b = k
      data(a + b) + (j + k)
    }
    printTensor3(gold, "Gold:")
    assert(getTensor3(out) == gold)
  }

  override def checkIR(block: Block[_]): Result = {
    // the IR should have only 3 SRAMNew nodes
    super.checkIR(block)
  }


}

@spatial class BankLines extends SpatialTest {

  def main(args: Array[String]): Unit = {

    val P1 = 1
    val P2 = 3
    val P3 = 4
    val loadPar = 1

    // Memories
    val INPUT_DATA = DRAM[Int](5, 5, 32)
    val OUTPUT_DATA = DRAM[Int](5, 5, 32)

    val MAXROWS = ArgIn[Int]
    val MAXCOLS = ArgIn[Int]
    val ARG = ArgIn[Int]
    setArg(ARG, 1)
    setArg(MAXROWS, 5)
    setArg(MAXCOLS, 5)

    // Load data (placeholder)
    val input = (0::5, 0::5, 0::32) {(i,j,k) => ((i + j + k) % 64 - 8).to[Int]}

    // Set data
    setMem(INPUT_DATA, input)

    Accel{
      val in_sram = SRAM[Int](3,3,32).noflat
      Foreach(5 by 1){row =>
        Foreach(5 by 1){col => 
          val idx0 = row * ARG.value
          val idx1 = col * ARG.value
          Foreach(0 until 3 by 1 par P1, 0 until 3 by 1 par P2){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < MAXROWS.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < MAXCOLS.value) in_sram(i::i+1,j::j+1,0::32) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::32 par loadPar)
          }
          val out_sram = SRAM[Int](32)
          Foreach(32 by 1 par P3){k => 
            val data = List.tabulate(3){i => List.tabulate(3){j => 
              if (row - 1 + i >= 0 && row - 1 + i < 5 && col - 1 + j >= 0 && col - 1 + j < 5) in_sram(i,j,k) else 0
            }}.flatten.reduceTree{_+_} 

            out_sram(k) = data
          }
          OUTPUT_DATA(row,col,0::32) store out_sram
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    val gold = (0::5, 0::5, 0::32){(i,j,k) => 
      (0::3, 0::3){(ii,jj) => 
        if (i - 1 + ii >= 0 && i - 1 + ii < 5 && j - 1 + jj >= 0 && j - 1 + jj < 5) input(i-1+ii,j-1+jj,k) else 0
      }.flatten.reduce{_+_}
    }
    printTensor3(results, "Got: ")
    printTensor3(gold, "Wanted: ")
    println(r"PASS: ${gold == results}")
    assert(gold == results)



  }
}
