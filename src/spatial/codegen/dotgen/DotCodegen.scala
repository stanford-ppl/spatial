package spatial.codegen.dotgen

import argon._
import scala.collection.mutable
import utils.io.files._
import sys.process._
import scala.language.postfixOps

trait DotCodegen extends argon.codegen.Codegen {
  override val lang: String = "info"
  override val ext: String = "dot"

  val stack = mutable.Stack[Scope]()
  def currScope = stack.head
  def nodes = currScope.nodes
  val scopes = mutable.Map[Option[Sym[_]],Scope]()
  def scope(sym:Option[Sym[_]]) = scopes.getOrElseUpdate(sym, {
    Scope(sym)
  })

  override protected def preprocess[R](b: Block[R]): Block[R] = {
    stack.clear
    scopes.clear
    super.preprocess(b)
  }

  type Edge = (Sym[_], Sym[_], Map[Sym[_], String]) // (from, to, alias)

  case class Scope(sym:Option[Sym[_]]) {
    val _nodes = mutable.ListBuffer[Sym[_]]()
    def nodes = _nodes.toList
    def addNode(sym:Sym[_]) = if (!_nodes.contains(sym) && !sym.isConst) _nodes += sym
    val externNodes = mutable.ListBuffer[Sym[_]]()
    val edges = mutable.ListBuffer[Edge]()
    val fileName = sym match {
      case Some(sym) => src"$sym"
      case None => entryFile.replace(".dot","")
    }
    val htmlPath = s"${out}${sep}$fileName.html"
    val dotPath = s"${out}${sep}$fileName.dot"

    def addExternNode(node:Sym[_]):this.type = { if (!node.isConst) externNodes += node; this }
    def addEdge(edge:Edge):this.type = { 
      val (from, to, alias) = edge
      if (!from.isConst && !to.isConst) edges += edge
      this
    }

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
        edges.groupBy { case (from,to,alias) => (getAlias(from, alias), getAlias(to, alias)) }.values.foreach { group =>
          // Avoid emit duplicated edges between nodes
          val (from, to, alias) = group.head
          emitEdge(from, to, alias)
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
    scopes.foreach { case (_, scope) =>
      inGen(out, "dot.log") {
        var errLine = ""
        s"dot -Tsvg -o ${scope.htmlPath} ${scope.dotPath}" ! ProcessLogger (
          { line => emit(line) },
          { line => emit(line); errLine += line }
        )
        if (errLine.nonEmpty) {
          val msg = if (errLine.contains("triangulation failed")) "Graph too large"
          else if (errLine.contains("command not found")) "Please install graphviz."
          else if (errLine.contains("trouble in init_ran")) "Graph too large. Update to latest graphviz might fix this"
          else ""
          warn(s"Dot graph generation failed for ${scope.dotPath}. $msg Log in ${out + sep}dot.log")
        }
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
    at += "URL" -> s""""${s"IR.html#$lhs"}""""
    at
  }

  def edgeAttr(from:Sym[_], to:Sym[_]):Map[String,String] = Map.empty

  def graphAttr(lhs:Sym[_]):Map[String,String] = {
    var at = Map[String,String]()
    at += "label" -> s""""${label(lhs)}""""
    at
  }

  def addEdge(from:Sym[_], to:Sym[_], alias:Map[Sym[_], String]=Map.empty):Unit = {
    if (nodes.contains(from) && nodes.contains(to)) currScope.addEdge((from, to, alias))
  }

  def getAlias(node:Sym[_], alias:Map[Sym[_],String]) = alias.getOrElse(node, src"$node")

  def emitEdge(from:Sym[_], to:Sym[_], alias:Map[Sym[_],String]=Map.empty):Unit = {
    emitEdge(getAlias(from, alias), getAlias(to, alias), edgeAttr(from, to))
  }

  def emitEdge(fromAlias:String, toAlias:String, at:Map[String,String]):Unit = {
    emit(src"$fromAlias -> $toAlias [ ${at.map { case (k,v) => s"$k=$v" }.mkString(" ")} ]")
  }

  def addInputs(lhs:Sym[_]) = {
    val groups = inputGroups(lhs)
    groups.foreach { case (name, inputs) => 
      inputs.foreach { in =>
        addEdge(in, lhs, Map(lhs -> src"${lhs}_${name}"))
      }
    }
    (inputs(lhs) diff groups.values.flatten.toSeq).foreach { in => 
      addEdge(in, lhs)
    }
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = s"$c"

}
