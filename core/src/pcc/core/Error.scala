package pcc.core

class LexerError(val ctx: SrcCtx, val msg: String) extends Error
class ParserError(val ctx: SrcCtx, val msg: String) extends Error

class TypeError(val ctx: SrcCtx, val msg: String) extends Error
class UserError(val ctx: SrcCtx, val msg: String) extends Error

class PlacerError(val msg: String) extends Error
class SearchFailure(override val msg: String) extends PlacerError(msg)


case class TestbenchFailure(msg: String) extends Error

case class CompilerErrors(stage: String, n: Int) extends Error
case class CompilerBugs(stage: String, n: Int) extends Error