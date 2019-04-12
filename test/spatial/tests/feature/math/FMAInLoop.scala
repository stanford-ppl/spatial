package spatial.tests.feature.math

import spatial.dsl._


@spatial class FMAInLoop extends SpatialTest {

   def main(args: Array[String]): Unit = {
    @struct case class Result(a:I16, b:I16, c:I16)
    val N = 16
    val M = 5
    val x = ArgOut[Result]

    Accel {
      val rf1 = RegFile[I16](3,3,Seq.fill[I16](9)(0))
      Foreach(N by 1){i => 
        val temp = (i * i).to[I16]
        rf1(0,0) = rf1(0,0) + temp // Should not fuse
      }
      val rf2 = RegFile[I16](3,3,Seq.fill[I16](9)(0))
      Foreach(M by 1){i => 
        val temp = rf2(0,0) * i.to[I16]
        rf2(0,0) = i.to[I16] + temp // Fuse is ok
      }
      val reg = Reg[I16](0)
      Foreach(N by 1){i => 
        val temp = (i * i).to[I16]
        reg := reg.value + temp // Fuse is ok (because specialized reduce)
      }
      x := Result(rf1(0,0), rf2(0,0), reg.value)
    }

    val gold1 = Array.tabulate(N){i => i.to[I16] * i.to[I16]}.reduce{_+_}
    val tmp = Array.empty[I16](M)
    for (i <- 0 until M) {
      if (i == 0) tmp(0) = 0
      else tmp(i) = tmp(i-1)*i.to[I16] + i.to[I16]
    }
    val gold2 = tmp(M-1)
    val gold = Result(gold1, gold2, gold1)
    println(r"got ${getArg(x)}, wanted $gold")
    assert(gold == getArg(x))
  }
}
