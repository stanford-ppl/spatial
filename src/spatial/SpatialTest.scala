package spatial

import argon.DSLTest
import forge.SrcCtx
import spatial.lang.{Bit, Text, Void}
import utils.io.files
import spatial.util.spatialConfig

trait SpatialTest extends Spatial with DSLTest with PlasticineTest { self =>
  /** By default, SpatialTests have no runtime arguments. Override to add list(s) of arguments. */
  override def runtimeArgs: Args = NoArgs
  override def dseModelArgs: Args = NoArgs
  override def finalModelArgs: Args = NoArgs


  abstract class ChiselBackend(name: String, args: String, make: String, run: String, model: String, shouldRunModels: Boolean = false)
    extends Backend(name,args,make,run,model,shouldRunModels) {

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
    args = "--sim --dot",
    make = "make",
    run  = "bash scripts/regression_run.sh scalasim",
    model = "noninteractive"
  ) {
    override def shouldRun: Boolean = checkFlag("test.Scala")
    override def parseRunError(line: String): Result = {
      if (line.trim.startsWith("at")) RunError(prev) // Scala exception
      else if (line.trim.contains("Assertion failure")) RunError(line) // Assertion failure
      else if (line.trim.contains("error")) RunError(line) // Runtime/compiler error
      else super.parseRunError(line)
    }
  }

  object VCS extends ChiselBackend(
    name = "VCS",
    args = "--synth --insanity --instrument --runtime --fpga VCS",
    make = {
      val vcd = checkFlag("VCD")
      println(s"Run VCD: ${vcd}")
      if (vcd) {
        "make -e VCD_ON=1"
      } else {
        "make -e VCD_OFF=1"
      }
    },
    run  = "bash scripts/regression_run.sh vcs",
    model  = "bash scripts/model_run.sh vcs",
    shouldRunModels = true
  ) {
    override def shouldRun: Boolean = checkFlag("test.VCS")
    override val makeTimeout: Long = 130000
  }

  object VCSTest extends ChiselBackend(
    name = "VCSTest",
    args = "--synth --instrument --runtime --fpga VCS",
    make = "make",
    run  = "",
    model  = ""
  ) {
    override def shouldRun: Boolean = checkFlag("test.VCSTest")
    override val makeTimeout: Long = 130000
  }

  object Zynq extends ChiselBackend(
    name = "Zynq",
    args = "--synth --insanity --fpga Zynq",
    make = "make",
    run  = "bash scripts/scrape.sh Zynq",
    model = "noninteractive"
  ) {
    override def shouldRun: Boolean = checkFlag("test.Zynq")
    override val makeTimeout: Long = 13000
  }

  object ZCU extends ChiselBackend(
    name = "ZCU",
    args = "--synth --insanity --fpga ZCU",
    make = "make",
    run  = "bash scripts/scrape.sh ZCU",
    model = "noninteractive"
  ) {
    override def shouldRun: Boolean = checkFlag("test.ZCU")
    override val makeTimeout: Long = 130000
  }

  object ZCUSynth extends ChiselBackend(
    name = "ZCUS",
    args = ZCU.args,
    make = ZCU.make,
    run = "bash scripts/scrape.sh ZCUS",
    model = ZCU.model
  ) {
    override def shouldRun: Boolean = checkFlag("test.ZCUS")
    override val makeTimeout: Long = ZCU.makeTimeout
  }

  object AWS extends ChiselBackend(
    name = "AWS",
    args = "--synth --insanity --fpga AWS_F1",
    make = "make aws-F1-afi",
    run  = "bash scripts/scrape.sh AWS",
    model = "noninteractive"
  ) {
    override def shouldRun: Boolean = checkFlag("test.AWS")
    override val makeTimeout: Long = 32400
  }

  object CXP extends ChiselBackend(
    name = "CXP",
    args = "--synth --insanity --fpga CXP",
    make = "make",
    run  = "",
    model = "noninteractive"
  ) {
    override def shouldRun: Boolean = checkFlag("test.CXP")
    override val makeTimeout: Long = 32400
  }

  class RequireErrors(errors: Int) extends IllegalExample("--sim", errors)
  object RequireErrors {
    def apply(n: Int): Seq[Backend] = Seq(new RequireErrors(n))
  }

  override def backends: Seq[Backend] = Seq(Scala, Zynq, ZCU, VCS, AWS, CXP, ZCUSynth, VCSTest) ++ super.backends

}
