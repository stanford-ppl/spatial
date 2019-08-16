import spatial.dsl._

@spatial object AccumType3 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y3 = ArgOut[Int]
    setArg(X, 2.to[Int])
    Accel {

      /**
      .----.
             t  | t  |
              | \ /  |
              |  +   |
              |  |   |
    i==0 --> \````/  |
              ``|`   |
               _|    |
              |__|---'

        */
      val acc3 = Reg[Int](0) // Valid accumulation
      'ACC3.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc3 := mux(i == 0, t, acc3.value + t)
      }
      Y3 := acc3
    }

    val gold3 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    assert(Y3 == gold3)
  }
}
