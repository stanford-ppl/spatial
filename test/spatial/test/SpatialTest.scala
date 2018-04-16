package spatial.test

import argon.DSLTest
import spatial.SpatialApp
import spatial.lang.{Bit, Text, Void}
import forge.SrcCtx

// Create a testbench which runs Scala tests
abstract class SpatialTest extends DSLTest with SpatialApp {
  private lazy val err = "ERROR.*Value '[0-9]+' is out of the range".r

  def assert(cond: Bit)(implicit ctx: SrcCtx): Void = spatial.dsl.assert(cond)
  def assert(cond: Bit, msg: Text)(implicit ctx: SrcCtx): Void = spatial.dsl.assert(cond, msg)

  class ChiselBackend(name: String, args: String, make: String, run: String)
    extends Backend(name,args,make,run) {

    override def parseMakeError(line: String): Result = {
      if (line.contains("Placer could not place all instances")) Error(line)
      else if (err.findFirstIn(line).isDefined) Error(line)
      else super.parseMakeError(line)
    }

    override def parseRunError(line: String): Result = {
      if (line.trim.endsWith("failed.")) Error(line)    // VCS assertion failure
      else super.parseRunError(line)
    }
  }

  def targets = Seq(
    new Backend(
      name = "Scala",
      args = "--sim",
      make = "make",
      run  = "bash run.sh"
    ) {
      override def parseRunError(line: String): Result = {
        if (line.trim.startsWith("at")) Error(prev)   // Scala exception
        else super.parseRunError(line)
      }
    },

    new ChiselBackend(
      name = "VCS",
      args = "--synth --fpga Zynq",
      make = "make vcs",
      run  = "bash scripts/regression_run.sh"
    ) {
      override val makeTimeout: Long = 13000
    },

    new ChiselBackend(
      name = "Zynq",
      args = "--synth --fpga Zynq",
      make = "make zynq",
      run  = "bash scripts/scrape.sh Zynq"
    ) {
      override val makeTimeout: Long = 13000
    },

    new ChiselBackend(
      name = "ZCU",
      args = "--synth",
      make = "make zcu",
      run  = "bash scripts/scrape.sh ZCU"
    ) {
      override val makeTimeout: Long = 13000
    },

    new ChiselBackend(
      name = "AWS",
      args = "--synth --fpga AWS_F1",
      make = "make aws-F1-afi",
      run  = "bash scripts/scrape.sh AWS"
    ) {
      override val makeTimeout: Long = 32400
    },

    new ChiselBackend(
      name = "Stats",
      args = "--synth",
      make = "make null",
      run  = "bash scripts/stats.sh"
    )
  )

  // TODO: Shouldn't try to compile multiple chisel targets if the app target is overridden
  override def backends: Seq[Backend] = {
    targets
  }

  protected def checkIR(block: argon.Block[_]): Result = Unknown

  final override def postprocess(block: argon.Block[_]): Unit = {
    import argon._
    import spatial.node.AssertIf

    val stms = block.nestedStms
    val hasAssert = stms.exists{case Op(_:AssertIf) => true; case _ => false }
    if (!hasAssert) throw Indeterminate
    checkIR(block)
  }

}
