package nova.core

import forge.implicits.terminal._
import forge.io.files

class Config {

  /** Verbosity **/
  var enWarn: Boolean = true
  var enError: Boolean = true
  var enInfo: Boolean = true
  var enGen: Boolean = true
  var logLevel: Int = 0   // 0 - No logging, 1 - dbg only, 2 - all logging

  def withVerbosity[T](
    warn: Boolean = enWarn,
    error: Boolean = enError,
    info: Boolean = enInfo,
    gen: Boolean = enGen,
    log: Int = logLevel
  )(block: => T): T = {
    val saveWarn = enWarn; val saveError = enError; val saveInfo = enInfo; val saveGen = enGen; val saveLog = logLevel
    enWarn = warn; enError = error; enInfo = info; enGen = gen; logLevel = log
    val result = block
    enWarn = saveWarn; enError = saveError; enInfo = saveInfo; enGen = saveGen; logLevel = saveLog
    result
  }

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

  def parse(args: Array[String]): Unit = args.foreach{
    case "-q"  => setVerbosity(0)
    case "-qq" => setVerbosity(-1)
    case "-v"  => setVerbosity(1)
    case "-vv" => setVerbosity(2)
    case x => System.out.warn(s"Unrecognized argument $x")
  }

  val helpText: String =
    s"""
       |Verbosity:
       | -q       Disable background logging
       | -qq      Disable logging and console printing
       | -v       Enable basic background logging
       | -vv      Enable verbose logging
     """.stripMargin

  def help(): Unit = {
    System.out.println(helpText)
  }

  /** Paths **/
  var name: String = "App"
  var logDir: String = files.cwd + files.sep + "logs" + files.sep + name + files.sep
  var genDir: String = files.cwd + files.sep + "gen" + files.sep + name + files.sep
  var repDir: String = files.cwd + files.sep + "reports" + files.sep + name + files.sep

  /** Banking **/
  var enableBufferCoalescing: Boolean = true

  def create: Config = new Config

  def init(args: Array[String]): Unit = parse(args)

  def reset(): Unit = {
    enWarn = true
    enError = true
    enInfo = true
    enGen = true
    logLevel = 0
  }

  def copyTo(target: Config): Unit = {
    target.enWarn = this.enWarn
    target.enError = this.enError
    target.enGen = this.enGen
    target.logLevel = this.logLevel
    target.name = this.name
    target.logDir = this.logDir
    target.genDir = this.genDir
    target.repDir = this.repDir
  }

}
