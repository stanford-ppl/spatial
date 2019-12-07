package spatial.tests.syntax

import spatial.dsl._

@spatial class VerilogSimpleBBox extends SpatialTest {
  override def runtimeArgs: Args = "3"

  @struct case class BBOX_IN(in1: Int, in2: Int)
  @struct case class BBOX_OUT(out: Int)
  @struct case class BBOX_DM_OUT(div: Int, mod: Int)

  def main(args: Array[String]): Unit = {
    val a = args(0).to[Int]
    val b = args(1).to[Int]
    val in1 = ArgIn[Int]
    val sum = DRAM[Int](16)
    val prod = DRAM[Int](16)
    val mod = DRAM[Int](16)
    val div = DRAM[Int](16)
    setArg(in1, a)

    Accel {
      val addsram = SRAM[Int](16)
      val mulsram = SRAM[Int](16)
      val divsram = SRAM[Int](16)
      val modsram = SRAM[Int](16)
      Foreach(16 by 1) { i =>
        addsram(i) = verilogBlackBox[BBOX_IN,BBOX_OUT](BBOX_IN(i, in1.value))(s"$DATA/verilogboxes/adder.v", latency = 3, pipelined = true, params = Map("LATENCY" -> 3)).out
        mulsram(i) = verilogBlackBox[BBOX_IN,BBOX_OUT](BBOX_IN(i, in1.value))(s"$DATA/verilogboxes/multiplier.v", latency = 8, pipelined = true, params = Map("LATENCY" -> 8)).out
        val dm = verilogBlackBox[BBOX_IN,BBOX_DM_OUT](BBOX_IN(i, in1.value))(s"$DATA/verilogboxes/divmodder.v", latency = 15, pipelined = true, params =  Map("LATENCY" -> 15))
        modsram(i) = dm.mod
        divsram(i) = dm.div
      }

      sum store addsram
      prod store mulsram
      mod store modsram
      div store divsram
    }

    printArray(getMem(sum), "got sum:" )
    val sumgold = Array.tabulate(16){i => i + a}
    printArray(sumgold, "wanted sum:")
    println(r"OK: ${sumgold == getMem(sum)}")
    assert(sumgold == getMem(sum))

    printArray(getMem(prod), "got prod:" )
    val prodgold = Array.tabulate(16){i => i * a}
    printArray(prodgold, "wanted prod:")
    println(r"OK: ${prodgold == getMem(prod)}")
    assert(prodgold == getMem(prod))

    printArray(getMem(mod), "got mod:" )
    val modgold = Array.tabulate(16){i => i % a}
    printArray(modgold, "wanted mod:")
    println(r"OK: ${modgold == getMem(mod)}")
    assert(modgold == getMem(mod))

    printArray(getMem(div), "got div:" )
    val divgold = Array.tabulate(16){i => i / a}
    printArray(divgold, "wanted div:")
    println(r"OK: ${divgold == getMem(div)}")
    assert(divgold == getMem(div))
  }

}
