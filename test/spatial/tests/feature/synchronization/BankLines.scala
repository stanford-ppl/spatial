package spatial.tests.feature.synchronization

import spatial.dsl._
import argon.Block
import argon.Op
import spatial.node._

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
      val in_sram = SRAM[Int](3,3,32).hierarchical.nofission
      Foreach(5 by 1){row =>
        Foreach(5 by 1){col =>
          val idx0 = row * ARG.value
          val idx1 = col * ARG.value
          Foreach(0 until 3 by 1 par P1, 0 until 3 by 1 par P2){(i,j) =>
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < MAXROWS.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < MAXCOLS.value) in_sram(i::i+1,j::j+1,0::32) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::32 par loadPar)
          }
          val out_sram = SRAM[Int](32)
          Foreach(32 by 1 par P3){k =>
            val data = List.tabulate(3,3){(i,j) =>
              if (row - 1 + i >= 0 && row - 1 + i < 5 && col - 1 + j >= 0 && col - 1 + j < 5) in_sram(i,j,k) else 0
            }.flatten.reduceTree{_+_}

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


@spatial class BankLines2 extends SpatialTest {

  def main(args: Array[String]): Unit = {
    /*
       This app checks if the hierarchical banking works, specifically the interplay between lockstep-ness and projections.
       bankrow:
           O   --->
                        O  --->
           |--unk-dist--|

       bankcol:
           O   _ _
           |    |
           |    |  unknown dist
           \/   |
               _|_
              O
              |
              |
              \/

        bankdiag
           O  --->
             O  --->
          |-|
           lockstep dist



     */
    val P1 = 3

    val bankrow_dram = DRAM[Int](8,8)
    val bankcol_dram = DRAM[Int](8,8)
    val bankdiag_dram = DRAM[Int](8,8)

    Accel{
      val bankrow = SRAM[Int](8,8).hierarchical.nofission
      Foreach(8 by 1, 8 by 1){(i,j) => bankrow(i,j) = 0}
      val starts = LUT[Int](8)(Seq.tabulate(8){i => (i % 4).to[Int]}:_*)
      Foreach(8 by 1 par P1){row =>
        Foreach(starts(row) until 8 by 1){col => bankrow(row,col) = row + col}
      }
      bankrow_dram store bankrow

      val bankcol = SRAM[Int](8,8).hierarchical.nofission
      Foreach(8 by 1, 8 by 1){(i,j) => bankcol(i,j) = 0}
      Foreach(8 by 1 par P1){col =>
        Foreach(starts(col) until 8 by 1){row => bankcol(row,col) = row + col}
      }
      bankcol_dram store bankcol

      val bankdiag = SRAM[Int](8,8).hierarchical.nofission
      Foreach(8 by 1, 8 by 1){(i,j) => bankdiag(i,j) = 0}
      Foreach(8 by 1 par P1){row =>
        Foreach((row % P1) until 8 by 1){col => bankdiag(row,col) = row + col}
      }
      bankdiag_dram store bankdiag
    }

    // Get results
    val goldrow = (0::8,0::8){(i,j) => if (j < (i % 4)) 0 else (i + j)}
    val goldcol = (0::8,0::8){(j,i) => if (j < (i % 4)) 0 else (i + j)}
    val golddiag = (0::8,0::8){(i,j) => if (j < (i % P1)) 0 else (i + j)}
    val gotrow = getMatrix(bankrow_dram)
    val gotcol = getMatrix(bankcol_dram)
    val gotdiag = getMatrix(bankdiag_dram)
    printMatrix(goldrow, "gold row")
    printMatrix(gotrow, "got row")
    printMatrix(goldcol, "gold col")
    printMatrix(gotcol, "got col")
    printMatrix(golddiag, "gold diag")
    printMatrix(gotdiag, "got diag")
    assert (goldrow == gotrow && goldcol == gotcol && golddiag == gotdiag)

  }
}
