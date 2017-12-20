package pcc.test

import pcc.lang._

object StageForeach extends Testbench {
  def main(): Void = {
    (0::32){i => println("Hi") }
  }
}

class StagingTests extends Tests {
  "StageForeach" should "compile" in { test(StageForeach) }
}
