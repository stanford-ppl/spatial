package spatial.codegen.dotgen

import argon._
import scala.collection.mutable
import utils.io.files
import sys.process._
import scala.language.postfixOps

trait DotHierarchicalCodegen extends DotCodegen {

  override def entryFile: String = s"Top.$ext"

  val stack = mutable.Stack[mutable.ListBuffer[Sym[_]]]()
  def nodes = stack.head

  override def clearGen = {} // prevent clear flatGen

  override protected def emitEntry(block: Block[_]): Unit = {
    open(src"digraph G {")
      stack.push(mutable.ListBuffer.empty)
      gen(block)
      nodes.foreach { sym => sym.op.foreach { op => emitInputs(sym) } }
      stack.pop
    close(src"}")
  }


  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    if (blocks(lhs).nonEmpty) {
      emitNode(lhs)

      inGen(out, s"$lhs.dot") {
        open(src"digraph G {")
        stack.push(mutable.ListBuffer.empty)

        emit(src"subgraph cluster_${lhs} {")
        emit(src"""${graphAttr(lhs).map { case (k,v) => s"$k=$v" }.mkString(" ")}""")
        strMeta(lhs)

        emitNode(lhs)

        rhs.binds.filter(_.isBound).foreach{ b =>
          emitNode(b)
          emitEdge(lhs, b)
          nodes += b
        }

        rhs.blocks.foreach(ret)

        emit(src"}")
        nodes.foreach { sym => emitInputs(sym) }
        stack.pop

        close(src"}")
      }
    } else {
      emitNode(lhs)
    }
  }

  override def emitInputs(lhs:Sym[_]) = {
    val groups = inputGroups(lhs)
    groups.foreach { case (name, inputs) => 
      inputs.foreach { in =>
        //if (!nodes.contains(in))
        emit(src"""$in -> ${lhs}_${name}""")
      }
    }
    (inputs(lhs) diff groups.values.flatten.toSeq).foreach { in => 
      //if (!nodes.contains(in)) emit(src"""tmp_${in}_${lhs} [ label="$in" ]""")
      emitEdge(in, lhs)
    }
  }

  override def graphAttr(lhs:Sym[_]):Map[String,String] = lhs match {
    case lhs if blocks(lhs).nonEmpty => 
      //val binds = rhs.binds.filter(_.isBound)
      //val bindsStr = if (binds.nonEmpty) s"\nbinds:\n${binds.mkString("\n")}" else ""
      //("label" -> s""""${label(lhs, rhs)}\n${lhs.ctx}$bindsStr"""")
      super.graphAttr(lhs) + ("URL" -> s""""file:///${out + files.sep + s"$lhs.svg"}"""")
    case lhs => super.graphAttr(lhs)
  }

  override def nodeAttr(lhs:Sym[_]):Map[String,String] = lhs match {
    case lhs if blocks(lhs).nonEmpty => 
      super.nodeAttr(lhs) + ("URL" -> s""""file:///${out + files.sep + s"$lhs.svg"}"""")
    case lhs => super.nodeAttr(lhs)
  }


}
