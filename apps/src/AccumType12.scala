import spatial.dsl._

@spatial object AccumType12 extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    val Y12 = ArgOut[Int]
    setArg(X, 2.to[Int])

    Accel {

      /**
      t   __
               /\ /  |
              |  +   |
              |  |   |
    j==0 --> \````/  |
              ``|`   |
               _|    |
              |__|---'

        */
      val acc12 = Reg[Int](0) // Not a valid accumulation because mux condition is not based on innermost iterators
      'ACC12.Foreach(2 by 1, 4 by 1, 4 by 1) { (i,j,k) =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc12 := mux(j == 0, t, t + acc12.value)
      }
      Y12 := acc12

    }

    val Y12Result = getArg(Y12)
    val gold12 = (args(0).to[Int] * args(0).to[Int]) * 8
    println("gold = " + gold12)
    println("Y12Result = " + Y12Result)
    assert(Y12Result == gold12)
    }
  }
