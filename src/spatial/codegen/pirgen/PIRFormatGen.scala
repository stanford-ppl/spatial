package spatial.codegen.pirgen

import argon._
import argon.lang._
import argon.codegen.{Codegen, FileDependencies}
import spatial.metadata._
import spatial.metadata.memory._
import spatial.node._

import scala.collection.mutable

case class Lhs(sym:Sym[_], postFix:Option[String]=None)

trait PIRFormatGen extends Codegen {

  val typeMap = mutable.Map[Lhs, String]()

  implicit def sym_to_lhs(sym:Sym[_]) = Lhs(sym,None)

  def state(lhs:Lhs, tp:Option[String]=None)(rhs: Any) = {
    var rhsStr = src"""$rhs.sctx("${lhs.sym.ctx}")"""
    val tpStr = tp match {
      case Some(tp) => tp
      case None => rhsStr.split("\\(")(0)
    }
    (lhs.sym.name, lhs.postFix) match {
      case (Some(name), Some(postFix)) => rhsStr += src""".name("${name}_${postFix}")"""
      case (Some(name), None) => rhsStr += src""".name("${name}")"""
      case (None, Some(postFix)) => rhsStr += src""".name("${postFix}")"""
      case (None, None) => 
    }
    emitStm(lhs, tpStr, rhsStr)
  }

  def alias(lhs:Lhs)(rhsFunc: Lhs) = {
    val rhs = rhsFunc
    emitStm(lhs, typeMap(rhs), rhs)
  }

  def stateOrAlias(lhs:Lhs, tp:Option[String]=None)(rhs:Any):Unit = {
    rhs match {
      case rhs:Lhs if (typeMap.contains(rhs)) => alias(lhs)(rhs)
      case rhs:Sym[_] => stateOrAlias(lhs,tp)(Lhs(rhs))
      case rhs => state(lhs,tp)(rhs)
    }
  }

  def comment(lhs:Lhs, tp:String) = {
    lhs.sym match {
      case Def(LUTNew(dims, elems)) => s"[$tp] ${quoteOrRemap(lhs)} = LUTNew($dims, elems)"
      case Def(op) => s"[$tp] ${quoteOrRemap(lhs)} = $op"
      case lhs => s"[$tp] ${quoteOrRemap(lhs)}"
    }
  }

  def emitStm(lhs:Lhs, tp:String, rhsStr:Any):Unit = {
    emit(src"val $lhs = $rhsStr // ${comment(lhs, tp)}")
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
