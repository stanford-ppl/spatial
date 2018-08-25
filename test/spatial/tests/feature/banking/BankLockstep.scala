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
      val dataInConfl = SRAM[Int](16,16)   // Should bank N = 8
      val dataInNoConfl = SRAM[Int](16,16) // Should duplicate (can't broadcast), N = 4 and 2
      val dataOut = SRAM[Int](4,16,16)     // Should bank N = 4
      val cols_todo = SRAM[Int](16)
      cols_todo load cols_dram
      dataInConfl load dram
      dataInNoConfl load dram

      println(r"Force banking that I want by reading: ${dataInConfl(0,0)} ${dataInConfl(0,1)} ${dataInConfl(0,2)}")
      println(r"Force banking that I want by reading: ${dataInNoConfl(0,0)} ${dataInNoConfl(0,1)} ${dataInNoConfl(0,2)}")

      Foreach(16 by 1 par 2){b1 => 
        val todo = cols_todo(b1)
        Sequential.Foreach(4 by 1 par 1){ p => 
          val x = Reduce(Reg[Int])(todo by 1 par 2){q => 
            dataInConfl(b1,q) + dataInNoConfl(b1,0)
          }{_+_}
          Foreach(todo by 1 par 2){j => dataOut(p,b1,j) = x}
          Foreach(todo until 16 by 1 par 2){j => dataOut(p,b1,j) = 0}
        }
      }
      result store dataOut

    }

    val gold = (0::4,0::16,0::16){(_,i,j) => if (j < cols(i)) Array.tabulate(16){k => if (k < cols(i)) i*16 + k + i*16 else 0}.reduce{_+_} else 0}
    val got = getTensor3(result)
    printTensor3(got, "data")
    printTensor3(gold, "gold")
    assert(got == gold)
  }
}
