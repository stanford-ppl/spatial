package spatial

import argon.DSLTest
import forge.SrcCtx
import spatial.lang.{Bit, Text, Void}
import utils.io.files
import spatial.util.spatialConfig

trait SpatialTest extends Spatial with DSLTest { self =>
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
    run  = "bash scripts/regression_run.sh scalasim"
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
    args = "--synth --fpga VCS",
    make = "make",
    run  = "bash scripts/regression_run.sh vcs"
  ) {
    override def shouldRun: Boolean = checkFlag("test.VCS")
    override val makeTimeout: Long = 3600
  }

  object Zynq extends ChiselBackend(
    name = "Zynq",
    args = "--synth --fpga Zynq",
    make = "make",
    run  = "bash scripts/scrape.sh Zynq"
  ) {
    override def shouldRun: Boolean = checkFlag("test.Zynq")
    override val makeTimeout: Long = 13000
  }

  object ZCU extends ChiselBackend(
    name = "ZCU",
    args = "--synth --fpga ZCU",
    make = "make",
    run  = "bash scripts/scrape.sh ZCU"
  ) {
    override def shouldRun: Boolean = checkFlag("test.ZCU")
    override val makeTimeout: Long = 13000
  }

  object AWS extends ChiselBackend(
    name = "AWS",
    args = "--synth --fpga AWS_F1",
    make = "make aws-F1-afi",
    run  = "bash scripts/scrape.sh AWS"
  ) {
    override def shouldRun: Boolean = checkFlag("test.AWS")
    override val makeTimeout: Long = 32400
  }

  abstract class PIRBackEnd (
    name: String, 
    paramField:String=""
  )(
    args:String=
      s"--pir --dot " + 
      s"--load-param=${files.buildPath(DATA, "params", "pir", s"${self.name}.param:$paramField")} " +
      s"--save-param=${files.buildPath(spatialConfig.genDir, "pir", "saved.param")} ",
    make:String = "make",
    run:String = "bash run.sh"
  ) extends Backend(name, args, make, run) {
    override def shouldRun: Boolean = checkFlag(s"test.${name}")
  }

  object PIR extends PIRBackEnd (
    name="PIR"
  )(
    args = "--pir --dot --vv"
  )

  object PIRNoPar extends PIRBackEnd (
    name="PIRNoPar", 
    paramField="nopar"
  )()

  object PIRSmallPar extends PIRBackEnd (
    name="PIRSmallPar", 
    paramField="smallpar"
  )()

  object PIRBigPar extends PIRBackEnd (
    name="PIRBigPar", 
    paramField="bigpar"
  )()

  class RequireErrors(errors: Int) extends IllegalExample("--sim", errors)
  object RequireErrors {
    def apply(n: Int): Seq[Backend] = Seq(new RequireErrors(n))
  }

  override def backends: Seq[Backend] = Seq(Scala, Zynq, ZCU, VCS, AWS, PIR, PIRNoPar, PIRSmallPar, PIRBigPar)

}
