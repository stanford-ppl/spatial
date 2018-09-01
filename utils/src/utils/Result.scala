package utils

import scala.util.control.NoStackTrace

case class BackendError(msg: String) extends Exception(msg) with NoStackTrace

sealed trait Result {
  def ==>(func: => Result): Result
  def orElse(result: => Result): Result
  def resolve(): Unit
  def continues: Boolean
}
object Result {
  type Result = utils.Result

  case object Pass extends Result {
    def ==>(func: => Result): Result = this orElse func
    def orElse(result: => Result): Result = result match {
      case Pass => Pass
      case _ => result
    }
    def resolve(): Unit = ()
    def continues: Boolean = true
  }

  case object Fail extends Exception("Test did not pass validation.") with Result with NoStackTrace {
    def ==>(func: => Result): Result = this
    def orElse(result: => Result): Result = this
    def resolve(): Unit = throw this
    def continues: Boolean = false
  }

  case object Unknown extends Exception("Test had no validation checks. (Indeterminate result)") with Result with NoStackTrace {
    def ==>(func: => Result): Result = this orElse func
    def orElse(result: => Result): Result = result
    def resolve(): Unit = throw this
    def continues: Boolean = true
  }

  case class CompileError(t: Throwable) extends Result {
    def ==>(func: => Result): Result = this
    def orElse(result: => Result): Result = this
    def resolve(): Unit = throw t
    def continues: Boolean = false
  }

  case class MakeError(msg: String) extends Exception(msg) with Result with NoStackTrace {
    def ==>(func: => Result): Result = this
    def orElse(result: => Result): Result = this
    def resolve(): Unit = throw this
    def continues: Boolean = false
  }

  case class RunError(msg: String) extends Exception(msg) with Result with NoStackTrace {
    def ==>(func: => Result): Result = this
    def orElse(result: => Result): Result = this
    def resolve(): Unit = throw this
    def continues: Boolean = false
  }

}


