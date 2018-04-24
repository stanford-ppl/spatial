package spatial.tests.syntax

import spatial.dsl._

@test class IntPrinting extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    Accel { }
    val x = -391880660.to[Int]
    println("x: " + x)
  }
}

// @test class RegLifting extends SpatialTest { // ReviveMe
//   override def runtimeArgs: Args = NoArgs

//   def main(args: Array[String]): Unit = {
//     Accel {
//       val reg = Reg[Int](3)
//       val a = reg + 4
//       val b = reg - 4
//       val c = reg * 4
//       val d = reg / 4
//       val a1 = reg < 4
//       val b1 = reg <= 4
//       val c1 = reg > 4
//       val d1 = reg >= 4

//       val e = 4 + reg
//       val f = 4 - reg
//       val g = 4 * reg
//       val h = 4 / reg
//       val e1 = 4 < reg
//       val f1 = 4 <= reg
//       val g1 = 4 > reg
//       val h1 = 4 >= reg

//       // TODO: Issue #157
//       val w = reg != 4
//       val x = reg == 4
//       val y = 4 != reg
//       val z = 4 == reg
//       println(a)
//     }
//   }
// }
