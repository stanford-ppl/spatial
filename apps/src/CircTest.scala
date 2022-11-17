import spatial.dsl._

@spatial object CircTest extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val out1 = ArgOut[Int]
//    val out2 = ArgOut[Int]
//    val out3 = ArgOut[Int]
    Accel {
      Pipe {
        val circ = Circ((x: Int) => x)

        val y1 = circ(20)
        out1 := y1
      }
//      out2 := 10
//
//      val s = SRAM[Int](16,32)
//      Foreach(16 by 1, 32 by 1) {(i, j) =>
//        s(i, j) = circ(i) + j
//      }
//
//      val y2 = circ(10)
//      out3 := y2
    }
    println(out1)
//    println(out2)
//    println(out3)
  }
}