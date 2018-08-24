package spatial.tests.feature.banking

import spatial.dsl._

@spatial class BankLockstep extends SpatialTest {

  // Inspired by MD_Grid
  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](16,16)
    val result = DRAM[Int](4,16,16)
    val cols_dram = DRAM[Int](16)
    val cols = Array.tabulate(16){i => i%3 + 5}
    val data = (0::16,0::16){(i,j) => i*16 + j}
    setMem(dram, data)
    setMem(cols_dram, cols)

    Accel {
      val dataIn = SRAM[Int](16,16)
      val dataOut = SRAM[Int](4,16,16)
      val cols_todo = SRAM[Int](16)
      cols_todo load cols_dram
      dataIn load dram

      println(r"Force banking that I want by reading: ${dataIn(0,0)} ${dataIn(0,1)} ${dataIn(0,2)}")

      Foreach(16 by 1 par 2){b1 => 
        val todo = cols_todo(b1)
        Sequential.Foreach(4 by 1 par 1){ p => 
          val x = Reduce(Reg[Int])(todo by 1 par 2){q => 
            dataIn(b1,q)
          }{_+_}
          Foreach(todo by 1 par 2){j => dataOut(p,b1,j) = x}
          Foreach(todo until 16 by 1 par 2){j => dataOut(p,b1,j) = 0}
        }
      }
      result store dataOut

    }

    val gold = (0::4,0::16,0::16){(_,i,j) => if (j < cols(i)) Array.tabulate(16){k => if (k < cols(i)) i*16 + k else 0}.reduce{_+_} else 0}
    val got = getTensor3(result)
    printTensor3(got, "data")
    printTensor3(gold, "gold")
    assert(got == gold)
  }
}
