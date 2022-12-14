package spatial.tests.compiler

import spatial.dsl._

@spatial class CircTest extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val out1 = ArgOut[Int]
    Accel {
//      // BUG: VarRead does not show up
//      Pipe {
//        val circ = Circ((x: Int) => x + 99)
//        var y1 = circ(20)
//        Pipe {
//          val circ = Circ((x: Int) => x + 999)
//          val y2 = circ(20)
//          y1 = y1 + y2
//        }
//        out1 := y1
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

      Pipe {
        // PIPE 1
        val reg = Reg[Int](0)
        val circ = Circ((x: Int) => x + 99)
        val y1 = circ(20)
        val y2 = circ(20)

        // PIPE 2 (y1.deq)
        val x1 = y1 + 20
        reg.write(100)

        // PIPE 3 (y2.deq)
        val x2 = y2 + 20
        val x3 = x1 + x2
        val x4 = x3 + y1
        val x5 = x4 + reg.value
        out1 := x5
      }
    }
    assert(out1.value == I32(497), r"Expected 497, found: ${out1.value}")
  }
}