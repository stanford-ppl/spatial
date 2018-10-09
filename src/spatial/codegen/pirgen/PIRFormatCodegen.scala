package spatial.codegen.pirgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.metadata._
import spatial.metadata.memory._

import scala.collection.mutable

trait PIRFormattedCodegen extends Codegen {

case class Lhs(sym:Sym[_], postFix:Option[String]=None)
trait Stm {
  val lhs:Lhs
  val comment:String
}
case class DefStm(lhs:Lhs, tp:String, args:Seq[Any], comment:String) extends Stm
case class AliasStm(lhs:Lhs, alias:Lhs, comment:String) extends Stm

  implicit def sym_to_lhs(sym:Sym[_]) = Lhs(sym)

  def state(stm:Stm) = {
    val lhs = stm.lhs
    assert(!stmMap.contains(lhs), s"Already contains $lhs -> ${stmMap(lhs)} but remap to $this")
    stmMap += lhs -> stm
    emit(src"val ${stm.lhs} = $stm // ${stm.comment}")
  }

  def define(lhs:Lhs, tp:String, args:Any*) = state(DefStm(lhs, tp, args, qdef(lhs.sym)))
  def defineWithComment(lhs:Lhs, tp:String, comment:String, args:Any*) = state(DefStm(lhs, tp, args, comment))
  def alias(lhs:Lhs, alias:Lhs) = state(AliasStm(lhs, alias, qdef(lhs.sym)))
  def alias(lhs:Lhs, alias:Lhs, comment:String) = state(AliasStm(lhs, alias, comment))

  val stmMap = mutable.Map[Lhs, Stm]()
  def lookup(lhs:Lhs):DefStm = stmMap(lhs) match {
    case AliasStm(lhs, alias, _) => lookup(alias)
    case stm:DefStm => stm
  }

  override protected def preprocess[R](b: Block[R]): Block[R] = {
    stmMap.clear
    super.preprocess(b)
  }

  override def emitHeader(): Unit = {
    super.emitHeader()
    inGen(out, "AccelMain.scala") {
      emit(s"implicit class CtxHelper[T<:IR](x:T) { def ctx(c:String):T = { srcCtxOf(x) = c; x } }")
      emitNameFunc
    }
  }

  def emitNameFunc = {
    emit(s"implicit class NameHelper[T<:IR](x:T) { def name(n:String):T = { nameOf(x) = n; x } }")
  }

  def qdef(sym:Sym[_]) = sym.op match {
    case Some(op) => src"$sym = $op"
    case None => src"$sym"
  }

  override protected def quoteOrRemap(arg: Any): String = arg match {
    case x:Iterable[_] => x.map(quoteOrRemap).toList.toString
    case Some(x) => s"Some(${quoteOrRemap(x)})"
    case None => s"None"
    case e: Sym[_] => quoteOrRemap(sym_to_lhs(e))
    case m: Type[_] => remap(m)
    case x@Lhs(sym,Some(postFix)) => s"${quote(sym)}_$postFix" 
    case x@Lhs(sym,None) => quote(sym)
    case DefStm(lhs, tp, args, _) =>
      val argsString = args.map {
        case (k,v) => s"$k=${quoteRef(v)}"
        case v => quoteRef(v)
      }.mkString(",")
      var q = s"${tp}($argsString)"
      lhs.sym.ctx match {
        case SrcCtx(dir, file, line, column, None) =>
        case ctx => 
          q = src"""$q.ctx("${ctx}${lhs.sym.name.map {n => s":$n"}.getOrElse("")}")"""
      }
      q = src"""$q.name("${lhs}")"""
      q
    case x@AliasStm(lhs, alias, _) => 
      var q = quoteRef(alias)
      q = src"""$q.name("${lhs}")"""
      q
    case x => x.toString
  }

  protected def quoteRef(x:Any):String = quoteOrRemap(x)

  def emitblk[T](header:String)(block: => T):T = {
    open(s"$header {")
    val res = block
    close("} ")
    res
  }
}

