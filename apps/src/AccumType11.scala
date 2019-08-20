import spatial.dsl._

@spatial object AccumType11 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y11 = ArgOut[Int]
    setArg(X, 2.to[Int])

    Accel {
      /**
               .------.
            0  |      |
            |  |      |
  ???? --> \````/     |
            ``|`      |
              |   t   |
               \ /    |
                +     |
               _|     |
              |__|----'

        */
      val acc11 = Reg[Int](0) // Not a valid accumulation because mux sel is based on something weird
      'ACC11.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc11 := mux(X.value == 0, acc11.value, 0) + t
      }
      Y11 := acc11

    }

    val Y11Result = getArg(Y11)
    val gold11 = (args(0).to[Int] * args(0).to[Int]) * 8
    println("gold = " + gold11)
    println("Y11Result = " + Y11Result)
    assert(Y11Result == gold11)
  }
}
