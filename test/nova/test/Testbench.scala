package nova.test

import nova.compiler.DSLApp
import utils.implicits.Readable._
import utest._

abstract class Testbench extends TestSuite {
  type Fail = core.TestbenchFailure

  def test(x: Any, args: Array[String] = Array("--vv", "-t")): Unit = x match {
    case x: DSLApp => x.main(args)
    case _ => throw new Exception(r"Don't know how to run test for ${x.getClass}")
  }
}
