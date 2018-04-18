package argon

import java.io.{File, PrintStream}

import utils.process.Subprocess
import utils.{Args, Testbench}

import scala.concurrent.{Await, Future, duration}

trait DSLTest extends Testbench with Compiler with Args { test =>
  //-------------------//
  // Testing Arguments //
  //-------------------//

  def compileArgs: Args = NoArgs
  def runtimeArgs: Args

  //-------------------//
  //      Backends     //
  //-------------------//

  def backends: Seq[Backend]
  def enable(str: String): Boolean = sys.props.get(str).exists(v => v.trim.toLowerCase == "true")
  lazy val DISABLE: Seq[Backend] = Seq(IGNORE_TEST)

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
    val run:  String
  ){ backend =>
    val makeTimeout: Long = 2000 // Timeout for compiling, in seconds
    val runTimeout: Long  = 2000 // Timeout for running, in seconds
    var prev: String = ""

    def shouldRun: Boolean
    override def toString: String = name
    def makeArgs: Seq[String] = make.split(" ").map(_.trim).filter(_.nonEmpty)
    def runArgs: Seq[String] = run.split(" ").map(_.trim).filter(_.nonEmpty)

    def parseMakeError(line: String): Result = {
      if (line.contains("error")) Error(line)
      else Unknown
    }
    def parseRunError(line: String): Result = {
      if (line.contains("PASS: 1") || line.contains("PASS: true")) Pass
      else if (line.contains("PASS: 0") || line.contains("PASS: false")) Fail
      else Unknown
    }

    final def runMake(): Result = {
      command("make", makeArgs, backend.makeTimeout, backend.parseMakeError)
    }
    final def runApp(): Result = {
      var result: Result = Unknown
      runtimeArgs.cmds.foreach { args =>
        val a = args.split(" ").map(_.trim).filter(_.nonEmpty)
        result = result orElse command("run", runArgs ++ a, backend.runTimeout, backend.parseRunError)
      }
      result orElse Pass
    }
    final def compile(): Iterator[() => Result] = {
      import scala.concurrent.ExecutionContext.Implicits.global   // implicit execution context for Futures

      val name = this.name.replace("_", "/")
      val stageArgs = test.compileArgs.cmds
      stageArgs.iterator.map{cmd => () => {
        try {
          val backArgs = backend.args.split(" ").map(_.trim)
          val stageArgs = cmd.split(" ").map(_.trim)
          val args = backArgs ++ stageArgs ++ Seq("-v")
          val f = Future{ scala.concurrent.blocking {
            init(args)
            IR.config.genDir = s"${IR.config.cwd}/gen/$backend/$name/"
            IR.config.logDir = s"${IR.config.cwd}/logs/$backend/$name/"
            compileProgram(args)
          }}
          Await.result(f, duration.Duration(backend.makeTimeout, "sec"))
          Unknown
        }
        catch {
          case e: Throwable => Error(e)
        }
      }}
    }

    final def command(pass: String, args: Seq[String], timeout: Long, parse: String => Result): Result = {
      import scala.concurrent.ExecutionContext.Implicits.global   // implicit execution context for Futures

      val cmdLog = new PrintStream(IR.config.logDir + s"/$pass.log")
      var cause: Result = Unknown
      val cmd = new Subprocess(args:_*)({case (line,_) =>
        val err = parse(line)
        cause = cause.orElse(err)
        cmdLog.println(line)
        prev = line
        None
      })

      try {
        val f = Future{ scala.concurrent.blocking{ cmd.block(IR.config.genDir) } }
        val code = Await.result(f, duration.Duration(timeout, "sec"))
        if (code != 0) cause = cause.orElse(Error(s"Non-zero exit code: $code"))
      }
      catch {
        case e: Throwable =>
          cmd.kill()
          cause = cause.orElse(Error(e))
      }
      finally {
        cmdLog.close()
      }
      cause
    }

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

  object IllegalExample extends Backend(
    name = "IllegalExample",
    args = "",
    make = "",
    run  = ""
  ) {
    def shouldRun = true
    override def runBackend(): Unit = {
      s"${test.name}" should "have compiler errors" in {
        compile().foreach{err =>
          err()
          IR.hadErrors shouldBe true
        }
      }
    }
  }


  object IGNORE_TEST extends Backend(
    name = "Ignore",
    args = "",
    make = "",
    run  = ""
  ) {
    def shouldRun: Boolean = true
    override def runBackend(): Unit = ignore should "compile, run, and verify" in { () }
  }


  private val tests = backends.filter(_.shouldRun)
  if (tests.isEmpty) {
    ignore should "...nothing? (No backends enabled. Enable using -D<backend>=true)" in { () }
  }
  else {
    tests.foreach{backend => backend.runBackend() }
  }

}
