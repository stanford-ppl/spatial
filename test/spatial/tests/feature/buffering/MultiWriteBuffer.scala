package spatial.tests.feature.buffering

import spatial.dsl._

@spatial class MultiWriteBuffer extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val R = 16
    val C = 16

    val mem = DRAM[Int](R, C)
    //val y = ArgOut[Int]

    Accel {
      val accum = SRAM[Int](R, C)
      MemReduce(accum)(1 until (R+1)) { row =>
        val sram_seq = SRAM[Int](R, C).buffer
        Foreach(0 until R, 0 until C) { (r, c) =>
          sram_seq(r,c) = 0
        }
        Foreach(0 until C) { col =>
          sram_seq(row-1, col) = 32*(row-1 + col)
        }
        sram_seq
      }{ (sr1, sr2) => sr1 + sr2 }

      mem store accum
    }

    val result = getMatrix(mem)
    val gold = (0::R, 0::C){(i,j) => 32*(i+j)}
    printMatrix(gold, "Gold:")
    printMatrix(result, "Result:")

    val cksum = gold.zip(result){_==_}.reduce{_&&_}
    assert(cksum)
    println("PASS: " + cksum + " (MultiWriteBuffer)")
  }
}

