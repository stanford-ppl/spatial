package pcc.core

import scala.util.control.NoStackTrace

class LexerError(val ctx: SrcCtx, val msg: String) extends Error with NoStackTrace
class ParserError(val ctx: SrcCtx, val msg: String) extends Error with NoStackTrace

class TypeError(val ctx: SrcCtx, val msg: String) extends Error with NoStackTrace
class UserError(val ctx: SrcCtx, val msg: String) extends Error with NoStackTrace
class PlacerError(val msg: String) extends Error with NoStackTrace
class SearchFailure(override val msg: String) extends PlacerError(msg)

case class TestbenchFailure(msg: String) extends Exception(msg) with NoStackTrace

case class CompilerErrors(stage: String, n: Int) extends Error with NoStackTrace
case class CompilerBugs(stage: String, n: Int) extends Error with NoStackTrace
