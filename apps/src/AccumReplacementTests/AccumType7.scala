package AccumReplacementTests

import spatial.dsl._

@spatial object AccumType7 extends SpatialApp with AccumBase {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y7 = ArgOut[Int]
    setArg(X, 2.to[Int])
    Accel {
      val acc7 = Reg[Int](0) // Valid accumulation
      'ACC7.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc7 := t + mux(i == 0, 0, acc7.value)
      }
      Y7 := acc7

    }

    val gold7 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    println("gold = ", gold7)
    println("Y7 = ", Y7)
    assert(Y7 == gold7)
  }

}
