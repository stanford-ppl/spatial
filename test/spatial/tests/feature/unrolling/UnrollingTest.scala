package spatial.tests.feature.unrolling

import spatial.dsl._

@spatial class UnrollingTest extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val o = ArgOut[Int]

    Accel {
      Foreach(32*16 by 16 par 2){i =>
        val x = SRAM[Int](32)

        Pipe {
          println(x(16))
        }

      }

      o := 32
    }

    println(getArg(o))
    assert(o == 32)
  }

}


@spatial class SwitchCondReuse extends SpatialTest {
  override def runtimeArgs: Args = "1"

  def main(args: Array[String]): Unit = {
    val x = ArgIn[Int]
    val in = args(0).to[Int]
    setArg(x,in)
    val out = DRAM[Int](16)
    assert(in < 5)

    Accel {
      val sram = SRAM[Int](16)
      if (x.value < 5) {
        Sequential.Foreach(16 by 1 par 4){i => 
          Pipe{sram(i) = i}
          Pipe{sram(i) = i}
        }
      }
      out store sram
    }

    val gold = Array.tabulate(16){i => i}
    val got = getMem(out)
    printArray(gold, "Gold")
    printArray(got, "Got")
    assert(gold == got)
    println(r"PASS: ${gold == got}")
  }

}
