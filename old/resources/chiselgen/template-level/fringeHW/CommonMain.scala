// See LICENSE for license details.

package fringe

import chisel3.Module
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


abstract class ArgsTester[+T <: Module](c: T)(implicit args: Array[String]) extends PeekPokeTester(c) {
  def printFail(msg: String) = println(Console.BLACK + Console.RED_B + s"FAIL: $msg" + Console.RESET)
  def printPass(msg: String) = println(Console.BLACK + Console.GREEN_B + s"PASS: $msg" + Console.RESET)
}

trait CommonMain {
  /**
   * 'args' variable that holds commandline arguments
   * TODO: Is using a var the best way to handle this?
   */
  implicit var args: Array[String] = _
  case class SplitArgs(chiselArgs: Array[String], testArgs: Array[String])

  type DUTType <: Module
  def dut: () => DUTType
  def tester: DUTType => ArgsTester[DUTType]

  def supportedTarget(t: String) = t match {
    case "aws"       => true
    case "aws-sim"   => true
    case "zynq"      => true
    case "zcu"       => true
    case "verilator" => true
    case "vcs"       => true
    case "xsim"      => true
    case "de1soc"    => true
    case "arria10"   => true
    case "asic"      => true
    case _           => false
  }

  def target = if (args.size > 0) args(0) else "verilator"

  def separateChiselArgs(args: Array[String]) = {
    val argSeparator = "--testArgs"
    val (chiselArgs, otherArgs) = if (args.contains("--testArgs")) {
      args.splitAt(args.indexOf("--testArgs"))
    } else {
      (args, Array[String]())
    }
    val actualChiselArgs = if (chiselArgs.size == 0) Array("--help") else chiselArgs
    val testArgs = otherArgs.drop(1)
    SplitArgs(actualChiselArgs, testArgs)
  }

  def main(args: Array[String]) {
    val splitArgs = separateChiselArgs(args)
    this.args = splitArgs.testArgs

    Predef.assert(supportedTarget(target), s"ERROR: Unsupported Fringe target '$target'")

    if (splitArgs.chiselArgs.contains("--test-command")) {
      val cmd = splitArgs.chiselArgs(splitArgs.chiselArgs.indexOf("--test-command")+1)
      Driver.run(dut, cmd)(tester)
    } else if (splitArgs.chiselArgs.contains("--verilog")) {
      chisel3.Driver.execute(Array[String]("--target-dir", s"verilog-${target}"), dut)
    } else {
      Driver.execute(splitArgs.chiselArgs, dut)(tester)
    }
  }
}
