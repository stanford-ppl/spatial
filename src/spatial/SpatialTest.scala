package spatial

import argon.DSLTest
import forge.SrcCtx
import spatial.lang.{Bit, Text, Void}

trait SpatialTest extends Spatial with DSLTest {
  /** By default, SpatialTests have no runtime arguments. Override to add list(s) of arguments. */
  override def runtimeArgs: Args = NoArgs


  abstract class ChiselBackend(name: String, args: String, make: String, run: String)
    extends Backend(name,args,make,run) {

    private lazy val err = "ERROR.*Value '[0-9]+' is out of the range".r

    override def parseMakeError(line: String): Result = {
      if (line.contains("Placer could not place all instances")) MakeError(line)
      else if (err.findFirstIn(line).isDefined) MakeError(line)
      else super.parseMakeError(line)
    }

    override def parseRunError(line: String): Result = {
      if (line.trim.endsWith("failed.")) RunError(line)    // VCS assertion failure
      else super.parseRunError(line)
    }
  }

  object Scala extends Backend(
    name = "Scala",
    args = "--sim",
    make = "make",
    run  = "bash run.sh"
  ) {
    def shouldRun: Boolean = checkFlag("test.Scala")
    override def parseRunError(line: String): Result = {
      if (line.trim.startsWith("at")) RunError(prev) // Scala exception
      else if (line.trim.contains("Assertion failure")) RunError(line) // Assertion failure
      else if (line.trim.contains("error")) RunError(line) // Runtime/compiler error
      else super.parseRunError(line)
    }
  }

  object VCS extends ChiselBackend(
    name = "VCS",
    args = "--synth --fpga Zynq --debugResources",
    make = "make vcs",
    run  = "bash scripts/regression_run.sh vcs"
  ) {
    override def shouldRun: Boolean = checkFlag("test.VCS")
    override val makeTimeout: Long = 3600
  }

  object VCS_noretime extends ChiselBackend(
    name = "VCS_noretime",
    args = "--synth --noretime --debugResources",
    make = "make vcs",
    run  = "bash scripts/regression_run.sh vcs-noretime"
  ) {
    override def shouldRun: Boolean = checkFlag("test.VCS_noretime")
    override val makeTimeout: Long = 3600
  }

  object Zynq extends ChiselBackend(
    name = "Zynq",
    args = "--synth --fpga Zynq --debugResources",
    make = "make zynq",
    run  = "bash scripts/scrape.sh Zynq"
  ) {
    override def shouldRun: Boolean = checkFlag("test.Zynq")
    override val makeTimeout: Long = 13000
  }

  object ZCU extends ChiselBackend(
    name = "ZCU",
    args = "--synth --fpga ZCU --debugResources",
    make = "make zcu",
    run  = "bash scripts/scrape.sh ZCU"
  ) {
    override def shouldRun: Boolean = checkFlag("test.ZCU")
    override val makeTimeout: Long = 13000
  }

  object AWS extends ChiselBackend(
    name = "AWS",
    args = "--synth --fpga AWS_F1 --debugResources",
    make = "make aws-F1-afi",
    run  = "bash scripts/scrape.sh AWS"
  ) {
    override def shouldRun: Boolean = checkFlag("test.AWS")
    override val makeTimeout: Long = 32400
  }

  object PIR extends Backend(
    name = "PIR",
    args = "--pir --dot",
    make = "",
    run  = "" 
  ) {
    override def shouldRun: Boolean = checkFlag("test.PIR")
    override def runBackend(): Unit = {
      s"${name}" should s"compile for backend PIR" in {
        val result = compile().foldLeft[Result](Unknown){ case (result, generate) =>
          result orElse generate()
        }
        result orElse Pass
      }
    }
  }


  class RequireErrors(errors: Int) extends IllegalExample("--sim", errors)
  object RequireErrors {
    def apply(n: Int): Seq[Backend] = Seq(new RequireErrors(n))
  }

  override def backends: Seq[Backend] = Seq(Scala, Zynq, ZCU, VCS, AWS, VCS_noretime, PIR)

}
