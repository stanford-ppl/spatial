package argon

import argon.passes.Pass

trait DSLApp extends Compiler {
  protected implicit class BlockOps[R](block: Block[R]) {
    def ==>(pass: Pass): Block[R] = runPass(pass, block)
    def ==>(pass: (Boolean,Pass)): Block[R] = if (pass._1) runPass(pass._2,block) else block
  }
  protected implicit class ConditionalPass(cond: Boolean) {
    def ?(pass: Pass): (Boolean, Pass) = (cond, pass)
  }

  def main(): Unit

  def stage(args: Array[String]): Block[_] = {
    val block = stageBlock{ main(); Invalid }
    block
  }
}
