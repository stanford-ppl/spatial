package pcc.core

import pcc.util.files
import scopt.OptionParser

class Config {
  private val parser = new OptionParser[Unit]("pcc"){}
  parser.head("pcc", "Plasticine Configuration Compiler")
  parser.help("help").text("Prints this usage text.")

  /** Verbosity **/
  var enWarn: Boolean = true
  var enError: Boolean = true
  var enInfo: Boolean = true
  var enGen: Boolean = true
  var logLevel: Int = 0   // 0 - No logging, 1 - dbg only, 2 - all logging
  var exitOnBug: Boolean = true

  /**
    * Set the verbosity using a single numeric value
    * v < -1: All printing, including report printing, is disabled
    * v < 0:  Console printing is disabled
    * v < 1:  Basic logging is disabled
    * v < 2:  Verbose logging is disabled
    */
  def setVerbosity(v: Int): Unit = {
    enWarn = v >= 0
    enError = v >= 0
    enInfo = v >= 0
    enGen = v > -2
    logLevel = v
  }
  def enLog: Boolean = logLevel >= 2
  def enDbg: Boolean = logLevel >= 1

  parser.opt[Unit]('q', "quiet").action((_,_) => setVerbosity(0)).text("Disable background logging")
  parser.opt[Unit]("qq").action((_,_) => setVerbosity(-1)).text("Disable logging and console printing")
  parser.opt[Unit]('v', "verbose").action((_,_) => setVerbosity(1)).text("Enable basic background logging")
  parser.opt[Unit]("vv").action((_,_) => setVerbosity(2)).text("Enable verbose logging")


  /** Paths **/
  var name: String = "App"
  var logDir: String = files.cwd + files.sep + "logs" + files.sep + name + files.sep
  var genDir: String = files.cwd + files.sep + "gen" + files.sep + name + files.sep
  var repDir: String = files.cwd + files.sep + "reports" + files.sep + name + files.sep

  def create: Config = new Config

  def init(args: Array[String]): Unit = parser.parse(args,())

  def reset(): Unit = {
    enWarn = true
    enError = true
    enInfo = true
    enGen = true
    logLevel = 0
    exitOnBug = true
  }

  def copyTo(target: Config): Unit = {
    target.enWarn = this.enWarn
    target.enError = this.enError
    target.enGen = this.enGen
    target.logLevel = this.logLevel
    target.exitOnBug = this.exitOnBug
    target.name = this.name
    target.logDir = this.logDir
    target.genDir = this.genDir
    target.repDir = this.repDir
  }

}
