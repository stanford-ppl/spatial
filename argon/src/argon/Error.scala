package argon

import utils.plural

import scala.util.control.NoStackTrace

case class UnhandledException(t: Throwable)
   extends Exception(s"Uncaught exception ${t.getMessage} (${t.getCause})")

case class EarlyStop(stage: String)
   extends Exception(s"Stop after pass $stage was requested.")
      with NoStackTrace

case class CompilerErrors(stage: String, n: Int)
   extends Exception(s"$n compiler ${plural(n,"error")} during pass $stage")
      with NoStackTrace

case class CompilerBugs(stage: String, n: Int)
   extends Exception(s"$n ${plural(n,"bug")} found during $stage")
      with NoStackTrace

case class RequirementFailure(ctx: SrcCtx, msg: String)
   extends Exception("Requirement failure: " + msg)
      with NoStackTrace

case class MissingDataFolder()
   extends Exception("The TEST_DATA_HOME environment variable was required for this test but was unset.")
      with NoStackTrace

case class CompilerTimeout(time: String)
   extends Exception(s"DSL compilation timed out after $time. (Indeterminate result)")
      with NoStackTrace

case class BackendTimeout(pass: String, time: String)
   extends Exception(s"Backend $pass timed out after $time. (Indeterminate result)")
      with NoStackTrace