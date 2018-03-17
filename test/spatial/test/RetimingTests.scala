package spatial.test

import nova.test.NovaTestbench
import spatial.dsl._
import utest._

@spatial object SimpleRetimePipe {

  def main(): Void = {
    val a = ArgIn[I32]
    val b = ArgIn[I32]
    val c = ArgIn[I32]
    val d = ArgOut[I32]
    Accel {
      d := a * b + c
    }
    println("d: " + getArg(d))
  }
}

@spatial object RetimeLoop {

  def main(): Void = {
    Accel {
      val x = Reg[I32]
      val sram = SRAM[I32](16, 16)
      x := Reduce(0)(0 until 16){i => i}{_+_}

      Foreach(0 until 16, 0 until 16){(i,j) =>
        sram(i,j) = ((i*j + 3) + x + 4) * 3
      }
    }
  }
}

@spatial object NestedPipeTest {

  def main(): Void = {
    // Declare SW-HW interface vals
    val x = ArgIn[I32]
    val y = ArgOut[I32]
    val N = 32

    // Connect SW vals to HW vals
    setArg(x, N)

    // Create HW accelerator
    Accel {
      Pipe(5 by 1) { i =>
        Pipe(10 by 1) { j =>
          Pipe {y := 3*(j + 4 + x + i)+x/4}
        }
      }
    }

    // Extract results from accelerator
    val result = getArg(y)

    // Create validation checks and debug code
    val gold = 3*(N + 4 + 4 + 9) + N / 4
    println("expected: " + gold)
    println("result: " + result)
  }
}


object RetimingTests extends Testbench { val tests = Tests {
  'SimpleRetimePipe - test(SimpleRetimePipe)
  'RetimeLoop - test(RetimeLoop)
  'NestedPipeTest - test(NestedPipeTest)
}}
