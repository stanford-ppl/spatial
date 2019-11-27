package spatial.tests.feature.buffering

import spatial.dsl._

@spatial class BufferHazard extends SpatialTest {
  /** You have to squint a little but the compiler will naturally buffer the SRAM
    * "bufhaz."  The buffer swaps for each iteration of l1, so each time we increment the
    * write row, we also swap the buffer.  Because L1 is even and has 2 stages, this means when E increments,
    * each time we read a row, we are reading the row from bufhaz on the buffer that was NOT written to.
    *   
    */

  def main(args: Array[String]): Unit = {

    val result_dram = DRAM[Int](8, 8)
    val init = Array.tabulate(8){i => i}
    val init_dram = DRAM[Int](8)
    setMem(init_dram, init)

    Accel {
      val bufhaz = SRAM[I32](8, 8).nonbuffer // Should throw error without flag
      val init_sram = SRAM[I32](8)
      val tmp = SRAM[I32](8)
      val result = SRAM[I32](8, 8)
      init_sram load init_dram
      Sequential.Foreach(2 by 1){E => 
        'LCA.Foreach(8 by 1 par 1) { l1 =>

          Foreach(0 until 8) { l2 =>
            tmp(l2) = Reduce(Reg[I32])(8 by 1){ l3 => mux(E == 0, init_sram(l3), bufhaz(l1, l3)) }{_+_}
          }

          Foreach(8 by 1){ l2 => 
            if (E == 0) {
              bufhaz(l1, l2) = tmp(l2)
            } else {
              result(l1, l2) = tmp(l2)
            }
          }
        }
      }
      result_dram store result
    }

    val got = getMatrix(result_dram)
    printMatrix(got, "Got: ")
    val cksum = got.flatten.map{x => x == 224}.reduce{_&&_}
    println("PASS: " + cksum + " (BufferHazard)")
    assert(cksum)
  }
}
