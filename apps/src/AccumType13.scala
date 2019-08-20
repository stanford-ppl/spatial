import spatial.dsl._

@spatial object AccumType13 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y13 = ArgOut[Int]
    setArg(X, 2.to[Int])

    Accel {
      /**
        *            t   __
        *            /\ /  |
        *           |  +   |
        *           |  |   |
        * j==0 --> \````/  |
        * & k==0    ``|`   |
        *            _|    |
        *           |__|---'
        *
        */
      val acc13 = Reg[Int](0) // Valid accumulation but tricky because compound ctr chain (watch your reset condition)
      'ACC13.Foreach(2 by 1, 4 by 1, 4 by 1) { (i, j, k) =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        // The invert case seems a bit more complex?
        // acc13 := mux(j != 0 || k != 0, t + acc13.value, t)
        acc13 := mux(j == 0 && k == 0, t, t + acc13.value)
        // TODO: is this a valid accum type?
        // acc13 := mux(i == 0 && j == 0, t, t + acc13.value)
      }
      Y13 := acc13

    }

    val Y13Result = getArg(Y13)
    val gold13 = (args(0).to[Int] * args(0).to[Int]) * 8
    println("gold = " + gold13)
    println("Y13Result = " + Y13Result)
    assert(Y13Result == gold13)
  }
}
