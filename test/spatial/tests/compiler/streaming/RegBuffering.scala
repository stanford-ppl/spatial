package spatial.tests.compiler.streaming

import spatial.dsl._


import spatial.metadata.memory._

@spatial class RegBuffering extends SpatialTest {
  val outerIters = 16
  val innerIters = 4

  override def compileArgs = "--max_cycles=1000"

  override def main(args: Array[String]) = {
    implicitly[argon.State].config.setV(3)
    val output = ArgOut[I32]
    Accel {
      val reg = Reg[I32](0)
      reg.buffer
      reg := 3
      Foreach(0 until outerIters by 1) {
        outer =>
          'Producer.Foreach(0 until innerIters) {
            i =>
              reg := reg + i * outer
          }
          'Consumer.Foreach(0 until innerIters) {
            inner =>
              output.write(reg.value + inner, inner == I32(0))
              reg := 0
          }
      }
    }
    println(r"Recv: ${output.value}")
    val gold = (0 to innerIters - 1).map(_ * (outerIters - 1)).sum
    assert(output.value == I32(gold), r"Expected $gold, received ${output.value}")
  }
}

class RegBufferingNoStream extends RegBuffering {
  override def compileArgs = "--nostreamify"
}

@spatial class RegSimple extends SpatialTest {

  override def compileArgs = "--max_cycles=1000"

  override def main(args: Array[String]) = {
    //    implicitly[argon.State].config.setV(3)
    val output = ArgOut[I32]
    Accel {
      val reg = Reg[I32](0)
      reg.buffer
      Pipe {
        reg := 3
      }

      Pipe {
        output := reg.value
      }
    }
    println(r"Recv: ${output.value}")
    assert(output.value == 3, r"Expected 90, received ${output.value}")
  }
}

@spatial class DataDependentControl extends SpatialTest {
  override def compileArgs = "--max_cycles=10000 --vv"
  override def main(args: Array[String]) = {
    implicitly[argon.State].config.setV(3)
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
          innerIterReg.explicitName = "innerIterReg"
          'Producer.Foreach(0 until outer) {
            inner =>
              innerIterReg := innerIterReg + inner
          }
          // innerIterReg = 6; 10; 15; 21
          'Consumer.Foreach(0 until innerIterReg.value) {
            inner2 =>
              accum1 := accum1 + inner2
          }
          // 15; 45; 105; 210

          'Consumer2.Foreach(0 until innerIterReg.value) {
            inner3 =>
              accum2 := accum2 + inner3 * innerIterReg.value
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

@spatial class DynamicZeroIterForeach extends SpatialTest {
  override def compileArgs = "--max_cycles=500 --vv"

  override def main(args: Array[String]) = {
    val output = ArgOut[I32]
    Accel {
      val accum1 = Reg[I32](0)
      accum1.explicitName = "accum1"
      'Outer.Foreach(0 until 4) {
        outer =>
          val innerIterReg = Reg[I32](0)
          innerIterReg.explicitName = "innerIterReg"
          'Producer.Foreach(0 until outer) {
            inner =>
              innerIterReg := innerIterReg + inner
          }
          'Consumer.Foreach(0 until innerIterReg.value) {
            inner2 =>
              accum1 := accum1 + inner2
          }
      }
      output := accum1
    }
    assert(checkGold(output, I32(3)))
  }
}
