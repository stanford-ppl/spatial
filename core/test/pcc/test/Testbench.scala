package pcc.test

import pcc.lang._
import org.scalatest.{FlatSpec, Matchers}
import pcc.compiler

abstract class Testbench extends compiler.App {
  override protected val testbench = true
}


abstract class Tests extends FlatSpec with Matchers {
  type Fail = pcc.core.TestbenchFailure
  val args = Array("--vv")

  def test(x: compiler.App, args: Array[String] = args): Unit = x.main(args)
}
