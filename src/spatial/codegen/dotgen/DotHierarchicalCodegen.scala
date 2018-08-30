package spatial.codegen.dotgen

import argon._
import scala.collection.mutable
import utils.io.files
import sys.process._
import scala.language.postfixOps
import spatial.metadata.control._

trait DotHierarchicalCodegen extends DotCodegen {

  override def entryFile: String = s"Top.$ext"

  case class Scope(sym:Option[Sym[_]]) {
    val nodes = mutable.ListBuffer[Sym[_]]()
    val fileName = sym match {
      case Some(sym) => src"$sym"
      case None => s"Top"
    }
    val svgpath = s"${out}${files.sep}$fileName.svg"
  }

  val stack = mutable.Stack[Scope]()
  def currScope = stack.head
  def nodes = currScope.nodes
  val scopes = mutable.ListBuffer[Scope]()

  override def clearGen = {} // prevent clear flatGen

  override protected def emitEntry(block: Block[_]): Unit = {
    withScope(None) {
      gen(block)
    }
    scopes.foreach { scope =>
      inGen(out, s"${scope.fileName}.dot") {
        close(src"}")
      }
    }
    scopes.clear
  }

  def withScope(sym:Option[Sym[_]])(block: => Unit) = {
    val scope = Scope(sym)
    inGen(out, s"${scope.fileName}.dot") {
      open(src"digraph G {")
        stack.push(scope)
        block
        nodes.foreach { sym => sym.op.foreach { op => emitInputs(sym) } }
        scopes += stack.pop
    }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    nodes += lhs
    emitNode(lhs)
    if (blocks(lhs).nonEmpty) {
      withScope(Some(lhs)) {
        open(src"subgraph cluster_${lhs} {")
          emit(src"""${graphAttr(lhs).map { case (k,v) => s"$k=$v" }.mkString(" ")}""")
          //strMeta(lhs)
          val bounds = rhs.binds.filter(_.isBound)
          //if (bounds.nonEmpty) emitNode(lhs)
          bounds.foreach{ b =>
            nodes += b
            emitNode(b)
            //emitEdge(lhs, b, src"$lhs", src"$b")
          }
          rhs.blocks.foreach(ret)
        close(src"}")
      }
    }
  }

  /*
   * Accesstor symbols of enclosing blocks. Include sym
   * */
  def ancestors(sym:Sym[_]):List[Sym[_]] = {
    val parent = sym match {
      case sym if sym.isBound => sym.parent.s
      case _ => sym.blk.s
    }
    sym :: parent.toList.flatMap { parent => ancestors(parent) }
  }

  /*
   * returns (shared, branchA, branchB) where branchA and branchB are ancesstors of a and b until their
   * lca, and shared is the ancesstors that shared by a and b. All in ascending order
   * */
  def ancestryBetween(a:Sym[_], b:Sym[_]):(List[Sym[_]], List[Sym[_]], List[Sym[_]]) = {
    val branchA = ancestors(a)
    val branchB = ancestors(b)
    val shared = (branchA intersect branchB)
    (shared, branchA.filterNot(shared.contains(_)), branchB.filterNot(shared.contains(_)))
  }

  def emitEscapeEdge(in:Sym[_], lhs:Sym[_]) = {
    if (!nodes.contains(in)) dbgblk(src"emitEscapeEdge($in, $lhs):"){
      val (shared, branchIn, branchLhs) = ancestryBetween(in, lhs)
      dbgs(src"in=$in")
      dbgs(src"lhs=$lhs")
      dbgs(src"shared=$shared")
      dbgs(src"branchIn=$branchIn")
      dbgs(src"branchLhs=$branchLhs")
      emitNode(in)
      branchLhs.filterNot{ sym => sym == lhs || currScope.sym.fold(false){ _ == sym } }.foreach { ancestor =>
        inGen(out, s"${Scope(Some(ancestor)).fileName}.dot") {
          emitNode(in)
          emitEdge(in, lhs, src"$in", src"${branchLhs(branchLhs.indexOf(ancestor)-1)}")
        }
      }
      val lca = shared.headOption
      inGen(out, s"${Scope(lca).fileName}.dot") {
        emitEdge(in, lhs, src"${branchIn.last}", src"${branchLhs.last}")
      }
    }
  }

  override def emitInputs(lhs:Sym[_]) = {
    val groups = inputGroups(lhs)
    groups.foreach { case (name, inputs) => 
      inputs.foreach { in =>
        emitEscapeEdge(in, lhs)
        emit(src"""$in -> ${lhs}_${name}""")
      }
    }
    (inputs(lhs) diff groups.values.flatten.toSeq).foreach { in => 
      emitEscapeEdge(in, lhs)
      emitEdge(in, lhs, src"$in", src"$lhs")
    }
  }

  override def graphAttr(lhs:Sym[_]):Map[String,String] = lhs match {
    case lhs if blocks(lhs).nonEmpty => 
      // navigate back to parent graph, or flat dot graph if already at top level
      super.graphAttr(lhs) + ("URL" -> s""""file:///${Scope(lhs.blk.s).svgpath}"""")
    case lhs => super.graphAttr(lhs)
  }

  override def nodeAttr(lhs:Sym[_]):Map[String,String] = lhs match {
    case lhs if blocks(lhs).nonEmpty => 
      super.nodeAttr(lhs) + ("URL" -> s""""file:///${Scope(Some(lhs)).svgpath}"""")
    case lhs => super.nodeAttr(lhs)
  }

}
