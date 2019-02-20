package utils

import org.scalatest.{FlatSpecLike, Matchers}

trait Testbench extends FlatSpecLike with Matchers /* with Serializable */ {
  type Result = utils.Result
  type CompileError = Result.CompileError
  type MakeError = Result.MakeError
  type RunError = Result.RunError
  type ModelError = Result.ModelError
  implicit def resultToBoolean(x: Boolean): Result = if (x) Pass else Fail
  lazy val Pass         = Result.Pass
  lazy val Fail         = Result.Fail
  lazy val Unknown      = Result.Unknown
  lazy val CompileError = Result.CompileError
  lazy val MakeError    = Result.MakeError
  lazy val RunError     = Result.RunError
  lazy val ModelError     = Result.ModelError

}
