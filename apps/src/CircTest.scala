import spatial.dsl._

@spatial object CircTest extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val out1 = ArgOut[Int]
    Accel {
      Pipe {
        val circ = Circ((x: Int) => x + 99)
        var y1 = circ(20)
        Pipe {
          val circ = Circ((x: Int) => x + 999)
          val y2 = circ(20)
          y1 = y1 + y2
        }
        out1 := y1
      }

//      Pipe {
//        val circ = Circ((x: Int) => x + 99)
//        val y1 = circ(20)
//        val mem = SRAM[Int](8)
//        Foreach(4 by 1) { j =>
//          mem(j % 2) += y1
//        }
//        mem(0) = circ(20)
//        out1 := y1 + mem(0)
//      }

//      Pipe {
//        val circ = Circ((x: Int) => x + 99)
//        val y1 = circ(20)
//        val mem = SRAM[Int](8)
//        Foreach(4 by 1) { j =>
//          mem(j % 2) += 1
//        }
//        mem(0) = circ(20)
//        out1 := y1 + mem(0)
//      }

//      val mem1 = SRAM[Int](8)
//      Foreach(8 by 1) { i =>
//        val x = mem1(0)
//        mem1(0) = 10
//        val mem2 = SRAM[Int](2)
//        mem1(2) = x
//        Foreach(4 by 1) { j =>
//          mem2(j % 2) += 1
//        }
//        mem1(i) = mem2(0) + mem2(1)
//      }
//      out1 := mem1(0)
    }
    println(out1)
  }
}