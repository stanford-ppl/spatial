package nova.test

import nova.lang._
import nova.compiler
import utest._

abstract class Test extends compiler.App {
  override protected val testbench = true
}


abstract class Testbench extends TestSuite {
  type Fail = nova.core.TestbenchFailure
  val defaultArgs = Array("-vv")

  def test(x: compiler.App, args: Array[String] = defaultArgs) = {
    x.main(args)
  }
}
