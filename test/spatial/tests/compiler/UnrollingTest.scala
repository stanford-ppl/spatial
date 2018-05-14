package spatial.tests.compiler

import spatial.dsl._

@test class UnrollingTest extends SpatialTest {
  def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {

    val y = DRAM[Int](32*16)
    val o = ArgOut[Int]

    Accel {
      Foreach(32*16 by 16 par 2){i =>
        val x = SRAM[Int](32)

        Foreach(0 until 16 par 4){j =>
          x(j) = i + j + 1
        }

        y(i::i+16) store x

      }

      o := 32
    }

    println(getArg(o))

    val gold = Array.tabulate(32*16){i => i + 1 }
    assert(getMem(y) == gold)

  }

}
