package spatial.tests.feature.control


import spatial.dsl._


@spatial class ReduceReverse extends SpatialTest {
  override def runtimeArgs: Args = "72" and "7"

  val N = 16.to[Int]

  def simpleReduce[T:Num](xin: T): T = {

    val x = ArgIn[T]
    val out = ArgOut[T]
    setArg(x, xin)

    Accel {
      out := Reduce(Reg[T](0.to[T]))(-N until 0 by 1){ ii =>
        x.value * ii.to[T]
      }{_+_}
    }
    getArg(out)
  }


  def main(args: Array[String]): Unit = {
    val x = args(0).to[Int]

    val result = simpleReduce(x)

    val gold = Array.tabulate(N){i => x * (i-N)}.reduce{_+_}
    println("expected: " + gold)
    println("result:   " + result)

    assert(gold == result)
  }
}

