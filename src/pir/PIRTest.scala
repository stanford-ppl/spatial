package pir

import argon.DSLTest

abstract class PIRTest extends DSLTest with PIRApp {
  override def runtimeArgs: Args = NoArgs

  object PLASMA extends Backend(
    name = "PIR",
    args = "",
    make = "",
    run  = ""
  ) {
    override def shouldRun: Boolean = true
  }

  def backends = Seq(PLASMA)
}
