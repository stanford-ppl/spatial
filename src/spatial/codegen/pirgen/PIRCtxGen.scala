package spatial.codegen.pirgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.metadata._
import spatial.metadata.memory._

import scala.collection.mutable

trait PIRCtxGen extends PIRCodegen {

  override def quoteRhs(lhs:Sym[_], rhs: => Any) = {
    src"""${super.quoteRhs(lhs, rhs)}.name("${lhs}").ctx("${lhs.ctx}")"""
  }

  override def emitHeader(): Unit = {
    super.emitHeader()
    inGen(out, "AccelMain.scala") {
      emit(s"implicit class CtxHelper[T<:IR](x:T) { def ctx(c:String):T = { srcCtxOf(x) = c; x } }")
      emit(s"implicit class NameHelper[T<:IR](x:T) { def name(n:String):T = { nameOf(x) = n; x } }")
    }
  }

}
