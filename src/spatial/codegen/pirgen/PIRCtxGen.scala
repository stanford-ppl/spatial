package spatial.codegen.pirgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.metadata._
import spatial.metadata.memory._

import scala.collection.mutable

trait PIRCtxGen extends PIRCodegen {

  override def quoteRhs(lhs:Sym[_], rhs: => Any) = {
    var q = ""
    q += src"""${super.quoteRhs(lhs, rhs)}.ctx("${lhs.ctx}")"""
    lhs.name.foreach { n => q += src""".name("${n}")""" }
    q
  }

  override def emitHelperFunction = {
    emit(s"def ctx(c:String):T = x match { case x:IR => srcCtxOf(x) = c; x; case _ => x }")
    emit(s"def name(n:String):T = x match { case x:IR => nameOf(x) = n; x; case _ => x }")
  }

}
