import spatial.dsl._

@spatial object AccumType1 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y1 = ArgOut[Int]
    setArg(X, 2.to[Int])
    Accel {
      val acc1 = Reg[Int](0)  // Not a valid accumulation because r = 0 for i == 0, then r = r + t else
      'ACC1.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc1 := mux(i == 0, 0, acc1.value + t)
      }
      Y1 := acc1
    }

    val gold1 = (args(0).to[Int] * args(0).to[Int]) * (4-1) * 8
    assert(Y1 == gold1)
  }
}
