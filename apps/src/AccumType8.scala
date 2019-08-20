import spatial.dsl._

@spatial object AccumType8 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y8 = ArgOut[Int]
    setArg(X, 2.to[Int])

    Accel {
      /**
      .-----.
                |  0  |
                |  |  |
      i!=0 --> \````/ |
                ``|`  |
              t   |   |
               \ /    |
                +     |
               _|     |
              |__|----'

        */
      val acc8 = Reg[Int](0) // Valid accumulation
      'ACC8.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc8 := t + mux(i != 0, acc8.value, 0)
      }
      Y8 := acc8

    }

    val Y8Result = getArg(Y8)
    val gold8 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    println("gold = " + gold8)
    println("Y8Result = " + Y8Result)
    assert(Y8Result == gold8)
  }
}
