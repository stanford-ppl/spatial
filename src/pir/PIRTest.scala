package pir

import argon.DSLTest

trait PIRTest extends PIR with DSLTest {
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
