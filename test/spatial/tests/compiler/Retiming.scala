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



@spatial class SystolishArray extends SpatialTest {

  def main(args: Array[String]): Void = {
    val a = ArgOut[Int]
    val b = ArgOut[Int]
    val c = ArgOut[Int]
    val d = ArgOut[Int]
    val e = ArgOut[Int]

    Accel {
      val A = Reg[Int](0)
      val B = Reg[Int](0)
      val C = Reg[Int](0)
      val D = Reg[Int](0)
      val E = Reg[Int](0)

      // Inspired by SHA1 with FullDelay of RegRead(E) being incorrect
      Foreach(5 by 1){i => 
        val temp = A + B + C + D + E + 1
        E := D; D := C; C := B; B := A; A := temp
      }

      a := A
      b := B
      c := C
      d := D
      e := E
    }

    val A_result = getArg(a)
    val B_result = getArg(b)
    val C_result = getArg(c)
    val D_result = getArg(d)
    val E_result = getArg(e)

    val A_gold = 16
    val B_gold = 8
    val C_gold = 4
    val D_gold = 2
    val E_gold = 1

    println(r"Got:    ${E_result} ${D_result} ${C_result} ${B_result} ${A_result}")
    println(r"Wanted: ${E_gold} ${D_gold} ${C_gold} ${B_gold} ${A_gold}")

    assert(A_result == A_gold && B_result == B_gold && C_result == C_gold && D_result == D_gold && E_result == E_gold)


  }
}


@spatial class UnrAccessCycleDetection extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Void = {
    val x = ArgIn[Int]
    setArg(x, 1)
    val w = DRAM[Int](32)
    Accel {
      // Inspired by SHA1 the last instance of this loop (not nested in an if statement) has incorrect II
      val W = SRAM[Int](32)
      val data = SRAM[Int](16)
      Foreach(16 by 1) { i => data(i) = i+1 } 

      if (x.value == 1) {
        'CORRECT.Foreach(32 by 1) { i =>
          W(i) = if (i < 16) {data(i)} else {W(i-3) + W(i-8) + W(i-14) + W(i-16)}
        }
      }

      Foreach(16 by 1) { i => data(i) = i } 

      'WRONG.Foreach(32 by 1) { i =>
        W(i) = if (i < 16) {data(i)} else {W(i-3) + W(i-8) + W(i-14) + W(i-16)}
      }

      w store W
    }

    printArray(getMem(w), "W: ")
    val gold = Array[Int](0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,23,27,31,42,49,56,70,80,97,117,133,163,192,217,270,314)
    printArray(gold, "G: ")
    assert(getMem(w) == gold)
  }
}


@spatial class FlatAffineAccess extends SpatialTest {

  override def runtimeArgs: Args = "256 16"

  def main(args: Array[String]): Unit = {
    
    val debug:scala.Boolean = false

    val x = args(0).to[Int]
    val y = args(1).to[Int]
    assert(x == 256 && y == 16)

    val size2 = ArgIn[Int]
    val oc = ArgIn[Int]

    setArg(size2, x)
    setArg(oc, y)

    val dram_1D = DRAM[Int](4096)
    val dram_2D = DRAM[Int](16,256)
    val dram_3D = DRAM[Int](16,16,16)

    Accel {
      val result_1D = SRAM[Int](4096)
      val result_2D = SRAM[Int](16,256)
      val result_3D = SRAM[Int](16,16,16)
      'LOOP1D.Foreach(0 until 16, 0 until 16, 0 until 16 par 4){(a,b,c) => 
        result_1D(c * size2 + a * oc + b) = a + b + c
      }
      'LOOP2D.Foreach(0 until 16, 0 until 16, 0 until 16 par 4){(a,b,c) => 
        result_2D(c, a * oc + b) = a + b + c
      }
      'LOOP3D.Foreach(0 until 16, 0 until 16, 0 until 16 par 4){(a,b,c) => 
        result_3D(c, a, b) = a + b + c
      }
      dram_1D store result_1D
      dram_2D store result_2D
      dram_3D store result_3D
    }

    val got_1D = getMem(dram_1D)
    val got_2D = getMatrix(dram_2D)
    val got_3D = getTensor3(dram_3D)

    val gold_2D = (0::16,0::256){(i,j) => i+(j/16).to[Int]+(j%16)}
    val gold_3D = (0::16,0::16,0::16){(i,j,k) => i+j+k}
    printArray(got_1D, "Got 1D:")
    printArray(gold_3D.flatten, "Gold 1D:")
    printMatrix(got_2D, "Got 2D:")
    printMatrix(gold_2D, "Gold 2D:")
    printTensor3(got_3D, "Got 3D:")
    printTensor3(gold_3D, "Gold 3D:")

    val cksum1 = got_1D == gold_3D.flatten 
    val cksum2 = got_2D == gold_2D 
    val cksum3 = got_3D == gold_3D
    val cksum = cksum1 & cksum2 & cksum3
    println(r"1D: $cksum1, 2D: $cksum2, 3D: $cksum3")
    println(r"PASS: ${cksum} (FlatAffineAccess)")
    assert(cksum)
  }
}
