package spatial.tests.feature.transfers


import spatial.dsl._


@test class StridedLoad extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val N = 192
    val src = (0::64,0::64){(i,j) => i+j}
    val dram = DRAM[Int](64,64)
    val out = DRAM[Int](32,64)
    setMem(dram, src)
    Accel{
      val sram = SRAM[Int](32,64)
      sram load dram(0::64 by 2, 0::64)
      out store sram
    }

    val gold = (0::32, 0::64){(i,j) => src(2*i, j)}
    val received = getMatrix(out)

    printMatrix(gold, "gold")
    printMatrix(received, "received")

    val cksum = received.zip(gold){_ == _}.reduce{_&&_}
    println("PASS: " + cksum + " (StridedLoad)")
    assert(cksum)
  }
}
