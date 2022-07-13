package spatial.tests.compiler.streaming

import spatial.dsl._


import spatial.metadata.memory._

@spatial class RegBuffering extends SpatialTest {
  val outerIters = 16
  val innerIters = 4

  override def compileArgs = "--max_cycles=1000 --vv"

  override def main(args: Array[String]) = {
    implicitly[argon.State].config.setV(3)
    val output = ArgOut[I32]
    Accel {
      val reg = Reg[I32](0)
      reg.buffer
      reg := 3
      Pipe.Foreach(0 until outerIters by 1) {
        outer =>
          'Producer.Foreach(0 until innerIters) {
            i =>
              reg := reg + i * outer
          }
          'Consumer.Foreach(0 until innerIters) {
            inner =>
              output.write(reg.value + inner, inner == 0)
              reg := 0
          }
      }
    }
    println(r"Recv: ${output.value}")
    assert(output.value == 90, r"Expected 90, received ${output.value}")
  }
}

class RegBufferingNoStream extends RegBuffering {
  override def compileArgs = "--nostreamify"
}

@spatial class DataDependentControl extends SpatialTest {
  override def compileArgs = "--max_cycles=1000"
  override def main(args: Array[String]) = {
    val output = ArgOut[I32]
    val output2 = ArgOut[I32]
    Accel {
      val accum1 = Reg[I32](0)
      accum1.explicitName = "accum1"
      val accum2 = Reg[I32](0)
      accum2.explicitName = "accum2"
      'Outer.Sequential.Foreach(4 until 8) {
        outer =>
          val innerIterReg = Reg[I32](0)
          'Producer.Sequential.Foreach(0 until outer) {
            inner =>
              innerIterReg := innerIterReg + inner
          }
          // innerIterReg = 6; 10; 15; 21
          'Consumer.Sequential.Foreach(0 until innerIterReg.value) {
            inner2 =>
              accum1 := accum1 + inner2
//              println(r"Accum1: $accum1, inner2: $inner2, reg: $innerIterReg")
          }
          // 15; 45; 105; 210

          'Consumer2.Sequential.Foreach(0 until innerIterReg.value) {
            inner3 =>
              accum2 := accum2 + inner3 * innerIterReg.value
//              println(r"Accum2: $accum2, inner3: $inner3, reg: $innerIterReg")
          }
          // 15*6; 45*10; 105 * 15; 21 * 210
      }
      output := accum1
      output2 := accum2
    }
    // output1 = 375
    // output2 = 6525
    println(r"Result: ${output.value}, Result2: ${output2.value}")
    assert(output.value == 375, r"Expected 375, got ${output.value}")
    assert(output2.value == 6525, r"Expected 6525, got ${output2.value}")
  }
}

class DataDependentControlNS extends DataDependentControl {
  override def compileArgs = "--nostreamify"
}
