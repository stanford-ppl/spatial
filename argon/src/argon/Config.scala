package argon

import utils.io.files

class Config {
  /** Verbosity */
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
  def setV(v: Int): Unit = {
    enWarn = v >= 0
    enError = v >= 0
    enInfo = v >= 0
    enGen = v > -2
    logLevel = v
  }
  def enLog: Boolean = logLevel >= 2
  def enDbg: Boolean = logLevel >= 1


  /** Staging */
  var enableAtomicWrites: Boolean = true
  var enableMutableAliases: Boolean = false

  /** Paths */
  var cwd: String = new java.io.File(".").getAbsolutePath

  var name: String = "App"
  var logDir: String = files.cwd + files.sep + "logs"
  var genDir: String = files.cwd + files.sep + "gen"
  var genDirOverride: Boolean = false
  var repDir: String = files.cwd + files.sep + "reports"

  /** Testing */
  var stop: Int = -1
  var test: Boolean = false

  /** Enable memory usage logging */
  var memlog: Boolean = false
  def enMemLog: Boolean = enLog || memlog

  /** Codegen */
  var naming: Boolean = true

  /** Host Codegen */
  var max_cycles: Long = 10000000000L


  def create: Config = new Config

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
