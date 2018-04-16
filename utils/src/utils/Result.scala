package utils

case object Indeterminate extends Exception("Indeterminate result. Test had no validation checks.")
case object FailedValidation extends Exception("Test did not pass validation.")

sealed abstract class Result {
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

  case object Fail extends Result {
    def ==>(func: => Result): Result = this
    def orElse(result: => Result): Result = this
    def resolve(): Unit = throw FailedValidation
    def continues: Boolean = false
  }

  case object Unknown extends Result {
    def ==>(func: => Result): Result = this orElse func
    def orElse(result: => Result): Result = result
    def resolve(): Unit = throw Indeterminate
    def continues: Boolean = true
  }

  case class Error(t: Throwable) extends Result {
    def ==>(func: => Result): Result = this
    def orElse(result: => Result): Result = this
    def resolve(): Unit = throw t
    def continues: Boolean = false
  }
  object Error {
    def apply(msg: String): Error = Error(new Exception(msg))
  }
}


