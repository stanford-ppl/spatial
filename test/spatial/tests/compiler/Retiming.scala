package spatial.tests.compiler

import argon._

import spatial.dsl._
import spatial.node.DelayLine

@spatial class SimpleRetimePipe extends SpatialTest {
  override def backends = super.backends.filterNot{be => (be == Scala) | (be == VCS_noretime)}

  def main(args: Array[String]): Unit = {
    val a = ArgIn[Int]
    val b = ArgIn[Int]
    val c = ArgIn[Int]
    val d = ArgOut[Int]
    Accel {
      d := a * b + c
    }
    println("d: " + getArg(d))
    assert(getArg(d) == 0.to[Int])
  }

  override def checkIR(block: Block[_]): Result = {
    val delays = block.nestedStms.count{case Op(_:DelayLine[_]) => true; case _ => false }
    delays shouldBe 3
    super.checkIR(block)
  }
}

@spatial class SimplePipeRetime2 extends SpatialTest {

  def main(args: Array[String]): Unit = {
    // add one to avoid dividing by zero
    val a = random[Int](10) + 1
    val b = random[Int](10) + 1

    val aIn = ArgIn[Int]
    val bIn = ArgIn[Int]
    setArg(aIn, a)
    setArg(bIn, b)

    val out1 = ArgOut[Int]
    val out2 = ArgOut[Int]
    Accel {
      out1 := (aIn * bIn) + aIn
      out2 := (aIn / bIn) + aIn
    }
    val gold1 = (a * b) + a
    val gold2 = (a / b) + a
    val cksum = gold1 == getArg(out1) && gold2 == getArg(out2)
    assert(cksum)
  }
}


@spatial class RetimeLoop extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](16,16)

    Accel {
      val x = Reg[Int]
      val sram = SRAM[Int](16, 16)
      x := Reduce(0)(0 until 16){i => i}{_+_}

      Foreach(0 until 16, 0 until 16){(i,j) =>
        sram(i,j) = ((i*j + 3) + x + 4) * 3
      }

      dram store sram
    }

    val x = Array.tabulate(16){i => i}.reduce(_+_)
    val gold = (0::16,0::16){(i,j) => ((i*j + 3) + x + 4) * 3 }
    assert(getMatrix(dram) == gold)
  }
}

@spatial class RetimeNestedPipe extends SpatialTest {
  override def runtimeArgs: Args = "6"

  def main(args: Array[String]): Unit = {
    val x = ArgIn[Int]
    val y = ArgOut[Int]
    val N = args(0).to[Int]

    setArg(x, N)

    Accel {
      Foreach(5 by 1) { i =>
        Foreach(10 by 1) { j =>
          Pipe { y := 3*(j + 4 + x + i)+x/4 }
        }
      }
    }

    val result = getArg(y)
    val gold = 3*(N + 4 + 4 + 9) + N / 4
    println("expected: " + gold)
    println("result: " + result)
    assert(result == gold)
  }
}


@spatial class RetimeRandomTest extends SpatialTest {

  def main(args: Array[String]): Void = {
    val x = ArgOut[Bit]
    val nx = ArgOut[Bit]
    Accel {
      val y = random[Bit]
      x := !y
      nx := y
    }
    println(r"bit: $x")
    assert(getArg(x) != getArg(nx))
  }
}

@spatial class RetimeOffsetTest extends SpatialTest {
  override def backends = DISABLED // TODO: Rewrite

  def main(args: Array[String]): Void = {
    Accel {
      val sram = SRAM[I32](64)
      val reg = Reg[I32](0)
      Foreach(64 par 2){i =>
        sram(i + reg.value) = i
        reg := (reg.value+1)*5
      }
      Foreach(64 par 16){i => println(sram(i)) }
    }
  }
}


