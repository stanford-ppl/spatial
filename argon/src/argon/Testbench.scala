package argon

import java.io.PrintStream

import utils.process.Subprocess
import utils.implicits.Readable._

import scala.concurrent.{Await, Future, duration}
import scala.util.{Success, Try, Failure}

trait Testbench {
  val backends: Seq[Backend]

  type Result = Option[Try[Boolean]]
  val Pass: Result = Some(Success(true))
  val Fail: Result = Some(Success(false))
  val Unknown: Result = None
  type Error  = Some[Failure[Boolean]]
  object Error {
    def apply(msg: String): Error = Some(Failure[Boolean](new Exception(msg)))
    def apply(t: Throwable): Error = Some(Failure[Boolean](t))
    def unapply(x: Result): Option[Throwable] = x match {
      case Some(Failure(t)) => Some(t)
      case _ => None
    }
  }

  implicit class ErrorOpt(x: Result) {
    def ==>(func: => Result): Result = x match {
      case Some(Failure(_)) => x
      case _ => func
    }
    def resolve(): Unit = x match {
      case Pass     => // Passed
      case Fail     => throw FailedTest
      case Unknown  => throw Indeterminate
      case Error(t) => throw t
    }
  }

  case object Indeterminate extends Exception("Indeterminate result. Add PASS: {outcome}.")
  case object FailedTest extends Exception("Test did not pass validation.")

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

  /**
    * Returns a function which runs the given Object for some Backend
    */
  def compile(app: DSLApp): Backend => Result = {backend: Backend =>
    import scala.concurrent.ExecutionContext.Implicits.global   // implicit execution context for Futures

    val name = app.name.replace("_", "/")
    try {
      val backArgs = backend.args.split(" ").map(_.trim)
      val stageArgs = app.stageArgs.split(" ").map(_.trim)
      val args = backArgs ++ stageArgs
      val console = new PrintStream(app.IR.config.logDir + "/compile.log")
      val f = Future{ scala.concurrent.blocking{
        implicit val IR: State = app.IR
        withOut(console){
          app.init(args)
          app.IR.config.setV(-2)     // TODO[5]: Won't see any of this output anyway
          app.IR.config.genDir = s"${app.IR.config.cwd}/gen/$backend/$name/"
          app.IR.config.logDir = s"${app.IR.config.cwd}/logs/$backend/$name/"
          app.IR.config.repDir = s"${app.IR.config.cwd}/reports/$backend/$name/"
          app.compileProgram(args)
          app match {
            case r: Requirements => r.complete()
            case _ =>
          }
        }
      }}
      Await.result(f, duration.Duration(backend.makeTimeout, "sec"))
      Unknown
    }
    catch {
      case e: Throwable => Error(e)
    }
  }

  def command(app: DSLApp, pass: String, args: Backend => Seq[String], timeout: Backend => Long, parse: Backend => String => Result): Backend => Result = {backend: Backend =>
    import scala.concurrent.ExecutionContext.Implicits.global   // implicit execution context for Futures

    val cmdLog = new PrintStream(app.IR.config.logDir + s"/$pass.log")
    var cause: Result = Unknown
    val cmd = new Subprocess(args(backend):_*)({case (line,_) =>
      val err = parse(backend)(line)
      cause = cause.orElse(err)
      cmdLog.println(line)
      backend.prev = line
      None
    })

    try {
      val f = Future{ scala.concurrent.blocking{ cmd.block(app.IR.config.genDir) } }
      val code = Await.result(f, duration.Duration(timeout(backend), "sec"))
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

  def make(app: DSLApp): Backend => Result = {
    command(app,"make", _.makeArgs,_.makeTimeout,_.parseMakeError)
  }
  def run(app: DSLApp): Backend => Result = {
    command(app, "run", _.runArgs :+ app.testArgs.mkString(" "), _.runTimeout, _.parseRunError)
  }



  def test(x: Any): Unit = x match {
    case app: DSLApp =>
      val compiler = compile(app)
      val maker = make(app)
      val runner = run(app)


      backends.foreach { backend =>
        val result = compiler(backend) ==>
          maker(backend) ==>
          runner(backend)
        result.resolve()
      }

    case _ => throw new Exception(r"Don't know how to run test for ${x.getClass}")
  }


}
