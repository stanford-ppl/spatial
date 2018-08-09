package spatial.tests.feature.retiming

import spatial.dsl._

@spatial class RetimeNestedPipe extends SpatialTest {
  override def runtimeArgs: Args = "6"

  def main(args: Array[String]): Unit = {
    val x = ArgIn[Int]
    val y = ArgOut[Int]
    val N = args(0).to[Int]

    setArg(x, N)

    Accel {
      Foreach(5 by 1) { i =>
        Foreach(10 by 1) { j =>
          Pipe { y := 3*(j + 4 + x + i)+x/4 }
        }
      }
    }

    val result = getArg(y)
    val gold = 3*(N + 4 + 4 + 9) + N / 4
    println("expected: " + gold)
    println("result: " + result)
    assert(result == gold)
  }
}