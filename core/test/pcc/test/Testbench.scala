package pcc.test

import pcc.lang._
import org.scalatest.{FlatSpec, Matchers}

abstract class Testbench extends App {
  override protected val testbench = true
}


abstract class Tests extends FlatSpec with Matchers {
  type Fail = pcc.core.TestbenchFailure
  val args = Array("--vv")

  def test(x: App, args: Array[String] = args): Unit = x.main(args)
}
