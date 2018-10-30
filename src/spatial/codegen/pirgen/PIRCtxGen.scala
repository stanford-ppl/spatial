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
    super.emitHelperFunction
    emit(s"def sctx(c:String):T = x.to[PIRNode].fold(x) { xx => xx.srcCtx(c); x }")
    emit(s"def name(c:String):T = x.to[PIRNode].fold(x) { xx => xx.name(c); x }")
    emit(s"def setMem(m:Memory):T = x.to[Access].fold(x) { xx => xx.order := m.accesses.size; xx.mem(m) ; x }")
  }

}
