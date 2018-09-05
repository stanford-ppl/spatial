package spatial.codegen.dotgen

import argon._
import scala.collection.mutable
import utils.io.files
import sys.process._
import scala.language.postfixOps

trait DotCodegen extends argon.codegen.Codegen {
  override val lang: String = "dot"
  override val ext: String = "dot"

  val stack = mutable.Stack[Scope]()
  def currScope = stack.head
  def nodes = currScope.nodes
  val scopeKeys = mutable.ListBuffer[Option[Sym[_]]]()
  val scopes = mutable.Map[Option[Sym[_]],Scope]()
  def scope(sym:Option[Sym[_]]) = scopes.getOrElseUpdate(sym, {
    scopeKeys += sym
    Scope(sym)
  })

  type Edge = (Sym[_], Sym[_], String, String) // (from, to, fromAlias, toAlias)

  case class Scope(sym:Option[Sym[_]]) {
    val _nodes = mutable.ListBuffer[Sym[_]]()
    def nodes = _nodes.toList
    def addNode(sym:Sym[_]) = if (!_nodes.contains(sym)) _nodes += sym
    val externNodes = mutable.ListBuffer[Sym[_]]()
    val edges = mutable.ListBuffer[(Sym[_], Sym[_], String, String)]()
    val fileName = sym match {
      case Some(sym) => src"$sym"
      case None => entryFile.replace(".dot","")
    }
    val htmlpath = s"${out}${files.sep}$fileName.html"

    def addExternNode(node:Sym[_]):this.type = { externNodes += node; this }
    def addEdge(edge:Edge):this.type = { edges += edge; this }

    // called when enter a new scope
    def begin(block: => Unit) = {
      enter {
        open(src"digraph G {")
        block
        nodes.foreach { sym => addInputs(sym) }
      }
    }

    // called when all scopes are done
    def end = {
      enter {
        externNodes.foreach { node => emitNode(node) }
        edges.groupBy { case (from,to,fromAlias,toAlias) => (fromAlias, toAlias) }.values.foreach { group =>
          // Avoid emit duplicated edges between nodes
          val (from, to, fromAlias, toAlias) = group.head
          emitEdge(from, to, fromAlias, toAlias)
        }
        close(src"}")
      }
    }

    def enter(block: => Unit) = {
      stack.push(this)
      inGen(out, s"${fileName}.dot") {
        block
      }
      stack.pop
    }
  }

  override protected def emitEntry(block: Block[_]): Unit = {
    scope(None).begin {
      gen(block)
    }
    scopes.values.foreach { scope => scope.end }
  }

  // Generate dot graphs to svg files
  override protected def postprocess[R](b: Block[R]): Block[R] = {
    files.listFiles(out, List(".dot")).foreach { file =>
      val path = out + files.sep + file.getName
      val outPath = path.replace(".dot",".html")
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
    var l = src"$lhs"
    lhs.name.foreach { name =>
      l += s"[$name]"
    }
    lhs.op.foreach { rhs =>
      l += s"\\n${rhs.getClass.getSimpleName}"
    }
    l 
  }

  // Node Attributes
  def nodeAttr(lhs:Sym[_]):Map[String,String] = {
    var at = Map[String,String]()
    if (blocks(lhs).nonEmpty) {
      at += "color" -> "red"
      at += "style" -> "filled"
    }
    at += "label" -> s""""${label(lhs)}""""
    at += "URL" -> s""""file:///${out + files.sep + src"IR.html#$lhs"}""""
    at
  }

  def edgeAttr(from:Sym[_], to:Sym[_]):Map[String,String] = Map.empty

  def graphAttr(lhs:Sym[_]):Map[String,String] = {
    var at = Map[String,String]()
    at += "label" -> s""""${label(lhs)}""""
    at
  }

  def addEdge(from:Sym[_], to:Sym[_], fromAlias:String, toAlias:String):Unit = {
    if (nodes.contains(from) && nodes.contains(to)) currScope.addEdge((from, to, fromAlias, toAlias))
  }

  def emitEdge(from:Sym[_], to:Sym[_], fromAlias:String, toAlias:String):Unit = {
    emitEdge(fromAlias, toAlias, edgeAttr(from, to))
  }

  def emitEdge(fromAlias:String, toAlias:String, at:Map[String,String]):Unit = {
    emit(src"$fromAlias -> $toAlias [ ${at.map { case (k,v) => s"$k=$v" }.mkString(" ")} ]")
  }

  def addInputs(lhs:Sym[_]) = {
    val groups = inputGroups(lhs)
    groups.foreach { case (name, inputs) => 
      inputs.foreach { in =>
        addEdge(in, lhs, src"$in", src"${lhs}_${name}")
      }
    }
    (inputs(lhs) diff groups.values.flatten.toSeq).foreach { in => 
      addEdge(in, lhs, src"$in", src"$lhs")
    }
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = s"$c"

  //override protected def quoteOrRemap(arg: Any): String = arg match {
    //case op:Op[_] => s"$op"
    //case _ => super.quoteOrRemap(arg)
  //}

}
