import spatial.dsl._

// PASSED. Seems that the Reduce type is overriden by noIntermediates...
@spatial object AccumType2 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y2 = ArgOut[Int]
    setArg(X, 2.to[Int])
    Accel {
      /**
      t   __
               /\ /  |
              |  +   |
              |  |   |
    i==0 --> \````/  |
              ``|`   |
               _|    |
              |__|---'

        */
      val acc2 = Reg[Int](0) // Valid accumulation
      'ACC2.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc2 := mux(i == 0, t, t + acc2.value)
      }
      Y2 := acc2
    }

    val gold2 = (args(0).to[Int] * args(0).to[Int]) * (4-1) * 8
    assert(Y2 == gold2)
  }
}
