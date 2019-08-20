import spatial.dsl._

@spatial object AccumType10 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y10 = ArgOut[Int]
    setArg(X, 2.to[Int])

    Accel {
      /**
      .------.
            0  |      |
            |  |      |
  i==0 --> \````/     |
            ``|`      |
              |   t   |
               \ /    |
                +     |
               _|     |
              |__|----'

        */
      val acc10 = Reg[Int](0) // Valid accumulation
      'ACC10.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc10 := mux(i == 0, 0, acc10.value) + t
      }
      Y10 := acc10
    }

    val Y10Result = getArg(Y10)
    val gold10 = (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    println("gold = " + gold10)
    println("Y10Result = " + Y10Result)
    assert(Y10Result == gold10)
  }
}
