package spatial.tests.feature.control


import spatial.dsl._


@spatial class ForeachSimple extends SpatialTest {
  override def runtimeArgs: Args = "16 16"

  def simpleSeq(xIn: Int, yIn: Int): Int = {
    val innerPar = 1 (1 -> 1)
    val tileSize = 96 (96 -> 96)

    val x = ArgIn[Int]
    val y = ArgIn[Int]
    val out = ArgOut[Int]
    setArg(x, xIn)
    setArg(y, yIn)
    Accel {
      val bram = SRAM[Int](tileSize)
      Foreach(tileSize by 1 par innerPar){ ii =>
        bram(ii) = x.value * ii
      }
      out := bram(y.value)
    }
    getArg(out)
  }


  def main(args: Array[String]): Unit = {
    val x = args(0).to[Int]
    val y = args(1).to[Int]
    val result = simpleSeq(x, y)

    val a1 = Array.tabulate(96){i => x * i}
    val gold = a1(y)

    println("expected: " + gold)
    println("result:   " + result)
    val chkSum = result == gold
    println("PASS: " + chkSum + " (SimpleSeq)")
    assert(chkSum)
  }
}
