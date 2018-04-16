package argon

import java.io.{File, PrintStream}

import utils.process.Subprocess
import utils.{Args, Testbench}

import scala.concurrent.{Await, Future, duration}

abstract class DSLTest extends Testbench with DSLApp with Args {
  //-------------------//
  //      Backends     //
  //-------------------//

  def backends: Seq[Backend]

  /** A backend which can compile and run a given application.
    *
    * @param name The name of the backend
    * @param args The compiler command line arguments to target this backend
    * @param make Command for compiling the generated code in the top-level generated directory
    * @param run  Command for running the generated code in the top-level generated directory
    */
  class Backend(
    val name: String,
    val args: String,
    val make: String,
    val run:  String
  ){
    val makeTimeout: Long = 2000 // Timeout for compiling, in seconds
    val runTimeout: Long  = 2000 // Timeout for running, in seconds
    var prev: String = ""

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
  }

  //-------------------//
  // Testing Arguments //
  //-------------------//
  case class Fuzz(ranges: Range*)

  final val NoArgs = Args(Seq(""))

  def compileArgs: Args = NoArgs
  def runtimeArgs: Args
  def fuzzArgs: Option[Fuzz] = None

  def command(backend: Backend, pass: String, args: Seq[String], timeout: Long, parse: String => Result): Result = {
    import scala.concurrent.ExecutionContext.Implicits.global   // implicit execution context for Futures

    val cmdLog = new PrintStream(IR.config.logDir + s"/$pass.log")
    var cause: Result = Unknown
    val cmd = new Subprocess(args:_*)({case (line,_) =>
      val err = parse(line)
      cause = cause.orElse(err)
      cmdLog.println(line)
      backend.prev = line
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

  def make(backend: Backend): Result = {
    command(backend, "make", backend.makeArgs, backend.makeTimeout, backend.parseMakeError)
  }
  def run(backend: Backend): Result = {
    command(backend, "run", backend.runArgs, backend.runTimeout, backend.parseRunError)
  }


  /**
    * Returns a function which runs the given Object for some Backend
    */
  def compile(backend: Backend): Iterator[() => Result] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val name = this.name.replace("_", "/")
    val stageArgs = if (this.compileArgs.cmds.isEmpty) Seq("") else this.compileArgs.cmds
    stageArgs.iterator.map{cmd => () => {
      try {
        val backArgs = backend.args.split(" ").map(_.trim)
        val stageArgs = cmd.split(" ").map(_.trim)
        val args = (backArgs ++ stageArgs ++ Seq("-v", "--test")).filterNot(_.isEmpty)
        val f = Future{ scala.concurrent.blocking {
          val cwd = new File(".").getAbsolutePath
          val logDir = s"$cwd/logs/$backend/$name/"
          withOut(logDir, "console.log") {
            init(args)
            IR.config.genDir = s"$cwd/gen/$backend/$name/"
            IR.config.logDir = logDir
            compileProgram(args)
          }
        }}
        Await.result(f, duration.Duration(backend.makeTimeout, "sec"))
        Unknown
      }
      catch {
        case e: Throwable => Error(e)
      }
    }}
  }

  backends.foreach{backend =>
    s"$name" should s"compile, run, and verify for backend ${backend.name}" in {
      var result: Result = Unknown
      val designs = compile(backend)
      this match {
        case req: argon.Requirements => req.complete()
        case _ => // Nothing
      }

      while (designs.hasNext && result.continues) {
        val generate = designs.next()
        result = result.orElse{
          generate() ==>
            make(backend) ==>
            run(backend)
        }
      }
      result match {
        case Error(t) => onException(t)
        case _ =>
      }
      result.resolve()
    }
  }
}
