package spatial.codegen.dotgen

import argon._
import scala.collection.mutable
import utils.io.files
import sys.process._
import scala.language.postfixOps

trait DotFlatCodegen extends DotCodegen {

  override def entryFile: String = s"Main.$ext"

  val nodes = mutable.ListBuffer[Sym[_]]()

  override protected def preprocess[R](b: Block[R]): Block[R] = {
    nodes.clear
    super.preprocess(b)
  }

  override protected def emitEntry(block: Block[_]): Unit = {
    open(src"digraph G {")
      gen(block)
      nodes.foreach { sym => emitInputs(sym) }
    close(src"}")
  }


  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    nodes += lhs
    if (rhs.blocks.nonEmpty) {
      emit(src"subgraph cluster_${lhs} {")
      emit(src"""${graphAttr(lhs).map { case (k,v) => s"$k=$v" }.mkString(" ")}""")
      strMeta(lhs)

      if (inputs(lhs).nonEmpty) emitNode(lhs)

      rhs.binds.filter(_.isBound).foreach{ b =>
        nodes += b
        emitNode(b)
        emitEdge(lhs, b, s"$lhs", s"$b")
      }

      rhs.blocks.foreach(ret)

      emit(src"}")
    } else {
      emitNode(lhs)
    }
  }

  override def nodeAttr(lhs:Sym[_]):Map[String,String] = lhs match {
    case lhs if blocks(lhs).nonEmpty => 
      super.nodeAttr(lhs) + ("URL" -> s""""file:///${out + files.sep + s"$lhs.svg"}"""")
    case lhs => super.nodeAttr(lhs)
  }

  override def graphAttr(lhs:Sym[_]):Map[String,String] = lhs match {
    case lhs if blocks(lhs).nonEmpty => 
      super.graphAttr(lhs) + ("URL" -> s""""file:///${out + files.sep + s"$lhs.svg"}"""")
    case lhs => super.graphAttr(lhs)
  }

}
