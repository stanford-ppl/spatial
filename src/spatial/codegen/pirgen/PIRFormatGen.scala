package spatial.codegen.pirgen

import argon._
import argon.lang._
import argon.codegen.{Codegen, FileDependencies}
import spatial.metadata._
import spatial.metadata.memory._

import scala.collection.mutable

case class Lhs(sym:Sym[_], postFix:Option[String]=None)

trait PIRFormatGen extends Codegen {

  val typeMap = mutable.Map[Lhs, String]()

  def quoteRhs(lhs:Sym[_], rhs: => Any) = src"""$rhs"""

  implicit def sym_to_lhs(sym:Sym[_]) = Lhs(sym,None)

  def state(lhs:Lhs, tp:Option[String]=None)(rhs: => Any) = {
    val rhsStr = quoteRhs(lhs.sym, rhs)
    val tpStr = tp match {
      case Some(tp) => tp
      case None => rhsStr.split("\\(")(0)
    }
    emitStm(lhs, tpStr, rhsStr)
  }

  def alias(lhs:Lhs)(rhsFunc: => Lhs) = {
    val rhs = rhsFunc
    emitStm(lhs, typeMap(rhs), rhs)
  }

  def comment(lhs:Sym[_]) = {
    lhs match {
      case Def(op) => src"$lhs = $op"
      case lhs => src"$lhs"
    }
  }

  def emitStm(lhs:Lhs, tp:String, rhsStr:Any):Unit = {
    emit(src"val $lhs = $rhsStr // ${comment(lhs.sym)}")
    typeMap += lhs -> tp
  }

  def qdef(sym:Sym[_]) = sym.op match {
    case Some(op) => src"$sym = $op"
    case None => src"$sym"
  }

  def emitBlk(header:Any)(blk: => Unit) = {
    open(src"$header {")
    blk
    close(src"}")
  }

}
