package spatial.tests.feature.math

import spatial.dsl._


@spatial class FMAInLoop extends SpatialTest {

   def main(args: Array[String]): Unit = {
    @struct case class Result(a:Int, b:Int)
    val N = 16
    val M = 5
    val x = ArgOut[Result]

    Accel {
      val reg1 = Reg[Int](0)
      Foreach(N by 1){i => 
        val temp = i * i
        reg1 := reg1.value + temp // Should not fuse
      }
      val reg2 = Reg[Int](0)
      Foreach(M by 1){i => 
        val temp = reg2.value * i
        reg2 := i + temp // Fuse is ok
      }
      x := Result(reg1.value, reg2.value)
    }

    val gold1 = Array.tabulate(N){i => i * i}.reduce{_+_}
    val tmp = Array.empty[Int](N)
    for (i <- 0 until N) {
      if (i == 0) tmp(0) = 0
      else tmp(i) = tmp(i-1)*i + i
    }
    val gold2 = tmp(N-1)
    val gold = Result(gold1, gold2)
    println(r"got ${getArg(x)}, wanted $gold")
    // assert(gold == getArg(x))
  }
}
