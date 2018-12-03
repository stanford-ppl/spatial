package spatial.tests.feature.control

import spatial.dsl._

@spatial class ParallelPipeInsertion extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val dramA = DRAM[I32](64)
    val dramB = DRAM[I32](64)
    val dramC = DRAM[I32](32)
    val outA = ArgOut[I32]

    val dataA = Array.tabulate(64){i => random[I32] }
    val dataB = Array.tabulate(64){i => random[I32] }
    setMem(dramA, dataA)
    setMem(dramB, dataB)

    Accel {
      val a = SRAM[I32](64)
      val b = SRAM[I32](32)

      Foreach(5 by 1){i => 
        val x = Reg[Int]
        x := i
        Parallel{
          if (x.value % 2 == 0) a load dramA
          else                  a load dramB
          val start = mux(x.value % 2 == 0, 32, 0)
          b load dramA(start::start+32)
        }
        outA := a(5)
        dramC store b
      }
    }

    val goldA = dataA(5)
    val notGoldA = dataB(5)
    val goldC = Array.tabulate(32){i => dataA(i+32)}
    println(r"Wanted $goldA (not $notGoldA), got ${getArg(outA)}")
    printArray(goldC, "GoldC")
    printArray(getMem(dramC), "GotC")

    // assert(getArg(outA) == goldA && getMem(dramC) == goldC)
  }
}
