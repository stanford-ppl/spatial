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

  case class ScopeBundle(impure: Vector[Impure], scope: Vector[Sym[_]], cache: Map[Op[_], Sym[_]])

  val bundleStack = scala.collection.mutable.ArrayBuffer[ScopeBundle]()

  /** Sets the current scope to the one described by bundle, and executes block in that context. Pops the scope
    * afterwards.
    *
    * @param block The block to be executed
    * @param bundle A tuple describing the current "state" of the engine.
    * @return The result of block
    */
  def WithScope[R](block: => R, bundle: ScopeBundle): (R, ScopeBundle) = {
    // saves existing bundle on top of stack
    val curr_bundle = saveBundle()
    bundleStack.append(curr_bundle)

    // uses passed bundle as context for evaluating block
    loadBundle(bundle)
    val result = block

    // capture effects of running block
    val ret_bundle = saveBundle()

    // restore old bundle
    val old_bundle = bundleStack.last
    loadBundle(old_bundle)

    // pop old bundle off of stack
    bundleStack.remove(bundleStack.size - 1)

    (result, ret_bundle)
  }

  def WithNewScope[R](block: => R, motion: Boolean): (R, ScopeBundle) = {
    val newBundle = ScopeBundle(Vector.empty, Vector.empty,
      // Empty the CSE cache in case code motion is disabled
      if (motion) cache else Map.empty
    )
    WithScope(block, newBundle)
  }

  private def loadBundle(bundle: ScopeBundle): Unit = {
    impure = bundle.impure
    scope = bundle.scope
    cache = bundle.cache
  }

  private def saveBundle(): ScopeBundle = {
    ScopeBundle(impure, scope, cache)
  }

  def init(): Unit = {
    loadBundle(ScopeBundle(Vector.empty, Vector.empty, Map.empty))
  }

  /** Graph Metadata */
  val globals: GlobalMetadata = new GlobalMetadata
  val scratchpad: ScratchpadMetadata = new ScratchpadMetadata

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
  def dseModelArgs: Seq[String] = app match {
    case test: DSLTest => test.dseModelArgs.cmds
    case _ => Nil
  }
  def finalModelArgs: Seq[String] = app match {
    case test: DSLTest => test.finalModelArgs.cmds
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
