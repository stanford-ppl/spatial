package nova.test

import spatial.dsl._
import utest._

abstract class Testbench extends TestSuite {
  type Fail = core.TestbenchFailure
  val defaultArgs = Array("-vv")

  def test(x: Test, args: Array[String] = defaultArgs): Unit = {
    x.main(args)
  }
}
