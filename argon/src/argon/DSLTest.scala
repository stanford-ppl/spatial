package argon

import java.io.PrintStream

import utils.process.Subprocess
import utils.{Args, Testbench}

import java.util.concurrent.TimeoutException
import scala.concurrent.{Await, Future, duration}

trait DSLTest extends Testbench with Compiler with Args { test =>
  //-------------------//
  // Testing Arguments //
  //-------------------//

  /** Override to supply list(s) of input arguments to the compiler to compile the application with.
    * The test is compiled multiple times for each backend if multiple lists are given.
    *
    * Suggested argument syntax is:
    *   override def compileArgs: Args = "arg00 arg01 arg02" and "arg10 arg11 arg12"
    *   OR, e.g.
    *   override def compileArgs = Args(Seq.tabulate(N){i => s"i {i+1}" })
    *
    *   Use the first version for a small number of diverse arguments.
    *   Use the second version to generate a large number of runtime arguments using some pattern.
    */
  def compileArgs: Args = NoArgs

  /** Override to supply list(s) of input arguments to run the test with for each backend.
    * The backend is run multiple times if multiple lists are given.
    * The number of runs for each backend is C*R, (C: # of compile lists, R: # of runtime lists)
    *
    * Suggested argument syntax is:
    *   override def runtimeArgs: Args = "arg00 arg01 arg02" and "arg10 arg11 arg12"
    *   OR, e.g.
    *   override def runtimeArgs = Args(Seq.tabulate(N){i => s"i {i+1}" })
    *
    *   Use the first version for a small number of diverse arguments.
    *   Use the second version to generate a large number of runtime arguments using some pattern.
    */
  def runtimeArgs: Args

  /** Override to supply list of input arguments to run during the dse/final performance models */
  def dseModelArgs: Args = ""
  def finalModelArgs: Args = ""

  lazy val DATA = sys.env.getOrElse("TEST_DATA_HOME", { throw MissingDataFolder() })

  //-------------------//
  //     Assertions    //
  //-------------------//
  // These provide staged versions of assert that shadow the native Scala and scalatest versions.
  // These assertions will be checked at application runtime (after backend code generation)
  // Use 'require' methods for unstaged assertions
  import argon.lang.{Bit,Text,Void}
  import argon.node.AssertIf

  /** Staged (application runtime) assertion.
    * This version is not recommended - give explicit failure message if possible.
    */
  def assert(cond: Bit)(implicit ctx: SrcCtx): Void = stage(AssertIf(Set.empty, cond, None))

  /** Staged (application runtime) assertion with failure message. */
  def assert(cond: Bit, msg: Text)(implicit ctx: SrcCtx): Void = stage(AssertIf(Set.empty, cond, Some(msg)))

  /** Unstaged (application staging) assertion.
    * This version is not recommended - give explicit failure message if possible.
    */
  def require(cond: Boolean)(implicit ctx: SrcCtx): Unit = {
    if (!cond) throw RequirementFailure(ctx,"")
  }
  /** Unstaged (application staging) assertion  with failure message. */
  def require(cond: Boolean, msg: String)(implicit ctx: SrcCtx): Unit = {
    if (!cond) throw RequirementFailure(ctx, msg)
  }

  //-------------------//
  //      Backends     //
  //-------------------//

  def backends: Seq[Backend] = Nil
  def property(str: String): Option[String] = sys.props.get(str)
  def checkFlag(str: String): Boolean = property(str).exists(v => v.trim.toLowerCase == "true")


  def commandLine: Boolean = checkFlag("ci")

  /** A backend which can compile and run a given application.
    *
    * @param name The name of the backend
    * @param args The compiler command line arguments to target this backend
    * @param make Command for compiling the generated code in the top-level generated directory
    * @param run  Command for running the generated code in the top-level generated directory
    */
  abstract class Backend(
    val name: String,
    val args: String,
    val make: String,
    val run:  String,
    val model: String,
    val shouldRunModels: Boolean = false
  ){ backend =>
    val makeTimeout: Long = 3000 // Timeout for compiling, in seconds
    val runTimeout: Long  = 3000 // Timeout for running, in seconds
    val modelTimeout: Long  = 180 // Timeout for running, in seconds
    var prev: String = ""

    def shouldRun: Boolean
    override def toString: String = name
    def makeArgs: Seq[String] = make.split(" ").map(_.trim).filter(_.nonEmpty)
    def runArgs: Seq[String] = run.split(" ").map(_.trim).filter(_.nonEmpty)
    def modelArgs(version: String): Seq[String] = (model + " " + version).split(" ").map(_.trim).filter(_.nonEmpty)

    def parseMakeError(line: String): Result = {
      if (line.contains("error") && !line.contains("to avoid error in")) MakeError(line)
      else Unknown
    }
    def parseRunError(line: String): Result = {
      if (line.contains("PASS: 1") || line.contains("PASS: true")) Pass
      else if (line.contains("PASS: 0") || line.contains("PASS: false")) Fail
      else Unknown
    }
    def parseModelError(line: String): Result = {
      if (line.toLowerCase.contains("total cycles for app")) Pass
      else Unknown
    }
    def genDir(name:String):String = s"${IR.config.cwd}/gen/$backend/$name/"
    def logDir(name:String):String = s"${IR.config.cwd}/logs/$backend/$name/"
    def repDir(name:String):String = s"${IR.config.cwd}/reports/$backend/$name/"

    /** Run DSL compilation for the given application. */
    final def compile(expectErrors: Boolean = false): Iterator[() => Result] = {
      import scala.concurrent.ExecutionContext.Implicits.global   // implicit execution context for Futures

      val name = test.name.replace(".", "/")
      val stageArgs = test.compileArgs.cmds
      stageArgs.iterator.map{cmd => () => {
        try {
          val backArgs = backend.args.split(" ").map(_.trim).filterNot(_.isEmpty)
          val stageArgs = cmd.split(" ").map(_.trim).filterNot(_.isEmpty)
          val args = backArgs ++ stageArgs ++ Seq("-v", "--test")
          val f = Future{ scala.concurrent.blocking {
            init(args)
            IR.config.genDir = genDir(name)
            IR.config.logDir = logDir(name)
            IR.config.repDir = repDir(name)
            compileProgram(args)
          }}
          Await.result(f, duration.Duration(backend.makeTimeout, "sec"))
          complete(None)
          Unknown
        }
        catch {
          case _: TimeoutException => CompileError(CompilerTimeout(s"${backend.makeTimeout} seconds"))
          case t: CompilerErrors if expectErrors => Unknown
          case t: Throwable =>
            val failure = handleException(t)
            complete(failure)
            CompileError(t)
        }
      }}
    }

    /** Run a backend command (either make or run) with given arguments and timeout. */
    final def command(pass: String, args: Seq[String], timeout: Long, parse: String => Result, Error: String => Result, wd:String=IR.config.genDir): Result = {
      import scala.concurrent.ExecutionContext.Implicits.global   // implicit execution context for Futures

      val cmdLog = new PrintStream(IR.config.logDir + s"/$pass.log")
      val cmdFile = new PrintStream(IR.config.logDir + s"/$pass.sh")
      var cause: Result = Unknown
      Console.out.println(s"Backend $pass in ${IR.config.logDir}/$pass.log")
      val cmdStr = args.mkString(" ")
      Console.out.println(cmdStr)
      cmdFile.println(s"${cmdStr}")
      cmdFile.close()
      val printOutput = checkFlag(s"test.tee")
      val cmd = new Subprocess(args:_*)({case (lline,_) =>
        val line = lline.replaceAll("[<>]","").replaceAll("&gt","").replaceAll("&lt","")
        val err = parse(line)
        cause = cause.orElse(err)
        cmdLog.println(line)
        if (printOutput) println(line)
        prev = line
        None
      })

      try {
        val f = Future{ scala.concurrent.blocking{ cmd.block(wd) } }
        val code = Await.result(f, duration.Duration(timeout, "sec"))
        val lines = cmd.stdout()
        val errs  = cmd.errors()
        lines.foreach{ll => val l = ll.replaceAll("[<>]","").replaceAll("&gt","").replaceAll("&lt",""); parse(l); cmdLog.println(l) } // replaceAll to prevent JUnit crash
        errs.foreach{ee => val e = ee.replaceAll("[<>]","").replaceAll("&gt","").replaceAll("&lt",""); parse(e); cmdLog.println(e) } // replaceAll to prevent JUnit crash
        if (code != 0) cause = cause.orElse(Error(s"Non-zero exit code $code.\n${errs.take(4).mkString("\n")}"))
        if (pass == "make" && code == 0) cause = Unknown // Don't report an error for zero exit codes in make phase
      }
      catch {
        case _: TimeoutException => cause = cause.orElse(Error(s"Timeout after $timeout seconds"))
        case e: Throwable        => cause = cause.orElse(Error(e.getMessage))
      }
      finally {
        if (cmd.isAlive) cmd.kill()
        cmdLog.close()
      }
      cause
    }

    /** Run backend compilation. */
    final def runMake(): Result = {
      command("make", makeArgs, backend.makeTimeout, backend.parseMakeError, MakeError.apply)
    }

    /** Run compiled generated code. */
    final def runApp(): Result = {
      var result: Result = Unknown
      runtimeArgs.cmds.foreach{ args =>
        result = result orElse command("run", runArgs :+ args, backend.runTimeout, backend.parseRunError, RunError.apply)
      }
      runModels()
      result orElse Pass
    }

    /** Run dse and final models. */
    final def runModels(): Unit = {
      if (shouldRunModels) {
        dseModelArgs.cmds.foreach{ args => 
          command("model", modelArgs("dse") :+ args, backend.modelTimeout, backend.parseModelError, ModelError.apply)
        }
        finalModelArgs.cmds.foreach{ args => 
          command("model", modelArgs("final") :+ args, backend.modelTimeout, backend.parseModelError, ModelError.apply)
        }
      }
    }

    /** Run everything for this backend, including DSL compile, backend make, and backend run. */
    def runBackend(): Unit = {
      s"${test.name}" should s"compile, run, and verify for backend $name" in {
        var result: Result = Unknown
        val designs = compile()
        while (designs.hasNext && result.continues) {
          val generate = designs.next()
          result = result.orElse{
            generate() ==>
              runMake() ==>
              runApp()
          }
        }
        result.resolve()
      }
    }
  }

  /** Denotes a negative test which is expected to have the specified number of DSL compile errors. */
  class IllegalExample(args: String, errors: Int) extends Backend(
    name = "IllegalExample",
    args = args,
    make = "",
    run  = "",
    model = ""
  ) {
    override def shouldRun: Boolean = true
    override def runBackend(): Unit = {
      s"${test.name}" should s"have $errors compiler errors" in {
        compile(expectErrors = true).foreach{err =>
          err()
          IR.hadErrors shouldBe true
          IR.errors shouldBe errors
        }
      }
    }
  }

  lazy val DISABLED: Seq[Backend] = Seq(IGNORE_TEST)

  /** Denotes a disabled test. (Use the DISABLED function here) */
  private object IGNORE_TEST extends Backend(
    name = "Ignore",
    args = "",
    make = "",
    run  = "",
    model = ""
  ) {
    override def shouldRun: Boolean = true
    override def runBackend(): Unit = ignore should "compile, run, and verify" in { () }
  }

  /** Override this method to provide custom IR checking after compilation has completed. */
  protected def checkIR(block: argon.Block[_]): Result = Unknown

  /** Postprocessing phase after compilation has completed. Only the last block is passed. */
  override def postprocess(block: argon.Block[_]): Unit = {
    import argon.node.AssertIf
    super.postprocess(block)

    if (config.test) {
      val stms = block.nestedStms
      val hasAssert = stms.exists{case Op(_: AssertIf) => true; case _ => false }
      if (!hasAssert) throw Unknown
      checkIR(block)
    }
  }


  private val tests = backends.filter(_.shouldRun)
  if (commandLine) {
    // If running from a non-testing script, run the standard compiler flow
    val args = sys.env.get("TEST_ARGS").map(_.split(" ")).getOrElse(Array.empty)
    System.out.println(s"Running standard compilation flow for test with args: ${args.mkString(" ")}")
    name should "compile" in { compile(args); sys.exit(0) }
  }
  else if (tests.isEmpty) {
    ignore should s"...nothing? (No backends enabled. Enable using -D<backend>=true). backends:\n${backends.mkString("\n")}" in { () }
  }
  else {
    // Otherwise run all the backend tests (if there are any enabled)
    tests.foreach{backend => backend.runBackend() }
  }

}
