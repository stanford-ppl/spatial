import spatial.dsl._

@spatial object AccumType14 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y14 = ArgOut[Int]
    setArg(X, 2.to[Int])

    Accel {
      /**
      .--.Wv
                0  |  |
                |  |  |
      i==0 --> \````/ |
                ``|`  |
              t1  |   |
               \ /    |
                +     |
           t2   |     |
             \ /      |
              +       |
             _|       |
            |__|------'

        */
      val acc14 = Reg[Int](0) // Valid accumulation but need to be careful about capturing the add chain
      'ACC14.Foreach(4 by 1) { i =>
        val t1 = List.tabulate(8) { j => X.value * X.value }.sumTree
        val t2 = List.tabulate(8) { i => X.value }.sumTree
        acc14 := t1 + mux(i == 0, 0, acc14.value) + t2
        // what about a case that looks like:
        // acc14 := t1 + t2 + mux(i == 0, 0, acc14.value)
      }
      Y14 := acc14
    }

    val Y14Result = getArg(Y14)
    val gold14 = (args(0).to[Int] * args(0).to[Int]) * 8
    println("gold = " + gold14)
    println("Y14Result = " + Y14Result)
    assert(Y14Result == gold14)
  }
}
