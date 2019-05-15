package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class StridedLoad extends SpatialTest {

  def main(args: Array[String]): Void = {
    val N = 192
    val src = (0::64,0::64){(i,j) => i+j}
    val dram = DRAM[Int](64,64)
    val dram2 = DRAM[Int](64,64)
    val out = DRAM[Int](32,64)
    val out2 = DRAM[Int](64,64)
    setMem(dram, src)
    setMem(dram2, src)
    Accel{
      val sram = SRAM[Int](32,64)
      sram load dram(0::64 by 2, 0::64)
      out store sram

      val sram2 = SRAM[Int](64,64)
      // sram2 load dram2(0::64 par 2, 0::64)
      Parallel{
        sram2(0::64 by 2, 0::64) load dram2(0::64 by 2, 0::64)
        sram2(1::64 by 2, 0::64) load dram2(1::64 by 2, 0::64)
      }
      out2 store sram2
    }

    val gold = (0::32, 0::64){(i,j) => src(2*i, j)}
    val gold2 = src
    val received = getMatrix(out)
    val received2 = getMatrix(out2)

    printMatrix(gold, "gold")
    printMatrix(received, "received")
    printMatrix(gold2, "gold2")
    printMatrix(received2, "received2")

    val cksum = received.zip(gold){_ == _}.reduce{_&&_}
    val cksum2 = received2.zip(gold2){_ == _}.reduce{_&&_}

    println("PASS: " + cksum + " (StridedLoad)")
    println("PASS2: " + cksum2 + " (StridedLoad)")
    assert(cksum && cksum2)
  }
}
