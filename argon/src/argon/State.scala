package argon

import java.io.PrintStream

import utils.io.NullOutputStream

import scala.collection.mutable

case class BundleHandle(bundleID: Int)
object BundleHandle {
  val InvalidHandle = BundleHandle(-2)
}

class ScopeBundle (var scope: Vector[Sym[_]],     // Statements in the current scope
                   var impure: Vector[Impure],    // Effectful statements in the current scope
                   var cache: Map[Op[_],Sym[_]],  // Definition cache used for CSE
                   val handle: BundleHandle) {
  def copy(): ScopeBundle = {
    new ScopeBundle(scope, impure, cache, handle)
  }
}

private[argon] class ScopeBundleRegistry() {
  // TODO(stanfurd): Replace these with weakreferences so that we don't cause a memory leak.
  private val bundles = scala.collection.mutable.Map[BundleHandle, ScopeBundle]()
  private var nextHandle = -1

  private def GetNewHandle() = {
    nextHandle += 1
    BundleHandle(nextHandle)
  }

  def CreateNewBundle(impure: Vector[Impure], scope: Vector[Sym[_]], cache: Map[Op[_], Sym[_]]): ScopeBundle = {
    val newHandle = GetNewHandle()
    val newBundle = new ScopeBundle(scope, impure, cache, newHandle)
    bundles(newHandle) = newBundle
    newBundle
  }

  def GetBundle(bundleHandle: BundleHandle): Option[ScopeBundle] = {
    assert(bundleHandle != BundleHandle.InvalidHandle, "Attempting to get invalid handle!")
    bundles.get(bundleHandle)
  }

  def copyTo(target: ScopeBundleRegistry): Unit = {
    target.nextHandle = nextHandle
    target.bundles.clear()
    bundles foreach {
      case (handle, value) =>
        target.bundles(handle) = value.copy()
    }
  }
}

class State(val app: DSLRunnable) extends forge.AppState with Serializable {
  implicit val stateImplicit: argon.State = this
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

//  /** Statements in the current scope. */
//  var scope: Vector[Sym[_]] = _
//
//  /** Effectful statements in the current scope. */
//  var impure: Vector[Impure] = _
//
//  /** Definition cache used for CSE */
//  var cache: Map[Op[_], Sym[_]] = Map.empty

  var currentBundle: ScopeBundle = null

  var scopeBundleRegistry = new ScopeBundleRegistry

  def saveBundle(): Unit = {
    assert(currentBundle != null, "Attempting to save null bundle!")

    // clean out current bundle
    currentBundle = null
  }

  def setBundle(bundle: ScopeBundle): Unit = {
    assert(currentBundle == null,  "Attempting to set bundle on top of existing bundle! Save it first.")
    currentBundle = bundle
  }

  def scope: Vector[Sym[_]] = currentBundle.scope
  def scope_=(update: Vector[Sym[_]]): Unit = currentBundle.scope = update

  def impure: Vector[Impure] = currentBundle.impure
  def impure_=(update: Vector[Impure]): Unit = currentBundle.impure = update

  def cache: Map[Op[_],Sym[_]] = currentBundle.cache
  def cache_=(update: Map[Op[_], Sym[_]]): Unit = currentBundle.cache = update

  def GetBundle = scopeBundleRegistry.GetBundle _
  def GetCurrentHandle(): BundleHandle = currentBundle.handle

  /** Sets the current scope to the one described by bundle, and executes block in that context. Pops the scope
    * afterwards.
    *
    * @param handle A handle to the tuple describing the current "state" of the engine.
    * @param block The block to be executed
    * @return The result of block
    */
  def WithScope[R](handle: BundleHandle)(block: => R): R = {

    // saves existing bundle
    val savedBundle = currentBundle
    saveBundle()

    // uses passed bundle as context for evaluating block
    setBundle(scopeBundleRegistry.GetBundle(handle).get)
    val result = block
    saveBundle()

    setBundle(savedBundle)
    assert(savedBundle == scopeBundleRegistry.GetBundle(savedBundle.handle).get)
    result
  }

  def WithNewScope[R](motion: Boolean)(block: => R): (R, BundleHandle) = {
    val newBundle = scopeBundleRegistry.CreateNewBundle(Vector.empty, Vector.empty,
      // Empty the CSE cache in case code motion is disabled
      if (motion) currentBundle.cache else Map.empty
    )
    (WithScope(newBundle.handle) {block}, newBundle.handle)
  }

  private def resetBundles() = {
    currentBundle = null
    scopeBundleRegistry = new ScopeBundleRegistry
  }

  def init(): Unit = {
    setBundle(scopeBundleRegistry.CreateNewBundle(Vector.empty, Vector.empty, Map.empty))
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
    resetBundles()
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
    scopeBundleRegistry.copyTo(target.scopeBundleRegistry)
    target.currentBundle = scopeBundleRegistry.GetBundle(currentBundle.handle).get
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
