import spatial.dsl._

@spatial object AccumType19 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y19 = ArgOut[Int]
    setArg(X, 2.to[Int])

    Accel {
      /**
        *            t   __
        *            /\ /  |
        *           |  +   |
        *           |  |   |
        * i==0 --> \````/  |
        * & j==0    ``|`   |
        *            _|    |
        *           |__|---'
        *
        */
      val acc19 = Reg[Int](0) // Invalid accumulation since the accum isn`t continuous
      'ACC13.Foreach(2 by 1, 4 by 1, 4 by 1) { (i, j, k) =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        // The invert case seems a bit more complex?
        // acc13 := mux(j != 0 || k != 0, t + acc13.value, t)
        acc19 := mux(i == 0 && j == 0, t, t + acc19.value)
      }
      Y19 := acc19

    }

    val Y19Result = getArg(Y19)
    val gold19 = (args(0).to[Int] * args(0).to[Int]) * 8
    println("gold = " + gold19)
    println("Y19Result = " + Y19Result)
    assert(Y19Result == gold19)
  }
}
