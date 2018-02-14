package pcc.test

import pcc.lang._
import utest._

object SimpleParTest extends Test {
  def main(): Void = {
    Accel {
      val sram = SRAM[I32](64)
      Foreach(64 par 16){i => sram(i) = i + 1  }
      Foreach(64 par 16){i => println(sram(i)) }
    }
  }
}

object FixedOffsetTest extends Test {
  def main(): Void = {
    val x = ArgIn[I32]
    Accel {
      val sram = SRAM[I32](64)
      Foreach(64 par 16){i => sram(i + x.value) = i + x.value }
      Foreach(64 par 16){i => println(sram(i)) }
    }
  }
}

object RandomOffsetTest extends Test {
  def main(): Void = {
    Accel {
      val sram = SRAM[I32](64)
      val reg = Reg[I32](0)
      Foreach(64 par 16){i => sram(i) = i }
      Foreach(64 par 16){i =>
        val x = sram(i + reg.value)
        reg := x
        println(x)
      }
    }
  }
}

object RandomOffsetTestWrite extends Test {
  def main(): Void = {
    Accel {
      val sram = SRAM[I32](64)
      val reg = Reg[I32](0)
      Foreach(64 par 16){i =>
        sram(i + reg.value) = i
        reg := (reg.value+1)*5
      }
      Foreach(64 par 16){i => println(sram(i)) }
    }
  }
}

object BankingTests extends Testbench { val tests = Tests{
  'SimpleParTest - test(SimpleParTest)
  'FixedOffsetTest - test(FixedOffsetTest)
  'RandomOffsetTest - test(RandomOffsetTest)
  'RandomOffsetTestWrite - test(RandomOffsetTestWrite)
}}
