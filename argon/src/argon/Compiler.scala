package argon

import argon.passes.Pass
import argon.transform.Transformer
import utils.io.files
import utils._
import utils.implicits.terminal._

trait Compiler { self =>
  protected[argon] var IR: State = new State
  final protected implicit def __IR: State = IR
  private val instrument = new Instrument()
  private val memWatch = new MemoryLogger()

  val script: String
  val desc: String
  def name: String = self.getClass.getName.replace("class ", "").replace('.','_').replace("$","")

  var directives: Map[String,String] = Map.empty
  def define[T](name: String, default: T)(implicit ctx: SrcCtx): T = directives.get(name.toLowerCase) match {
    case Some(arg) =>
      try {
        (default match {
          case _: Boolean => arg.toBoolean
          case _: Char  => arg.head
          case _: Byte  => arg.toByte
          case _: Short => arg.toShort
          case _: Int   => arg.toInt
          case _: Long  => arg.toLong
          case _: Float => arg.toFloat
          case _: Double => arg.toDouble
          case _: String => arg
          case _ =>
            error(ctx, s"Don't know how to get flag of type ${default.getClass}")
            error(ctx)
            default
        }).asInstanceOf[T]
      }
      catch {case _: Throwable =>
        error(ctx, s"Could not parse flag $name value $arg")
        error(ctx)
        default
      }
    case None => default
  }

  protected def checkBugs(stage: String): Unit = if (IR.hadBugs) throw CompilerBugs(stage, IR.bugs)
  protected def checkErrors(stage: String): Unit = if (IR.hadErrors) throw CompilerErrors(stage, IR.errors)
  protected def checkWarnings(): Unit = if (IR.hadWarnings) {
    warn(s"""${IR.warnings} ${plural(IR.warnings, "warning","warnings")} found""")
  }

  protected def onException(t: Throwable): Unit = {
    if (state == null) throw t
    if (config == null) throw t

    val trace = t.getStackTrace
    val oldLL = config.logLevel
    config.logLevel = 2
    withLog(config.logDir, config.name + "_exception.log") {
      if (t.getMessage != null) { log(t.getMessage); log("") }
      if (t.getCause != null) { log(t.getCause); log("") }
      trace.foreach{elem => log(elem.toString) }
    }
    config.logLevel = oldLL
    bug(s"An exception was encountered while compiling ${config.name}: ")
    if (t.getMessage != null) bug(s"  ${t.getMessage}")
    if (t.getCause != null) bug(s"  ${t.getCause}")
    else bug(s"  $t")
    if (config.enDbg) {
      trace.take(20).foreach{t => bug("  " + t) }
      if (trace.length > 20) bug(s" .. [see ${config.logDir}${config.name}_exception.log]")
    }
    bug(s"This is due to a compiler bug. A log file has been created at: ")
    bug(s"  ${config.logDir}${config.name}_exception.log")
  }

  def stageApp(args: Array[String]): Block[_]

  def runPasses[R](b: Block[R]): Block[R]



  final def runPass[R](t: Pass, block: Block[R]): Block[R] = instrument(t.name){
    if (t.isInstanceOf[Transformer]) {
      globals.clearBeforeTransform()
    }
    val issuesBefore = state.issues

    if (config.enMemLog) memWatch.note(t.name)
    if (config.enLog) info(s"Pass: ${t.name}")
    if (t.needsInit) t.init()
    val result = t.run(block)
    // After each traversal, check whether there were any reported errors or unresolved issues
    val issuesAfter = state.issues
    val persistingIssues = issuesBefore intersect issuesAfter
    persistingIssues.foreach{_.onUnresolved(t.name) }
    checkBugs(t.name)
    checkErrors(t.name)

    // Mirror after transforming
    t match {
      case f: Transformer =>
        globals.mirrorAfterTransform(f)
        if (config.enLog) info(s"Symbols: ${IR.maxId}")
      case _ =>
    }

    result
  }

  final def stageProgram(args: Array[String]): Block[_] = instrument("staging"){
    if (config.enMemLog) memWatch.note("Staging")
    val block = withLog(config.logDir, "0000_Staging.log"){
      dbg(s"Rewrite Rules: ")
      IR.rewrites.names.foreach{name => dbg(s"  $name") }
      dbg(s"Flow Rules: ")
      IR.flows.names.foreach{name => dbg(s"  $name") }

      stageApp(args)
    }
    checkBugs("staging")
    checkErrors("staging") // Exit now if errors were found during staging
    block
  }

  def postprocess(block: Block[_]): Unit = { }

  final def compileProgram(args: Array[String]): Unit = instrument("compile"){
    val block = stageProgram(args)
    if (config.enLog) info(s"Symbols: ${IR.maxId}")
    val result = runPasses(block)
    postprocess(result)
  }

  type CLIParser = scopt.OptionParser[Unit]
  def defineOpts(cli: CLIParser): Unit = {
    cli.note("Verbosity")
    cli.opt[Unit]("qq").action{(_,_)=> config.setV(-1) }.text("Quiet Mode: No logging or console printing")
    cli.opt[Unit]('q',"quiet").action{(_,_)=> config.setV(0) }.text("User Mode: No background logging [default]")
    cli.opt[Unit]('v',"verbose").action{(_,_) => config.setV(1)}.text("Dev Mode: Basic logging")
    cli.opt[Unit]("vv").action{(_,_) => config.setV(2) }.text("Debug Mode: All logging and metrics")

    cli.note("")
    cli.note("Output")
    cli.opt[String]('n',"name").action{(n,_) => config.name = n }.text("Set application name [<app>]")
    cli.opt[String]('o',"out").action{(d,_) => config.genDir = d; config.genDirOverride = true }.text("Set output directory [./gen/<name>]")
    cli.opt[String]('l',"log").action{(d,_) => config.logDir = d }.text("Set log directory [./logs/<name>]")
    cli.opt[String]('r',"report").action{(d,_) => config.repDir = d }.text("Set report directory [./reports/<name>]")
    cli.opt[Unit]("nonaming").action{(_,_) => config.naming = false }.text("Disable verbose naming")

    cli.opt[Unit]("test").action{(_,_) => config.test = true }.text("Testbench Mode: Throw exception on failure.").hidden()
    cli.help("X").hidden()
  }

  /** Called after initial command-line argument parsing has finished. */
  def settings(): Unit = { }

  /** Override to create a custom Config instance */
  def initConfig(): Config = new Config
  def flows(): Unit = { }
  def rewrites(): Unit = { }

  final def init(args: Array[String]): Unit = instrument("init"){
    IR = new State                 // Create a new, empty state
    IR.config = initConfig()
    IR.config.name = name          // Set the default program name
    directives = Map.empty

    val (direcs, other) = args.partition(_.startsWith("-D"))

    directives ++= direcs.flatMap{d =>
      val parts = d.drop(2).split("=").map(_.trim)
      if (parts.length == 2) Some(parts.head.toLowerCase -> parts.last)
      else { warn("Unrecognized argument: $d"); None }
    }

    val parser = new scopt.OptionParser[Unit](script){
      override def reportError(msg: String): Unit = { System.out.error(msg); IR.logError() }
      override def reportWarning(msg: String): Unit = { System.out.warn(msg); IR.logWarning() }
    }
    parser.head(script, desc)
    parser.help("help").text("prints this usage text")
    defineOpts(parser)
    parser.parse(other, ())         // Initialize the Config (from commandline)
    settings()                     // Override config with any DSL or app-specific settings
    IR.config.logDir = IR.config.logDir + files.sep + name + files.sep
    IR.config.genDir = IR.config.genDir + files.sep + {if (config.genDirOverride) "" else {name + files.sep}}
    IR.config.repDir = IR.config.repDir + files.sep + name + files.sep

    info(s"Compiling ${config.name} to ${config.genDir}")
    if (config.enDbg) info(s"Logging ${config.name} to ${config.logDir}")
    if (config.test) info("Running in testbench mode")

    files.deleteExts(IR.config.logDir, "log")

    flows()
    rewrites()
  }

  def execute(args: Array[String]): Unit = instrument("compiler"){
    init(args)
    if (config.enMemLog) memWatch.start(config.logDir)
    compileProgram(args)
  }

  protected implicit class BlockOps[R](block: Block[R]) {
    def ==>(pass: Pass): Block[R] = runPass(pass, block)
    def ==>(pass: (Boolean,Pass)): Block[R] = if (pass._1) runPass(pass._2,block) else block
  }
  protected implicit class ConditionalPass(cond: Boolean) {
    def ?(pass: Pass): (Boolean, Pass) = (cond, pass)
  }

  /**
    * The "real" entry point for the application
    */
  def compile(args: Array[String]): Unit = {
    instrument.reset()
    var failure: Option[Throwable] = None
    try {
      execute(args)
    }
    catch {
      case t: CompilerBugs =>
        onException(t)
        failure = Some(t)

      case t @ CompilerErrors(stage,n) =>
        error(s"${IR.errors} ${plural(n,"error")} found during $stage")
        failure = Some(t)

      case t: Throwable =>
        onException(t)
        val except = UnhandledException(t)
        except.setStackTrace(t.getStackTrace)
        failure = Some(except)
    }

    checkWarnings()
    val tag = {
      if (IR.hadBugs || IR.hadErrors || failure.nonEmpty) s"[${Console.RED}failed${Console.RESET}]"
      else s"[${Console.GREEN}success${Console.RESET}]"
    }

    if (config.enMemLog) memWatch.finish()
    if (config.enDbg) {
      instrument.dump("Nova Profiling Report", getOrCreateStream(config.logDir,"9999_Timing.log"))
      Instrumented.set.foreach{i =>
        val log = i.fullName.replace('.','_')
        val heading = s"${i.instrumentName}: ${i.toString} (${i.hashCode()})"
        val stream = getOrCreateStream(config.logDir,log + ".log")
        i.dumpInstrument(heading, stream)
        stream.println("\n")
        info(s"Profiling results for ${i.fullName} dumped to ${config.logDir}$log.log")
        i.resetInstrument()
      }
    }

    val time = instrument.totalTime
    msg(s"$tag Total time: " + "%.4f".format(time/1000.0f) + " seconds")

    IR.streams.values.foreach{stream => stream.close() }

    if (config.test && failure.nonEmpty) throw failure.get
    else if (failure.nonEmpty || IR.hadBugs || IR.hadErrors) sys.exit(1)
  }
}
