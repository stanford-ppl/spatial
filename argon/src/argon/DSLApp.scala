package argon

import argon.passes.Pass

trait DSLApp extends Compiler {
  val stageArgs: String = ""
  val testArgs: String = ""

  protected implicit class BlockOps[R](block: Block[R]) {
    def ==>(pass: Pass): Block[R] = runPass(pass, block)
    def ==>(pass: (Boolean,Pass)): Block[R] = if (pass._1) runPass(pass._2,block) else block
  }
  protected implicit class ConditionalPass(cond: Boolean) {
    def ?(pass: Pass): (Boolean, Pass) = (cond, pass)
  }
}
