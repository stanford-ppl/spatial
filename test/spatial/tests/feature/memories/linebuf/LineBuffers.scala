package spatial.tests.feature.memories.linebuf

import spatial.dsl._

@spatial class LineBufs extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val init_dram = DRAM[I32](10,16)
    val init = (0::10,0::16){(i,j) => i*16 + j}
    val last_dram = DRAM[I32](3,16)
    setMem(init_dram, init)

    Accel {
      val lb = LineBuffer[I32](3,16)
      val sram = SRAM[I32](3,16)
      Foreach(10 by 1){i => 
        lb load init_dram(i, 0::16 par 4)
        Foreach(16 by 1 par 3){j => 
          sram(0,j) = lb(0,j) // <--- newest data (highest #s in this app)
          sram(1,j) = lb(1,j) 
          sram(2,j) = lb(2,j) // <--- oldest data (lowest #s in this app)
        }
      }
      last_dram store sram
    }

    val got = getMatrix(last_dram)
    val gold = (0::3,0::16){(i,j) => init(7 + (2-i), j)}
    printMatrix(got, "Got")
    printMatrix(gold, "Gold")
    println(r"Pass: ${got == gold}")
    assert(got == gold)

  }
}