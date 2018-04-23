package spatial.tests.feature.unit

import spatial.dsl._

@test class MixedIO extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    val cst1 = 32
    val cst2 = 23;
    val cst3 = 11
    val cst4 = 7
    val io1 = HostIO[Int]
    val io2 = HostIO[Int]
    val io_unused = HostIO[Int]
    val x1 = ArgIn[Int]
    val x2 = ArgIn[Int]
    val x_unused = ArgIn[Int]
    val y1 = ArgOut[Int]
    val y2 = ArgOut[Int]
    val y3 = ArgOut[Int]
    val y4 = ArgOut[Int]
    val y5 = ArgOut[Int]
    val y_unused = ArgOut[Int]
    val m1 = DRAM[Int](16)
    val m2 = DRAM[Int](16)
    val m_unused = DRAM[Int](16)
    setArg(io1, cst1)
    setArg(io2, cst2)
    setArg(x1, cst3)
    setArg(x2, cst4)
    val data = Array[Int](0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15)
    // val data = Array.tabulate(16){i => i}
    setMem(m1, data)

    Accel {
      Pipe { io1 := io1.value + 2}
      Pipe { io2 := io2.value + 4}
      Pipe { y2 := 999 }
      Pipe { y1 := x1.value + 6 }
      Pipe { y2 := x2.value + 8 }
      val reg = Reg[Int](0) // Nbuffered reg with multi writes, note that it does not do what you think!
      Sequential.Foreach(3 by 1) {i =>
        Pipe{reg :+= 1}
        Pipe{y4 := reg}
        Pipe{reg :+= 1}
        Pipe{y5 := reg}
      }
      val sram1 = SRAM[Int](16)
      val sram2 = SRAM[Int](16)
      sram1 load m1
      sram2 load m1
      m2 store sram1
      Pipe { y3 := sram2(3) }
    }

    val r1 = getArg(io1)
    val g1 = cst1 + 2
    val r2 = getArg(io2)
    val g2 = cst2 + 4
    val r3 = getArg(y1)
    val g3 = cst3 + 6
    val r4 = getArg(y2)
    val g4 = cst4 + 8
    val r5 = getMem(m2)
    val g6 = data(3)
    val r6 = getArg(y3)
    val g7 = 5
    val r7 = getArg(y4)
    val g8 = 6
    val r8 = getArg(y5)
    println("expected: " + g1 + ", " + g2 + ", " + g3 + ", " + g4 + ", "+ g6 + ", " + g7 + ", " + g8)
    println("received: " + r1 + ", " + r2 + ", " + r3 + ", " + r4 + ", "+ r6 + ", " + r7 + ", " + r8)
    printArray(r5, "Mem: ")
    val cksum = r1 == g1 && r2 == g2 && r3 == g3 && r4 == g4 && r6 == g6 && data.zip(r5){_==_}.reduce{_&&_} && r7 == g7 && r8 == g8
    assert(cksum)
  }
}
