package spatial.tests.compiler

import spatial.dsl._

@spatial class RegisterWriteRead extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val iters = 32
    val output = DRAM[Int](iters)
    val arg = ArgIn[Int]
    setArg(arg, 5)
    Accel {
      val sram = SRAM[Int](iters)
      Pipe.Foreach(iters by 1) {
        i =>
          val v = Reg[Int]
          v := arg + i
          sram(i) = v
      }
      output store sram
    }
    printArray(getMem(output))
    assert(Bit(true))
  }
}