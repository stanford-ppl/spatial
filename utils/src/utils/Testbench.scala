package utils

import org.scalatest.{FlatSpec,Matchers}

abstract class Testbench extends FlatSpec with Matchers {
  type Result = utils.Result
  type Error = Result.Error
  lazy val Pass: Result = Result.Pass
  lazy val Fail: Result = Result.Fail
  lazy val Unknown: Result = Result.Unknown
  lazy val Error: Result.Error.type = Result.Error

  lazy val Indeterminate: Exception = utils.Indeterminate
  lazy val FailedValidation: Exception = utils.FailedValidation
}
