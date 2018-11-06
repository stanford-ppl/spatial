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

}
