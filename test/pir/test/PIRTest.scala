package pir.test

import argon.DSLTest
import pir.PIRApp

abstract class PIRTest extends DSLTest with PIRApp {
  override def runtimeArgs: Args = NoArgs

  def backends = Seq(
    new Backend(
      name = "PIR",
      args = "",
      make = "",
      run  = ""
    )
  )
}
