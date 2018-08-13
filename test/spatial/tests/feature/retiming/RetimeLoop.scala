package spatial.tests.feature.retiming

import spatial.dsl._

@spatial class RetimeLoop extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](16,16)

    Accel {
      val x = Reg[Int]
      val sram = SRAM[Int](16, 16)
      x := Reduce(0)(0 until 16){i => i}{_+_}

      Foreach(0 until 16, 0 until 16){(i,j) =>
        sram(i,j) = ((i*j + 3) + x + 4) * 3
      }

      dram store sram
    }

    val x = Array.tabulate(16){i => i}.sum
    val gold = (0::16,0::16){(i,j) => ((i*j + 3) + x + 4) * 3 }
    assert(getMatrix(dram) == gold)
  }
}