import spatial.dsl._

@spatial object AccumType6 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y6 = ArgOut[Int]
    setArg(X, 2.to[Int])
    Accel {
      /**
      .-----.
            t  |  t  |
             \ /  /  |
              +  |   |
              |  |   |
    i==0 --> \````/  |
              ``|`   |
               _|    |
              |__|---'

        */
      val acc6 = Reg[Int](0) // Not a valid accumulation because r = r + t for i == 0, then r = t else
      'ACC6.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc6 := mux(i == 0, t + acc6.value, t)
      }
      Y6 := acc6
    }

    val gold6 =  (args(0).to[Int] * args(0).to[Int]) * 8
    assert(Y6 == gold6)
  }
}
