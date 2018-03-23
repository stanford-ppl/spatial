package spatial.test

import spatial.dsl._
import utest._

@spatial object SimpleParTest {
  def main(): Void = {
    Accel {
      val sram = SRAM[I32](64)
      Foreach(64 par 16){i => sram(i) = i + 1  }
      Foreach(64 par 16){i => println(sram(i)) }
    }
  }
}

@spatial object FixedOffsetTest {
  def main(): Void = {
    val x = ArgIn[I32]
    Accel {
      val sram = SRAM[I32](64)
      Foreach(64 par 16){i => sram(i + x.value) = i + x.value }
      Foreach(64 par 16){i => println(sram(i)) }
    }
  }
}

@spatial object RandomOffsetTest {
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

@spatial object RandomOffsetTestWrite {
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
