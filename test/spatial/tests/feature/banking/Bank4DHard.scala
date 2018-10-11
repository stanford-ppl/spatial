package spatial.tests.feature.banking

import spatial.dsl._

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


@spatial class BankLines extends SpatialTest {

  def main(args: Array[String]): Unit = {

    val P1 = 1
    val P2 = 3
    val loadPar = 1

    // Memories
    val INPUT_DATA = DRAM[Int](5, 5, 32)
    val OUTPUT_DATA = DRAM[Int](5, 5, 32)

    // Load data (placeholder)
    val input = (0::5, 0::5, 0::32) {(i,j,k) => ((i + j + k) % 64 - 8).to[Int]}

    // Set data
    setMem(INPUT_DATA, input)

    Accel{
      val in_sram = SRAM[Int](3,3,32).hierarchical
      Foreach(5 by 1){row =>
        Foreach(5 by 1){col => 
          Foreach(0 until 3 by 1 par P1, 0 until 3 by 1 par P2){(i,j) => 
            if (row - 1 + i >= 0 && row - 1 + i < 5 && col - 1 + j >= 0 && col - 1 + j < 5) in_sram(i::i+1,j::j+1,0::32) load INPUT_DATA(row-1+i::row+i, col-1+j::col+j, 0::32 par loadPar)
          }
          val out_sram = SRAM[Int](32)
          Foreach(32 by 1){k => out_sram(k) = Reduce(Reg[Int])(3 by 1, 3 by 1){(i,j) => if (row - 1 + i >= 0 && row - 1 + i < 5 && col - 1 + j >= 0 && col - 1 + j < 5) in_sram(i,j,k) else 0}{_+_}}
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
