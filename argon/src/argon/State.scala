package argon

import java.io.PrintStream

import utils.io.NullOutputStream

import scala.collection.mutable

class State(val app: DSLRunnable) extends forge.AppState with Serializable {
  /** Config */
  var config: Config = _

  /** Code Motion */
  private var _motionAllowed: Boolean = false
  def mayMotion: Boolean = _motionAllowed
  def enableMotion(): Unit = { _motionAllowed = true }

  /** Rewrite rules **/
  val rewrites = new Rewrites

  /** Flow rules **/
  val flows = new Flows

  /** Symbol IDs */
  private var id: Int = -1
  def maxId: Int = id-1 // Inclusive
  def nextId(): Int = { id += 1; id }

  /** Statements in the current scope. */
  var scope: Vector[Sym[_]] = _

  /** Effectful statements in the current scope. */
  var impure: Vector[Impure] = _

  /** Definition cache used for CSE */
  var cache: Map[Op[_], Sym[_]] = Map.empty

  /** Set the scope to be empty. Should not mutate current value.
    * NOTE: Should save and restore the scope surrounding this call.
    */
  def newScope(motion: Boolean): Unit = {
    scope = Vector.empty
    impure = Vector.empty
    if (!motion) cache = Map.empty // Empty the CSE cache in case code motion is disabled
  }

  /** Graph Metadata */
  val globals: GlobalMetadata = new GlobalMetadata

  /** Compiler passes */
  var pass: Int = 0
  def paddedPass: String = paddedPass(pass)
  def paddedPass(pass: Int): String = { val p = pass.toString; "0"*(4 - p.length) + p }
  def isStaging: Boolean = pass == 0

  /** Logging / Streams */
  var logTab: Int = 0
  var genTabs = new mutable.HashMap[PrintStream, Int]
  var out: PrintStream = Console.out
  var log: PrintStream = new PrintStream(new NullOutputStream)
  var gen: PrintStream = new PrintStream(new NullOutputStream)
  val streams = new mutable.HashMap[String, PrintStream]
  def streamName: String = streams.map(_.swap).apply(gen)
  def incGenTab(): Unit = { genTabs(gen) = genTabs(gen) + 1 }
  def decGenTab(): Unit = { genTabs(gen) = genTabs(gen) - 1 }
  def getGenTab: Int = { if (genTabs.contains(gen)) genTabs(gen) else {genTabs += gen -> 0; 0} }

  /** Infos */
  var infos: Int = 0
  def hadInfos: Boolean = infos > 0
  def logInfo(): Unit = { infos += 1 }

  /** Warnings */
  var warnings: Int = 0
  def hadWarnings: Boolean = warnings > 0
  def logWarning(): Unit = { warnings += 1 }

  /** Errors */
  var errors: Int = 0
  def hadErrors: Boolean = errors > 0
  def logError(): Unit = { errors += 1 }

  /** Bugs */
  var bugs: Int = 0
  def hadBugs: Boolean = bugs > 0
  def logBug(): Unit = { bugs += 1 }

  /** Back-edges */
  var issues: Set[Issue] = Set.empty
  def hasIssues: Boolean = issues.nonEmpty

  def runtimeArgs: Seq[String] = app match {
    case test: DSLTest => test.runtimeArgs.cmds
    case _ => Nil
  }

  def resetErrors(): Unit = errors = 0
  def reset(): Unit = {
    config.reset()
    id = -1
    scope = null
    impure = null
    cache = Map.empty
    globals.reset()
    pass = 1
    logTab = 0
    genTabs.clear()
    log = new PrintStream(new NullOutputStream)
    gen = new PrintStream(new NullOutputStream)
    streams.clear()
    infos = 0
    warnings = 0
    errors = 0
    bugs = 0
    issues = Set.empty
  }

  def copyTo(target: State): Unit = {
    this.config.copyTo(target.config)
    target.id = this.id
    target.scope = this.scope
    target.impure = this.impure
    target.cache = this.cache
    globals.copyTo(target.globals)
    target.pass = this.pass
    target.logTab = this.logTab
    target.genTabs = this.genTabs
    target.log = this.log
    target.gen = this.gen
    target.streams ++= this.streams
    target.infos = this.infos
    target.warnings = this.warnings
    target.errors = this.errors
    target.bugs = this.bugs
  }
}
