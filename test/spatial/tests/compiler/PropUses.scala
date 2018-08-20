package spatial.tests.compiler

import argon.Block
import spatial.dsl._

@spatial class PropUses extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val x = ArgIn[I8]
    val n = args(0).to[I8]
    setArg(x,n)
    val y = ArgOut[Int]
    Accel {
      val numiter = x.value.to[I32] // RegRead -> FixToFix -> CounterNew -> Controller used to crash TransientCleanup
      y := Reduce(Reg[Int])(numiter by 1){i => i}{_+_}
    }
    val gold = Array.tabulate(args(0).to[Int]){i => i}.reduce{_+_}
    println(r"Want $gold, got ${getArg(y)}")
    assert(getArg(y) == gold)
  }

}
