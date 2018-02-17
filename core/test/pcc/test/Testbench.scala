package pcc.test

import pcc.lang._
import pcc.compiler
import utest._

abstract class Test extends compiler.App {
  override protected val testbench = true
}


abstract class Testbench extends TestSuite {
  type Fail = pcc.core.TestbenchFailure
  val defaultArgs = Array("-vv")

  def test(x: compiler.App, args: Array[String] = defaultArgs) = {
    x.main(args)
  }
}
