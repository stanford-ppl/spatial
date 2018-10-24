package spatial.codegen.pirgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.metadata._
import spatial.metadata.memory._

import scala.collection.mutable

trait PIRCtxGen extends PIRCodegen {

  override def quoteRhs(lhs:Sym[_], rhs: Any) = {
    var q = ""
    q += src"""${super.quoteRhs(lhs, rhs)}.sctx("${lhs.ctx}")"""
    lhs.name.foreach { n => q += src""".name("${n}")""" }
    q
  }

  override def emitHelperFunction = {
    emit(s"def sctx(c:String):T = x match { case n:PIRNode => n.srcCtx := c; x; case _ => x }")
    emit(s"def name(c:String):T = x match { case n:PIRNode => n.name := c; x; case _ => x }")
  }

}
