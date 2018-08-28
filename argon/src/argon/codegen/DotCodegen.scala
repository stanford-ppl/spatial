package argon.codegen

import argon._

trait DotCodegen extends Codegen {
  override val lang: String = "dot"
  override val ext: String = "dot"

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    val inputs = rhs.inputs intersect syms(rhs.productIterator.filterNot(_.isInstanceOf[Block[_]])).toSeq
    if (rhs.blocks.nonEmpty) {
      emit(src"subgraph cluster_${lhs} {")
      emit(src"""label="${label(lhs, rhs)}"""")
      strMeta(lhs)

      if (inputs.nonEmpty) {
        emit(src"""$lhs [label="${label(lhs, rhs)}" color=red style=filled]""")
      }

      rhs.binds.filter(_.isBound).foreach{b =>
        emit(src"""$b [label="${b}"]""")
        emit(src"$lhs -> $b")
      }

      rhs.blocks.foreach(ret)

      emit(src"}")
    } else {
      emit(src"""$lhs [label="${label(lhs, rhs)}" ]""")
    }
    inputs.foreach { in => emitEdge(in, lhs) }
  }

  def label(lhs:Sym[_], rhs:Op[_]) = s"$lhs\n${rhs.getClass.getSimpleName}"

  def emitEdge(from:Sym[_], to:Sym[_]) = {
    emit(src"$from -> $to")
  }

  override protected def emitEntry(block: Block[_]): Unit = {
    open(src"digraph G {")
      gen(block)
    close(src"}")
  }
}
