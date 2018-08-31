package spatial.codegen.dotgen

import argon._
import scala.collection.mutable
import utils.io.files
import sys.process._
import scala.language.postfixOps
import spatial.metadata.control._

trait DotHierarchicalCodegen extends DotCodegen {

  override def entryFile: String = s"Top.$ext"

  override def clearGen = {} // prevent clear flatGen

  case class Scope(sym:Option[Sym[_]]) {
    val nodes = mutable.ListBuffer[Sym[_]]()
    val edges = mutable.Map[(String, String), Map[String,String]]()
    val fileName = sym match {
      case Some(sym) => src"$sym"
      case None => s"Top"
    }
    val svgpath = s"${out}${files.sep}$fileName.svg"
    def addEdge(from:Sym[_], to:Sym[_], fromAlias:String, toAlias:String):Unit = {
      edges += ((fromAlias, toAlias) -> edgeAttr(from, to))
    }

    // called when enter a new scope
    def begin(block: => Unit) = {
      enter {
        open(src"digraph G {")
        block
        nodes.foreach { sym => sym.op.foreach { op => emitInputs(sym) } }
      }
    }

    // called when all scopes are done
    def end = {
      // Avoid emitint duplicated edges between nodes
      enter {
        edges.foreach { case ((from, to), attr) => 
          emitEdge(from, to, attr)
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

  val stack = mutable.Stack[Scope]()
  def currScope = stack.head
  def nodes = currScope.nodes
  val scopes = mutable.Map[Option[Sym[_]],Scope]()
  def scope(sym:Option[Sym[_]]) = scopes.getOrElseUpdate(sym, Scope(sym))

  override protected def emitEntry(block: Block[_]): Unit = {
    scope(None).begin {
      gen(block)
    }
    scopes.values.foreach { scope => scope.end }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    nodes += lhs
    emitNode(lhs)
    if (blocks(lhs).nonEmpty) {
      scope(Some(lhs)).begin {
        open(src"subgraph cluster_${lhs} {")
          emit(src"""${graphAttr(lhs).map { case (k,v) => s"$k=$v" }.mkString(" ")}""")
          //strMeta(lhs)
          val bounds = rhs.binds.filter(_.isBound)
          bounds.foreach{ b =>
            nodes += b
            emitNode(b)
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

  override def emitEscapeEdge(from:Sym[_], to:Sym[_], fromAlias:String, toAlias:String) = {
    if (!nodes.contains(from)) dbgblk(src"emitEscapeEdge($from, $to):"){
      val (shared, fromBranch, toBranch) = ancestryBetween(from, to)
      dbgs(src"from=$from")
      dbgs(src"to=$to")
      dbgs(src"shared=$shared")
      dbgs(src"fromBranch=$fromBranch")
      dbgs(src"toBranch=$toBranch")
      toBranch.filterNot{ sym => sym == to }.foreach { ancestor =>
        scope(Some(ancestor)).enter {
          emitNode(from)
          emitEdge(from, to, src"$from", src"${toBranch(toBranch.indexOf(ancestor)-1)}")
        }
      }
      fromBranch.filterNot{ sym => sym == from }.foreach { ancestor =>
        scope(Some(ancestor)).enter {
          emitNode(to)
          emitEdge(from, to, src"${fromBranch(fromBranch.indexOf(ancestor)-1)}", src"$to")
        }
      }
      scope(shared.headOption).enter {
        emitEdge(from, to, src"${fromBranch.last}", src"${toBranch.last}")
      }
    } else {
      emitEdge(from, to, fromAlias, toAlias)
    }
  }

  override def emitEdge(from:Sym[_], to:Sym[_], fromAlias:String, toAlias:String):Unit = {
    currScope.addEdge(from, to, fromAlias, toAlias)
  }

  override def graphAttr(lhs:Sym[_]):Map[String,String] = lhs match {
    case lhs if blocks(lhs).nonEmpty => 
      // navigate back to parent graph, or flat dot graph if already at top level
      super.graphAttr(lhs) + ("URL" -> s""""file:///${scope(lhs.blk.s).svgpath}"""")
    case lhs => super.graphAttr(lhs)
  }

  override def nodeAttr(lhs:Sym[_]):Map[String,String] = lhs match {
    case lhs if blocks(lhs).nonEmpty => 
      super.nodeAttr(lhs) + ("URL" -> s""""file:///${scope(Some(lhs)).svgpath}"""")
    case lhs => super.nodeAttr(lhs)
  }

}
