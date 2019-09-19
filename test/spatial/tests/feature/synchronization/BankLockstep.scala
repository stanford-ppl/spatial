package spatial.tests.feature.synchronization

object BLHelper{
  def contains(a: Option[String], b: String): Boolean = {a.getOrElse("").indexOf(b) != -1}
}

import argon.Block
import argon.Op
import spatial.node._
import spatial.dsl._

@spatial class BankLockstep extends SpatialTest {
  override def dseModelArgs: Args = "6"
  override def finalModelArgs: Args = "6"

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
      val dataInConfl = SRAM[Int](16,16)//.flat?   // Should duplicate
      val dataInNoConfl = SRAM[Int](16,16)//.flat?  // Should not duplicate
      val dataOut = SRAM[Int](4,16,16).hierarchical     // Should bank N = 4
      val cols_todo = SRAM[Int](16).nofission
      cols_todo load cols_dram
      dataInConfl load dram
      dataInNoConfl load dram

      println(r"Force banking that I want by reading: ${dataInConfl(0,0)} ${dataInConfl(0,1)} ${dataInConfl(0,2)}")
      println(r"Force banking that I want by reading: ${dataInNoConfl(0,0)} ${dataInNoConfl(0,1)} ${dataInNoConfl(0,2)}")

      Foreach(16 by 1 par 2){b1 => 
        val todo = cols_todo(b1)
        val t = dataInNoConfl(b1, 0)
        Sequential.Foreach(4 by 1 par 2){ p => 
          val x = Reduce(Reg[Int])(todo by 1 par 2){q => 
            dataInConfl(b1,q) + t
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

  override def checkIR(block: Block[_]): Result = {
    val dataInConfl_count = block.nestedStms.collect{case x@Op(sram:SRAMNew[_,_]) if BLHelper.contains(x.name, "dataInConfl") => sram }.size
    val dataInNoConfl_count = block.nestedStms.collect{case x@Op(sram:SRAMNew[_,_]) if BLHelper.contains(x.name, "dataInNoConfl") => sram }.size

    require(dataInConfl_count == 2, "Should only have 2 duplicates of dataInConfl")
    require(dataInNoConfl_count == 1, "Should only have 1 duplicate of dataInNoConfl")

    super.checkIR(block)
  }

}

