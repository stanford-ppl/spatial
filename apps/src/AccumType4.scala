import spatial.dsl._

@spatial object AccumType4 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y4 = ArgOut[Int]
    setArg(X, 2.to[Int])
    Accel {
      /**
      .-------.
             | t     |
             \ /\    |
              +  |   |
              |  |   |
    i!=0 --> \````/  |
              ``|`   |
               _|    |
              |__|---'

        */
      // TODO: think of a case that would make this part wrong...
      val acc4 = Reg[Int](0) // Valid accumulation
      'ACC4.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc4 := mux(i != 0, acc4.value + t, t)
      }
      Y4 := acc4
    }

    val gold4 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    assert(Y4 == gold4)
  }
}
