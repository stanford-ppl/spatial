package spatial.tests.feature.control


import spatial.dsl._


@spatial class ReduceSimple extends SpatialTest {
  override def runtimeArgs: Args = "72" and "7"

  val N = 96.to[Int]

  def simpleReduce[T:Num](xin: T): T = {
    val P = param(8)

    val x = ArgIn[T]
    val out = ArgOut[T]
    setArg(x, xin)

    Accel {
      out := Reduce(Reg[T](0.to[T]))(N by 1){ ii =>
        x.value * ii.to[T]
      }{_+_}
    }
    getArg(out)
  }


  def main(args: Array[String]): Unit = {
    val x = args(0).to[Int]

    val result = simpleReduce(x)

    val gold = Array.tabulate(N){i => x * i}.reduce{_+_}
    println("expected: " + gold)
    println("result:   " + result)

    val cksum = gold == result
    println("PASS: " + cksum + " (SimpleReduce)")
    assert(cksum)
  }
}