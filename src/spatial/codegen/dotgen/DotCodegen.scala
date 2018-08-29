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

  //override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    //if (blocks(lhs).nonEmpty) {
      //emit(src"subgraph cluster_${lhs} {")
      //emit(src"""${graphAttr(lhs).map { case (k,v) => s"$k=$v" }.mkString(" ")}""")
      //strMeta(lhs)

      //if (inputs(lhs).nonEmpty) emitNode(lhs)

      //rhs.binds.filter(_.isBound).foreach{ b =>
        //emitNode(b)
        //emitEdge(lhs, b)
        //nodes += b
      //}

      //rhs.blocks.foreach(ret)

      //emit(src"}")
    //} else {
      //emitNode(lhs)
    //}
  //}

  def emitNode(lhs:Sym[_]):Unit = {
    val at = nodeAttr(lhs)
    emit(src"""$lhs [ ${at.map { case (k,v) => s"$k=$v" }.mkString(" ")} ]""")
    inputGroups(lhs).foreach { case (name, inputs) =>
      emit(src""" ${lhs}_${name} [ label="$name" shape=invhouse]""")
      emit(src"${lhs}_${name} -> $lhs")
    }
    nodes += lhs
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
      s"$lhs\n${rhs.getClass.getSimpleName}"
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

  def emitEdge(from:Sym[_], to:Sym[_]):Unit = {
    val at = edgeAttr(from, to)
    emit(src"$from -> $to [ ${at.map { case (k,v) => s"$k=$v" }.mkString(" ")} ]")
  }

  def emitInputs(lhs:Sym[_]) = {
    val groups = inputGroups(lhs)
    groups.foreach { case (name, inputs) => 
      inputs.foreach { in =>
        if (nodes.contains(in)) emit(src"""$in -> ${lhs}_${name}""")
      }
    }
    (inputs(lhs) diff groups.values.flatten.toSeq).foreach { in => 
      if (nodes.contains(in)) emitEdge(in, lhs)
    }
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = s"$c"

}
