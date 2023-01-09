package spatial.tests.feature.control


import spatial.dsl._

@spatial class SwitchCaseController extends SpatialTest {
  override def dseModelArgs: Args = "0 50" // Not sure about 2nd
  override def finalModelArgs: Args = "0 50" // Not sure about 2nd
  override def runtimeArgs: Args = "1 32"

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32)
    val x = ArgIn[Int]
    val y = ArgIn[Int]
//    val plzSetTo1 = args(0).to[Int]
//    val plzSetTo32 = args(1).to[Int]
    val plzSetTo1 = 1
    val plzSetTo32 = 32
    assert(plzSetTo1 == 1)
    assert(plzSetTo32 == 32)
    setArg(x, plzSetTo1)
    setArg(y, plzSetTo32)
    // Inspired by Sort_Radix
    Accel{
      val sram = SRAM[Int](64)
      Foreach(32 by 1){i => sram(i) = 0}
      Foreach(32 by 1) { blockID => 
        Sequential.Foreach(4 by 1) {i => 
          val indx = (blockID * 4 + i) * x // Make indx look random
          val reg = Reg[Int]
          reg := indx
          Foreach(x-1 by 1) { k => reg := reg.value >> 1}
          if (reg < y) {println(s"print something so it makes me an inner pipe??? $reg, ${sram(reg)}");sram(reg) = sram(reg) + 1}
        }
      }
      dram store sram

    }
    val allresult = getMem(dram)
    val result = Array.tabulate(32){i => allresult(i)}
    val gold = Array.tabulate(32){i => 1.to[Int]}
    printArray(result, "result")
    printArray(gold, "gold")
    assert(gold == result)
    println(r"Pass: ${gold == result}")
  }
}
