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
