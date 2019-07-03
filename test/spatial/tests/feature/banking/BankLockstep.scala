package spatial.tests.feature.banking

import argon.Block
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
      val dataInConfl = SRAM[Int](16,16)   // Should bank N = 8
      val dataInNoConfl = SRAM[Int](16,16) // Should duplicate (can't broadcast), N = 4 and 2
      val dataOut = SRAM[Int](4,16,16).hierarchical     // Should bank N = 4
      val cols_todo = SRAM[Int](16).noduplicate
      cols_todo load cols_dram
      dataInConfl load dram
      dataInNoConfl load dram

      println(r"Force banking that I want by reading: ${dataInConfl(0,0)} ${dataInConfl(0,1)} ${dataInConfl(0,2)}")
      println(r"Force banking that I want by reading: ${dataInNoConfl(0,0)} ${dataInNoConfl(0,1)} ${dataInNoConfl(0,2)}")

      Foreach(16 by 1 par 2){b1 => 
        val todo = cols_todo(b1)
        Sequential.Foreach(4 by 1 par 2){ p => 
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

@spatial class DephasingDuplication extends SpatialTest {
  override def dseModelArgs: Args = "16"
  override def finalModelArgs: Args = "16"


  // Inspired by MD_Grid
  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](4,16)
    val result = DRAM[Int](4)
    val out = ArgOut[Int]
    val data = (0::4,0::16){(i,j) => i*16 + j}
    val in = ArgIn[Int]
    setArg(in, 3)
    setMem(dram, data)

    Accel {
      val sram = SRAM[Int](4,16).hierarchical.noduplicate // Only try to bank hierarchically to expose bug #123
      val sram2 = SRAM[Int](4)
      sram load dram
      out := 'LOOP1.Reduce(Reg[Int])(4 by 1 par 2){i => 
        val max = if (in.value < 10) 16 else 8 // Should always choose 16, but looks random to compiler
        Reduce(Reg[Int])(max by 1 par 2){j =>  // j should dephase relative to different lanes of LOOP1 since this is variable counter
          sram(i,j)
        }{_+_}
      }{_+_}

    }

    val gold = data.flatten.reduce{_+_}
    val got = getArg(out)
    println(r"gold $gold =?= $out out")
    assert(out == gold)
  }

  override def checkIR(block: Block[_]): Result = {
    import argon._
    import spatial.metadata.memory._
    import spatial.node._

    // We should have 2 dups of sram and one of sram2
    val srams = block.nestedStms.collect{case sram:SRAMNew[_,_] => sram }
    if (srams.size > 0) assert (srams.size == 3)

    super.checkIR(block)
  }

}

@spatial class SynchronizedStartsTest extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val dummy = ArgIn[Int]
    setArg(dummy, 5)
    val P1 = 2
    val P2 = 2

    Accel {
      def fillSRAM(s: SRAM1[Int]): Unit = Foreach(s.size by 1){i => s(i) = i}

      // All controllers are synchronized and happy
      val sram1 = SRAM[Int](16*16)
      fillSRAM(sram1)
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        'LAYERB.Foreach(4 by 1, 4 by 1){(b0, b1) => 
          'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
          'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => println(r"${sram1(a0*16 + c0)}")}
        }
      }

      // LAYERC_STAGE0 causes c0 to be non-synchronized (even if STAGE0 and STAGE1 were swapped, since LAYERB is a loop)
      val sram2 = SRAM[Int](16*16)  // Must duplicate for each lane of LAYERA (P1 duplicates)
      fillSRAM(sram2)
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        'LAYERB.Foreach(4 by 1, 4 by 1){(b0, b1) => 
          'LAYERC_STAGE0.Foreach(start until 4 by 1){_ => println(r"$dummy")}
          'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => println(r"${sram2(a0*16 + c0)}")}
        }
      }

      // All controllers are synchronized.  Even though LAYERB has uid-variant runtime, LAYERA unrolls it as MoP
      val sram3 = SRAM[Int](16*16)
      fillSRAM(sram3)
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        'LAYERB.Foreach(4 by 1, 4 + start by 1){(b0, b1) => 
          'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
          'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => println(r"${sram3(a0*16 + c0)}")}
        }
      }

      // No controllers are synchronized, because LAYERA unrolls as PoM and LAYERB causes uid-variant runtime
      val sram4 = SRAM[Int](16*16) // Must duplicate for each lane of LAYERA and LAYERC_STAGE1 (P1*P2 duplicates)
      fillSRAM(sram4)
      'LAYERA.Pipe.POM.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        'LAYERB.Foreach(4 by 1, 4 + start by 1){(b0, b1) => 
          'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
          'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => println(r"${sram4(a0*16 + c0)}")}
        }
      }

      // All controllers are synchronized, but LAYERC_STAGE1 iters of uid(a0)=1 are offset by 1, because LAYERA unrolls as PoM and LAYERB causes uid-variant runtime
      val sram5 = SRAM[Int](16*16)
      fillSRAM(sram5)
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        'LAYERB.Foreach(4 by 1, 4 by 1){(b0, b1) => 
          'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
          'LAYERC_STAGE1.Foreach(start until 16 by 1 par P2){c0 => println(r"${sram5(a0*16 + c0)}")}
        }
      }


    }

    assert(true)
  }
}
