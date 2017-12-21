package pcc

import pcc.core.{CompilerBugs, CompilerErrors}
import pcc.data._
import pcc.traversal.{Pass, Transformer}
import pcc.util.files
import pcc.util.files.deleteExts

import scala.collection.mutable.ArrayBuffer


trait Compiler { self =>
  protected var IR: State = new State
  final implicit def __IR: State = IR

  def name: String = self.getClass.getName.replace("class ", "").replace('.','_').replace("$","")
  protected val testbench: Boolean = false

  final protected val passes: ArrayBuffer[Pass] = ArrayBuffer.empty[Pass]
  //final protected def codegenerators: Seq[Codegen] = passes.collect{case x: Codegen => x}

  protected def checkBugs(start: Long, stage: String): Unit = if (IR.hadBugs) {
    throw CompilerBugs(stage, IR.bugs)
  }

  protected def checkErrors(start: Long, stage: String): Unit = if (IR.hadErrors) {
    throw CompilerErrors(stage, IR.errors)
  }

  protected def checkWarnings(): Unit = if (IR.hadWarnings) {
    warn(s"""${IR.warnings} ${plural(IR.warnings, "warning","warnings")} found""")
  }

  protected def onException(t: Throwable): Unit = {
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
      trace.take(4).foreach{t => bug("  " + t) }
      if (trace.length > 4) bug(s"  ... [see log]")
    }
    bug(s"This is due to a compiler bug. A log file has been created at: ")
    bug(s"  ${config.logDir}${config.name}_exception.log")
  }

  def createTraversalSchedule(state: State): Unit
  def stageProgram(args: Array[String]): Block[_]
  def settings(): Unit = { }

  final protected def runTraversals[R](startTime: Long, b: Block[R], timingLog: String): Unit = {
    var block: Block[R] = b

    // --- Traversals
    for (t <- passes) {
      if (t.needsInit) t.init()

      block = t.run(block)
      // After each traversal, check whether there were any reported errors
      checkBugs(startTime, t.name)
      checkErrors(startTime, t.name)

      val v = IR.config.logLevel
      IR.config.logLevel = 2
      inLog(timingLog) {
        dbg(s"  ${t.name}: " + "%.4f".format(t.lastTime / 1000))
      }
      IR.config.logLevel = v

      t match {
        case f: Transformer => globals.mirrorAll(f)
        case _ =>
      }
    }
  }


  def compileProgram(args: Array[String]): Unit = {
    deleteExts(config.logDir, ".log")
    msg(s"Compiling ${config.name} to ${config.genDir}")
    if (config.enLog) msg(s"Logging ${config.name} to ${config.logDir}")

    val startTime = System.currentTimeMillis()
    if (config.enDbg) echoConfig()
    val block = withLog(config.logDir, "0000 Staging.log"){ stageProgram(args) }

    // Exit now if errors were found during staging
    checkBugs(startTime, "staging")
    checkErrors(startTime, "staging")

    val timingLog = setupStream(IR.config.logDir, "9999 Timing.log")
    runTraversals(startTime, block, timingLog)

    val time = (System.currentTimeMillis - startTime).toFloat

    val v = IR.config.logLevel
    IR.config.logLevel = 2
    inLog(timingLog) {
      dbg(s"  Total: " + "%.4f".format(time / 1000))
      dbg(s"")
      val totalTimes = passes.distinct.groupBy(_.name).mapValues{pass => pass.map(_.totalTime).sum }.toList.sortBy(_._2)
      for (t <- totalTimes) {
        dbg(s"  ${t._1}: " + "%.4f".format(t._2 / 1000))
      }
    }
    IR.config.logLevel = v
  }

  final def echoConfig(): Unit = {
    info(s"Name: ${config.name}")
    info(s"Rep directory: ${config.repDir}")
    info(s"Log directory: ${config.logDir}")
    info(s"Gen directory: ${config.genDir}")
    info(s"Show warnings: ${config.enWarn}")
    info(s"Show errors:   ${config.enError}")
    info(s"Show infos:    ${config.enInfo}")
    info(s"Enable logging: ${config.enDbg}")
    info(s"Enable verbose: ${config.enLog}")
  }

  final def init(args: Array[String]): Unit = {
    //val oldState = IR
    IR = new State                      // Create a new, empty state

    // TODO: Copy globals (created prior to the main method) to the new state's graph
    //val globals = 0 until oldState.graph.firstNonGlobal
    //oldState.graph.copyNodesTo(globals, IR.graph)

    passes.clear()                 // Reset traversal passes
    IR.config.init(args)           // Initialize the Config (from files)
    IR.config.name = name          // Set the default program name
    IR.config.logDir = files.cwd + files.sep + "logs" + files.sep + name + files.sep
    IR.config.genDir = files.cwd + files.sep + "gen" + files.sep + name + files.sep
    IR.config.repDir = files.cwd + files.sep + "reports" + files.sep + name + files.sep

    settings()                     // Override config with any DSL or App specific settings
    createTraversalSchedule(IR)    // Set up the compiler schedule for the app
  }

  /**
    * The "real" entry point for the application
    */
  final def main(args: Array[String]): Unit = {
    val start = System.currentTimeMillis

    try {
      init(args)
      compileProgram(args)
    }
    catch {
      case CompilerBugs(stage,n) =>
        onException(new Exception(s"Encountered compiler ${plural(n,"bug","bugs")} during pass $stage"))
        if (testbench) throw new TestbenchFailure(s"$n compiler bugs")

      case CompilerErrors(stage,n) =>
        error(s"""${IR.errors} ${plural(IR.errors,"error","errors")} found during $stage""")

      case t: TestbenchFailure => throw t

      case t: Throwable => onException(t); IR.bugs += 1
    }
    val time = (System.currentTimeMillis - start).toFloat

    checkWarnings()
    val tag = {
      if (IR.hadBugs || IR.hadErrors) s"[${Console.RED}failed${Console.RESET}]"
      else s"[${Console.GREEN}success${Console.RESET}]"
    }
    msg(s"$tag Total time: " + "%.4f".format(time/1000) + " seconds")

    IR.streams.values.foreach{stream => stream.close() }
  }

}


