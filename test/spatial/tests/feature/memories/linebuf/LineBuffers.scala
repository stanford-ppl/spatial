package spatial.tests.feature.memories.linebuf

import spatial.dsl._

@spatial class LineBufs extends SpatialTest {

  val tests = Seq.fill(4) {true}

  def main(args: Array[String]): Unit = {
    println(s"REMEMBER: This app relies on the pipe binding transformer!")
    val init_dram = DRAM[I32](10,24)
    val init = (0::10,0::24){(i,j) => i*24 + j}
    val last_dram = DRAM[I32](3,24)
    val last_dram2 = DRAM[I32](3,24)
    val last_dram3 = DRAM[I32](3,24)
    val init4 = (0::12,0::26){(i,j) => i*26 + j}
    val init4_dram = DRAM[I32](12,26)
    val last_dram4 = DRAM[I32](7,25)
    setMem(init4_dram, init4)
    setMem(init_dram, init)

    Accel {
      if (tests(0)) {
        val lb = LineBuffer[I32](3, 24)
        val sram = SRAM[I32](3, 24)
        Foreach(10 by 1) { i =>
          lb load init_dram(i, 0 :: 24 par 4)
          Foreach(24 by 1 par 3) { j =>
            sram(0, j) = lb(0, j) // <--- newest data (highest #s in this app)
            sram(1, j) = lb(1, j)
            sram(2, j) = lb(2, j) // <--- oldest data (lowest #s in this app)
          }
        }
        last_dram store sram
      }
      if (tests(1)) {
        val lb1 = LineBuffer[I32](3, 24)
        val lb2 = LineBuffer[I32](3, 24)
        val sram2 = SRAM[I32](3, 24)
        Foreach(10 by 1) { i =>
          // Parallel{
          lb1 load init_dram(i, 0 :: 24 par 4)
          lb2 load init_dram(i, 0 :: 24 par 4)
          // }
          Foreach(24 by 1 par 3) { j =>
            sram2(0, j) = lb1(0, j) + lb2(0, j) // <--- newest data (highest #s in this app)
            sram2(1, j) = lb1(1, j) + lb2(1, j)
            sram2(2, j) = lb1(2, j) + lb2(2, j) // <--- oldest data (lowest #s in this app)
          }
        }
        last_dram2 store sram2
      }
      if (tests(2)) {
        val lb3 = LineBuffer.strided[I32](3, 24, 2)
        val sram3 = SRAM[I32](3, 24)
        Foreach(10 by 2) { i =>
          Foreach(2 by 1, 24 by 1 par 1) { (r, j) =>
            lb3.enqAt(r, (i + r) * 24 + j)
          }
          Foreach(24 by 1 par 3) { j =>
            sram3(0, j) = lb3(0, j) // <--- newest data (highest #s in this app)
            sram3(1, j) = lb3(1, j)
            sram3(2, j) = lb3(2, j) // <--- oldest data (lowest #s in this app)
          }
        }
        last_dram3 store sram3
      }

      if (tests(3)) {
        val lb4 = LineBuffer.strided[I32](8, 26, 2)
        val sram4 = SRAM[I32](7, 25).flat
        Foreach(12 by 2) { i =>
          lb4 load init4_dram(i :: i + 2, 0 :: 26)
          // Foreach(2 by 1, 25 by 1 par 1){(r,j) =>
          //   lb4.enqAt(r, (i+r)*26 + j)
          // }
          // Foreach(25 by 1 par 3, 7 by 1){(j,i) =>
          //   sram4(i,j) = lb4(1+i,j)
          // }
          Foreach(25 by 1 par 1) { j =>
            sram4(0, j) = lb4(1, j) // <--- newest data (highest #s in this app)
            sram4(1, j) = lb4(2, j)
            sram4(2, j) = lb4(3, j)
            sram4(3, j) = lb4(4, j)
            sram4(4, j) = lb4(5, j)
            sram4(5, j) = lb4(6, j)
            sram4(6, j) = lb4(7, j) // <--- oldest data (lowest #s in this app)
          }
        }
        last_dram4 store sram4

      }
    }

    if (tests(0)) {
      val got = getMatrix(last_dram)
      val gold = (0 :: 3, 0 :: 24) { (i, j) => init(7 + (2 - i), j) }
      printMatrix(got, "Got")
      printMatrix(gold, "Gold")
      println(r"Pass: ${got == gold}")
      assert(got == gold)
    }

    if (tests(1)) {
      val got2 = getMatrix(last_dram2)
      val gold2 = (0 :: 3, 0 :: 24) { (i, j) => init(7 + (2 - i), j) * 2 }
      printMatrix(got2, "Parallel LCA Got")
      printMatrix(gold2, "Parallel LCA Gold")
      println(r"Parallel Pass: ${got2 == gold2}")
      assert(got2 == gold2)
    }

    if (tests(2)) {
      val got3 = getMatrix(last_dram3)
      val gold3 = (0 :: 3, 0 :: 24) { (i, j) => init(7 + (2 - i), j) }
      printMatrix(got3, "EnqAt LCA Got")
      printMatrix(gold3, "EnqAt LCA Gold")
      println(r"EnqAt Pass: ${got3 == gold3}")
      assert(got3 == gold3)
    }

    if (tests(3)) {
      val got4 = getMatrix(last_dram4)
      val gold4 = (0 :: 7, 0 :: 25) { (i, j) => init4(9 + (1 - i), j) }
      printMatrix(got4, "EnqAt 7-row LCA Got")
      printMatrix(gold4, "EnqAt 7-row LCA Gold")
      println(r"EnqAt 7-row Pass: ${got4 == gold4}")
      assert(got4 == gold4)
    }
    assert(Bit(true))
  }
}