package spatial.tests.feature.control


import spatial.dsl._


@spatial class ReduceOptimized extends SpatialTest {
  override def runtimeArgs: Args = "100"

  type T = FixPt[TRUE,_28,_4]
  type T2 = FixPt[TRUE,_8,_0]

  def main(args: Array[String]): Unit = {
    val niter = args(0).to[Int]
    val out0 = ArgOut[T]
    val out1 = ArgOut[T]
    val out2 = ArgOut[T]
    val out3 = ArgOut[T2]
    val in = ArgIn[T]
    val N = ArgIn[Int]

    setArg(N, niter)
    setArg(in, 5.to[T])
    Accel {      
      Sequential.Foreach(N+1 by 1){ j => 
        val x = Reg[T](0)
        Reduce(x)(j by 1 par 3){ i =>
          i.to[T] * i.to[T] 
        }{_+_}
        Pipe{out0 := x.value}
      }

      Sequential.Foreach(N+1 by 1){ j => 
        val x = Reg[T2](0)
        Reduce(x)(j by 1){ i =>
          i.to[T2] * i.to[T2]
        }{_+_}
        Pipe{out3 := x.value}
      }
    }

    val gold0 = Array.tabulate[T](niter){i => (i*i).to[T]}.reduce{_+_}
    val gold3 = Array.tabulate[T2](niter){i => (i*i).to[T2]}.reduce{_+_}
    println(r"expected: $gold0 $gold3")
    println(r"result  : ${getArg(out0)} ${getArg(out3)}")

    val cksum = gold0 == getArg(out0) && gold3 == getArg(out3)
    println("PASS: " + cksum + " (ReduceOptimized)")
    assert(cksum)
  }
}