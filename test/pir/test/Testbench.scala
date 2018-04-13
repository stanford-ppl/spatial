package pir.test

import org.scalatest.{FlatSpec, Matchers}

abstract class Testbench extends FlatSpec with argon.Testbench with Matchers {
  val backends = Seq(
    new Backend(
      name = "PIR",
      args = "",
      make = "",
      run  = ""
    )
  )
}
