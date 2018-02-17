//package nova.compiler
//package lexer
//
//import nova.core.LexerError
//
//import scala.util.parsing.combinator.RegexParsers
//import scala.util.parsing.input.Positional
//
//sealed trait Token extends Positional
//
//case class METHODNAME(str: String) extends Token
//case class IDENTIFIER(str: String) extends Token
//case class LITERAL(str: String) extends Token
//case class IF() extends Token
//case class ELSE() extends Token
//case class END() extends Token
//
//case class VAL() extends Token
//
//case class ARROW() extends Token  // =>
//case class COLON() extends Token  // :
//case class COMMA() extends Token  // ,
//case class PERIOD() extends Token // .
//case class LBRACK() extends Token // [
//case class RBRACK() extends Token // ]
//case class LPAREN() extends Token // (
//case class RPAREN() extends Token // )
//case class LBRACE() extends Token // {
//case class RBRACE() extends Token // }
//case class EQUALS() extends Token // =
//
//case class MATCH() extends Token
//case class CASE() extends Token
//
//
//object Lexer extends RegexParsers {
//  override def skipWhitespace = true
//  override val whiteSpace = "[ \t\r\f]+".r
//
//  def apply(code: String): Either[LexerError, List[Token]] = {
//    /*val lines = code.split("\n")
//    val lexed = parse(tokens, code) match {
//      case NoSuccess(msg, next) => Left(LexerError(new SrcCtx(next.pos.line, next.pos.column,Some(lines(next.pos.line))), msg))
//      case Success(result, next) => Right(result)
//    }*/
//    Right(Nil)
//  }
//
//  def tokens: Parser[List[Token]] = {
//    phrase(rep1(matchx | colon | arrow
//      | equals | comma | literal | identifier | operator)) ^^ { rawTokens => rawTokens }
//  }
//
//  def operator: Parser[METHODNAME] = positioned {
//    "[<>@#$%^&*/-+=!?:~][<>@#$%^&*/-+=!?:~]".r ^^ {str => METHODNAME(str) }
//  }
//
//  def identifier: Parser[IDENTIFIER] = positioned {
//    "[a-zA-Z_][a-zA-Z0-9_]*(_[a-zA-Z0-9<>@#$%^&*/-+=!?:~])?".r ^^ { str => IDENTIFIER(str) }
//  }
//
//  def literal: Parser[LITERAL] = positioned {
//    """"[^"]*"""".r ^^ { str =>
//      val content = str.substring(1, str.length - 1)
//      LITERAL(content)
//    }
//  }
//
//  def ifx           = positioned { "if"            ^^ (_ => IF()) }
//  def elsex         = positioned { "else"          ^^ (_ => ELSE()) }
//  def matchx        = positioned { "match"         ^^ (_ => MATCH()) }
//  def colon         = positioned { ":"             ^^ (_ => COLON()) }
//  def arrow         = positioned { "=>"            ^^ (_ => ARROW()) }
//  def equals        = positioned { "=="            ^^ (_ => EQUALS()) }
//  def comma         = positioned { ","             ^^ (_ => COMMA()) }
//
//}
