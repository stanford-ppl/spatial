//package nova.compiler
//package parser
//
//import forge.SrcCtx
//import core.ParserError
//import nova.compiler.lexer._
//
//import scala.util.parsing.combinator.Parsers
//import scala.util.parsing.input.{NoPosition, Position, Positional, Reader}
//
//sealed trait AST extends Positional
//case class ValDef(name: IDENTIFIER, typ: Option[TypeTree], rhs: AST) extends AST
//case class Blk(stms: List[AST]) extends AST
//
//case class TypeTree(name: IDENTIFIER, args: List[TypeArgs]) extends AST
//case class TypeArgs(args: List[TypeTree]) extends AST
//
//object Parser extends Parsers {
//  override type Elem = Token
//
//  class TokenReader(tokens: Seq[Token]) extends Reader[Token] {
//    override def first: Token = tokens.head
//    override def atEnd: Boolean = tokens.isEmpty
//    override def pos: Position = tokens.headOption.map(_.pos).getOrElse(NoPosition)
//    override def rest: Reader[Token] = new TokenReader(tokens.tail)
//  }
//
//  def apply(tokens: Seq[Token]): Either[ParserError, AST] = {
//    val reader = new TokenReader(tokens)
//    /*program(reader) match {
//      case NoSuccess(msg, next) => Left(ParserError(new SrcCtx(next.pos.line, next.pos.column), msg))
//      case Success(result, next) => Right(result)
//    }*/
//    Left(new ParserError(SrcCtx.empty, "Parser incomplete"))
//  }
//
//  def program: Parser[AST] = positioned { phrase(block) }
//
//  def block: Parser[AST] = positioned {
//    rep1(statement) ^^ {stmtList => Blk(stmtList) }
//  }
//
//  def statement: Parser[AST] = positioned {
//    val untypedVal = (VAL() ~ identifier ~ EQUALS() ~ statement) ^^ {case _~name~_~stm => ValDef(name, None, stm) }
//    val typedVal   = (VAL() ~ identifier ~ COLON() ~ typetree ~ EQUALS() ~ statement) ^^ {case _~name~_~tree~_~stm => ValDef(name, Some(tree), stm) }
//    //val prefixCall  = (identifier ~ identifier)
//    //val postfixCall = (identifier ~ identifier)
//    //val binaryCall  = (identifier ~ identifier ~ identifier)
//    untypedVal | typedVal
//  }
//
//  def typetree: Parser[TypeTree] = positioned {
//    val rawType = identifier ^^ {name => TypeTree(name, Nil) }
//    val appType = (identifier ~ rep1(typeargs)) ^^ {case name~args => TypeTree(name,args) }
//    rawType | appType
//  }
//  def typeargs: Parser[TypeArgs] = positioned {
//    (LBRACK() ~ repsep(typetree, COMMA()) ~ RBRACK()) ^^ {case _~argList~_ => TypeArgs(argList)  }
//  }
//
//  /*def ifThen: Parser[IfThen] = positioned {
//    (IF() ~ statement ~ block) ^^ {case _ ~ cond ~ block => IfThen(cond, block) }
//  }
//
//  def otherwiseThen: Parser[OtherwiseThen] = positioned {
//    (OTHERWISE() ~ ARROW() ~ INDENT() ~ block ~ DEDENT()) ^^ {
//      case _ ~ _ ~ _ ~ block ~ _ => OtherwiseThen(block)
//    }
//  }*/
//
//  private def identifier: Parser[IDENTIFIER] = positioned {
//    accept("identifier", { case id @ IDENTIFIER(name) => id })
//  }
//
//  private def literal: Parser[LITERAL] = positioned {
//    accept("string literal", { case lit @ LITERAL(name) => lit })
//  }
//
//}
