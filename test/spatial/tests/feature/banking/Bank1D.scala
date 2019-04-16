package spatial.tests.feature.banking

import spatial.dsl._

@spatial class Bank1D extends SpatialTest {
  override def dseModelArgs: Args = "16 16 16 16 16"
  override def finalModelArgs: Args = "16 16 16 16 16"
  override def runtimeArgs: Args = "16 7 2"

  val C = 64
  val tile = 16

  def main(args: Array[String]): Unit = {
    val LEN = ArgIn[Int]
    val START = ArgIn[Int]
    val PROBE = ArgIn[Int]
    val len = args(0).to[Int]
    val start = args(1).to[Int]
    val probe = args(2).to[Int]
    setArg(LEN,len)
    setArg(START,start)
    setArg(PROBE,probe)
    val dram = DRAM[Int](C)
    val data = Array.tabulate(C){i => i}
    setMem(dram, data)
    val RESULT = ArgOut[Int]

    Accel {
      val directW1 = SRAM[Int](tile)
      val directW2 = SRAM[Int](tile)
      val directW3 = SRAM[Int](tile)
      val directW4 = SRAM[Int](C)
      val directW5 = SRAM[Int](C)
      val xBarW1   = SRAM[Int](tile)
      val xBarW2   = SRAM[Int](C)
      directW1             load dram(0::tile par 8)
      directW2             load dram(16::16 + tile par 8)
      directW3             load dram(0::LEN par 8)     // Unaligned but local addr should start at const 0
      directW4             load dram(21::21+LEN par 8) // Unaligned but local addr should start at const 0
      directW5(21::21+LEN) load dram(21::21+LEN par 8) // Unaligned but local addr should start at const 21
      xBarW1               load dram(START::START + 16 par 8) // Cannot be direct bank
      xBarW2(START::START+LEN)   load dram(START::START + 16 par 8) // Cannot be direct bank

      RESULT := directW1(PROBE.value) + directW2(PROBE.value) + directW3(PROBE.value) + directW4(PROBE.value) + directW5(21 + PROBE.value) + xBarW1(PROBE.value) + xBarW2(START.value + PROBE.value)
    }

    val gold = data(probe) + data(probe + 16) + data(probe) + data(probe + 21) + data(probe + 21) + data(probe + start) + data(probe + start)
    val result = getArg(RESULT)
    println(r"got $result, wanted $gold")
    assert(result == gold)
  }
}

@spatial class PartialXBar extends SpatialTest {
  override def runtimeArgs: Args = "8 7"

  val tile = 32

  def main(args: Array[String]): Unit = {
    val test1 = ArgOut[Int]

    Accel {
      // 8-lane directW, 4-lane 2-bank xBarR
      val sram1 = SRAM[Int](tile)
      Foreach(tile by 1 par 8){i => sram1(i) = i}
      test1 := Reduce(Reg[Int])(tile by 1 par 4){i => sram1(i)}{_+_}

    }

    val result1 = getArg(test1)

    val gold1 = Array.tabulate(tile){i => i}.reduce{_+_}

    val cksum1 = result1 == gold1
    println(r"$cksum1: Test1 - $gold1 =?= $result1")
    assert(cksum1)
  }
}

@spatial class Cyclic1D extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val RESULT1 = ArgOut[Int]
    val RESULT2 = ArgOut[Int]

    Accel {
      val x = SRAM[Int](128)               // N = 9, B = 4, alpha = 3
      Foreach(128 by 1){i => x(i) = i}
      RESULT1 := Reduce(Reg[Int])(128 by 4 par 3){i => x(i+1) + x(i+2) + x(i+3)}{_+_}
      val y = SRAM[Int](128).noduplicate.noblockcyclic // N = 16, B = 1, alpha = 1
      Foreach(128 by 1){i => y(i) = i}
      RESULT2 := Reduce(Reg[Int])(128 by 4 par 3){i => y(i+1) + y(i+2) + y(i+3)}{_+_}
    }

    val gold = (0 to 127 by 4).map{i => i+1 + i+2 + i+3}.reduce{_+_}
    val result1 = getArg(RESULT1)
    val result2 = getArg(RESULT2)
    println(r"got $result1 and $result2, wanted $gold for both")
    assert(result1 == gold && result2 == gold)
  }
}
