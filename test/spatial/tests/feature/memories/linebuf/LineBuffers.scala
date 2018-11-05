package spatial.tests.feature.memories.linebuf

import spatial.dsl._

@spatial class LineBufs extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val init_dram = DRAM[I32](10,16)
    val init = (0::10,0::16){(i,j) => i*16 + j}
    val last_dram = DRAM[I32](3,16)
    val last_dram2 = DRAM[I32](3,16)
    val last_dram3 = DRAM[I32](3,16)
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

      val lb1 = LineBuffer[I32](3,16)
      val lb2 = LineBuffer[I32](3,16)
      val sram2 = SRAM[I32](3,16)
      Foreach(10 by 1){i => 
        Parallel{
          lb1 load init_dram(i, 0::16 par 4)
          lb2 load init_dram(i, 0::16 par 4)  
        }
        Foreach(16 by 1 par 3){j => 
          sram2(0,j) = lb1(0,j) + lb2(0,j) // <--- newest data (highest #s in this app)
          sram2(1,j) = lb1(1,j) + lb2(1,j) 
          sram2(2,j) = lb1(2,j) + lb2(2,j) // <--- oldest data (lowest #s in this app)
        }
      }
      last_dram2 store sram2

      val lb3 = LineBuffer.strided[I32](3,16,2)
      val sram3 = SRAM[I32](3,16)
      Foreach(10 by 2){i => 
        Foreach(2 by 1, 16 by 1 par 1){(r,j) => 
          lb3.enqAt(r, (i+r)*16 + j)
        }
        Foreach(16 by 1 par 3){j => 
          sram3(0,j) = lb3(0,j) // <--- newest data (highest #s in this app)
          sram3(1,j) = lb3(1,j) 
          sram3(2,j) = lb3(2,j) // <--- oldest data (lowest #s in this app)
        }
      }
      last_dram3 store sram3

    }

    val got = getMatrix(last_dram)
    val gold = (0::3,0::16){(i,j) => init(7 + (2-i), j)}
    printMatrix(got, "Got")
    printMatrix(gold, "Gold")

    val got2 = getMatrix(last_dram2)
    val gold2 = (0::3,0::16){(i,j) => init(7 + (2-i), j)*2}
    printMatrix(got2, "Parallel LCA Got")
    printMatrix(gold2, "Parallel LCA Gold")

    val got3 = getMatrix(last_dram3)
    val gold3 = (0::3,0::16){(i,j) => init(7 + (2-i), j)}
    printMatrix(got3, "EnqAt LCA Got")
    printMatrix(gold3, "EnqAt LCA Gold")

    println(r"Pass: ${got == gold}")
    println(r"Parallel Pass: ${got2 == gold2}")
    println(r"EnqAt Pass: ${got3 == gold3}")

    assert(got == gold && got2 == gold2 && got3 == gold3)

  }
}