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
    lhs.name.foreach { n => q += src""".name("${lhs.name}")""" }
    q
  }

  override def emitAccelHeader: Unit = {
    super.emitAccelHeader
    emit(s"implicit class CtxHelper[T<:IR](x:T) { def ctx(c:String):T = { srcCtxOf(x) = c; x } }")
    emit(s"implicit class NameHelper[T<:IR](x:T) { def name(n:String):T = { nameOf(x) = n; x } }")
  }

}
