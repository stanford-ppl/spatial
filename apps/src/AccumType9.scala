import spatial.dsl._

@spatial object AccumType9 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y9 = ArgOut[Int]
    setArg(X, 2.to[Int])

    Accel {
      /**
            .---------.
            |  0      |
            |  |      |
  i!=0 --> \````/     |
            ``|`      |
              |   t   |
               \ /    |
                +     |
               _|     |
              |__|----'

        */
      val acc9 = Reg[Int](0) // Valid accumulation, but only monsters would write it this way
      'ACC9.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc9 := mux(i != 0, acc9.value, 0) + t
      }
      Y9 := acc9
    }

    val Y9Result = getArg(Y9)
    val gold9 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    println("gold = " + gold9)
    println("Y9Result = " + Y9Result)
    assert(Y9Result == gold9)
  }
}
