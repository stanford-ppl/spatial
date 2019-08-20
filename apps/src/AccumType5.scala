import spatial.dsl._

@spatial object AccumType5 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y5 = ArgOut[Int]
    setArg(X, 2.to[Int])
    Accel {

      /**
      .-----.
            t  |  t  |
             \ /  /  |
              +  |   |
              |  |   |
    i!=0 --> \````/  |
              ``|`   |
               _|    |
              |__|---'

        */
      val acc5 = Reg[Int](0) // Valid accumulation
      'ACC5.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc5 := mux(i != 0, t + acc5.value, t)
      }
      Y5 := acc5
    }

    val gold5 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    assert(Y5 == gold5)
  }
}
