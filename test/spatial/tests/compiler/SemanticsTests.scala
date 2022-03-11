package spatial.tests.compiler

import spatial.dsl._

@spatial class RegisterWriteRead extends SpatialTest {
//  override def compileArgs = super.compileArgs + "--nostreamify"

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
    val outputTensor = getMem(output)
    Foreach(iters by 1) {
      i =>
        assert(outputTensor(i) == i + 5, s"Error at location $i -- received ${outputTensor(i)}, expected ${i + 5}")
    }
    printArray(outputTensor)
    assert(Bit(true))
  }
}