package spatial

import argon.DSLTest
import forge.SrcCtx
import spatial.lang.{Bit, Text, Void}

trait SpatialTest extends Spatial with DSLTest {
  private lazy val err = "ERROR.*Value '[0-9]+' is out of the range".r

  def assert(cond: Bit)(implicit ctx: SrcCtx): Void = spatial.dsl.assert(cond)
  def assert(cond: Bit, msg: Text)(implicit ctx: SrcCtx): Void = spatial.dsl.assert(cond, msg)

  abstract class ChiselBackend(name: String, args: String, make: String, run: String)
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

  object Scala extends Backend(
    name = "Scala",
    args = "--sim",
    make = "make",
    run  = "bash run.sh"
  ) {
    def shouldRun: Boolean = enable("test.Scala")
    override def parseRunError(line: String): Result = {
      if (line.trim.startsWith("at")) Error(prev)   // Scala exception
      else super.parseRunError(line)
    }
  }

  object VCS extends ChiselBackend(
    name = "VCS",
    args = "--synth --fpga Default",
    make = "make vcs",
    run  = "bash scripts/regression_run.sh"
  ) {
    override def shouldRun: Boolean = enable("test.VCS")
    override val makeTimeout: Long = 13000
  }

  object Zynq extends ChiselBackend(
    name = "Zynq",
    args = "--synth --fpga Zynq",
    make = "make zynq",
    run  = "bash scripts/scrape.sh Zynq"
  ) {
    override def shouldRun: Boolean = enable("test.Zynq")
    override val makeTimeout: Long = 13000
  }

  object ZCU extends ChiselBackend(
    name = "ZCU",
    args = "--synth",
    make = "make zcu",
    run  = "bash scripts/scrape.sh ZCU"
  ) {
    override def shouldRun: Boolean = enable("test.ZCU")
    override val makeTimeout: Long = 13000
  }

  object AWS extends ChiselBackend(
    name = "AWS",
    args = "--synth --fpga AWS_F1",
    make = "make aws-F1-afi",
    run  = "bash scripts/scrape.sh AWS"
  ) {
    override def shouldRun: Boolean = enable("test.AWS")
    override val makeTimeout: Long = 32400
  }

  override def backends: Seq[Backend] = Seq(Scala, Zynq, VCS, AWS)

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
