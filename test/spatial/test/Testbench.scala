package spatial.test

import argon.DSLApp
import org.scalatest.{FlatSpec, Matchers}

abstract class Testbench extends FlatSpec with argon.Testbench with Matchers {
  private lazy val err = "ERROR.*Value '[0-9]+' is out of the range".r

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

  override val backends = Seq(
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
      args = "--synth --fpga Default",
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
}
