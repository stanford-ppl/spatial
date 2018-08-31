package spatial.codegen.dotgen

import argon._
import scala.collection.mutable
import utils.io.files
import sys.process._
import scala.language.postfixOps

trait DotCodegen extends argon.codegen.Codegen {
  override val lang: String = "dot"
  override val ext: String = "dot"


  def nodes:mutable.ListBuffer[Sym[_]]

  // Generate dot graphs to svg files
  override protected def postprocess[R](b: Block[R]): Block[R] = {
    files.listFiles(out, List(".dot")).foreach { file =>
      val path = out + files.sep + file.getName
      val outPath = path.replace(".dot",".svg")
      val exit = s"dot -Tsvg -o $outPath $path" !

      if (exit != 0) {
        info(s"Dot graph generation failed. Please install graphviz")
      }
    }
    super.postprocess(b)
  }

  final def emitNode(lhs:Sym[_]):Unit = emitNode(lhs, nodeAttr(lhs))

  def emitNode(lhs:Sym[_], nodeAttr:Map[String, String]):Unit = {
    emit(src"""$lhs [ ${nodeAttr.map { case (k,v) => s"$k=$v" }.mkString(" ")} ]""")
    inputGroups(lhs).foreach { case (name, inputs) =>
      emit(src""" ${lhs}_${name} [ label="$name" shape=invhouse]""")
      emit(src"${lhs}_${name} -> $lhs")
    }
  }

  def inputGroups(lhs:Sym[_]):Map[String, Seq[Sym[_]]] = Map.empty

  def inputs(lhs:Sym[_]):Seq[Sym[_]] = {
    lhs.op.map { rhs =>
      rhs.inputs intersect syms(rhs.productIterator.filterNot { field =>
        field.isInstanceOf[Block[_]]
      }).toSeq
    }.getOrElse(Nil)
  }

  def blocks(lhs:Sym[_]) = lhs.op.map { _.blocks }.getOrElse(Nil)

  def label(lhs:Sym[_]) = {
    lhs.op.fold {
      s"$lhs"
    } { rhs =>
      s"$lhs\\n${rhs.getClass.getSimpleName}"
    }
  }

  // Node Attributes
  def nodeAttr(lhs:Sym[_]):Map[String,String] = {
    var at = Map[String,String]()
    if (blocks(lhs).nonEmpty) {
      at += "color" -> "red"
      at += "style" -> "filled"
    }
    at += "label" -> s""""${label(lhs)}""""
    at
  }

  def edgeAttr(from:Sym[_], to:Sym[_]):Map[String,String] = Map.empty

  def graphAttr(lhs:Sym[_]):Map[String,String] = {
    var at = Map[String,String]()
    at += "label" -> s""""${label(lhs)}""""
    at
  }

  def emitEscapeEdge(from:Sym[_], to:Sym[_], fromAlias:String, toAlias:String):Unit = {
    if (nodes.contains(from) && nodes.contains(to)) emitEdge(from, to, fromAlias, toAlias)
  }

  def emitEdge(from:Sym[_], to:Sym[_], fromAlias:String, toAlias:String):Unit = {
    emitEdge(fromAlias, toAlias, edgeAttr(from, to))
  }

  def emitEdge(fromAlias:String, toAlias:String, at:Map[String,String]):Unit = {
    emit(src"$fromAlias -> $toAlias [ ${at.map { case (k,v) => s"$k=$v" }.mkString(" ")} ]")
  }

  def emitInputs(lhs:Sym[_]) = {
    val groups = inputGroups(lhs)
    groups.foreach { case (name, inputs) => 
      inputs.foreach { in =>
        emitEscapeEdge(in, lhs, src"$in", src"${lhs}_${name}")
      }
    }
    (inputs(lhs) diff groups.values.flatten.toSeq).foreach { in => 
      emitEscapeEdge(in, lhs, src"$in", src"$lhs")
    }
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = s"$c"

  override protected def quoteOrRemap(arg: Any): String = arg match {
    case arg:SrcCtx => s"$arg"
    case _ => super.quoteOrRemap(arg)
  }

}
