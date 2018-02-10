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

object BankingTests extends Testbench { val tests = Tests{
  'SimpleParTest - test(SimpleParTest)
}}
