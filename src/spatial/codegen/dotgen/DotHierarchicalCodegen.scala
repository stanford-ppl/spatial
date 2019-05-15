package spatial.codegen.dotgen

import argon._
import scala.collection.mutable
import utils.io.files._
import sys.process._
import scala.language.postfixOps
import spatial.metadata.control._
import spatial.metadata.bounds._

trait DotHierarchicalCodegen extends DotCodegen {

  override def entryFile: String = s"Top.$ext"

  //override def clearGen(): Unit = {} // everything cleared in DotFlatCodegen

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    currScope.addNode(lhs)
    emitNode(lhs)
    if (blocks(lhs).nonEmpty) {
      scope(Some(lhs)).begin {
        open(src"subgraph cluster_${lhs} {")
          emit(src"""${graphAttr(lhs).map { case (k,v) => s"$k=$v" }.mkString(" ")}""")
          //strMeta(lhs)
          rhs.binds.filter(_.isBound).foreach{ b =>
            currScope.addNode(b)
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
    val parent = sym.blk.s
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

  override def addEdge(from:Sym[_], to:Sym[_], alias:Map[Sym[_], String]=Map.empty):Unit = {
    if (!nodes.contains(from)) dbgblk(src"emitEscapeEdge($from, $to):"){
      assert(from.vecConst.isEmpty)
      val (shared, fromBranch, toBranch) = ancestryBetween(from, to)
      dbgs(src"from=$from")
      dbgs(src"to=$to")
      dbgs(src"shared=$shared")
      dbgs(src"fromBranch=$fromBranch")
      dbgs(src"toBranch=$toBranch")
      toBranch.filterNot{ sym => sym == to }.foreach { ancestor =>
        val toNode = toBranch(toBranch.indexOf(ancestor)-1)
        scope(Some(ancestor))
          .addExternNode(from)
          .addEdge(from, to, alias + (to -> getAlias(toNode, alias)))
      }
      fromBranch.filterNot{ sym => sym == from }.foreach { ancestor =>
        val fromNode = fromBranch(fromBranch.indexOf(ancestor)-1)
        scope(Some(ancestor))
          .addExternNode(to)
          .addEdge(from, to, alias + (from -> getAlias(fromNode, alias)))
      }
      scope(shared.headOption)
        .addEdge(from, to, alias + (from -> getAlias(fromBranch.last, alias)) + (to -> getAlias(toBranch.last, alias)))
    } else {
      currScope.addEdge(from, to, alias)
    }
  }

  override def nodeAttr(lhs:Sym[_]):Map[String,String] = lhs match {
    case lhs if blocks(lhs).nonEmpty => 
      super.nodeAttr(lhs) + ("URL" -> s""""${scope(Some(lhs)).fileName}.html"""")
    case lhs => super.nodeAttr(lhs)
  }

  override def graphAttr(lhs:Sym[_]):Map[String,String] = lhs match {
    case lhs if blocks(lhs).nonEmpty => 
      // navigate back to parent graph, or flat dot graph if already at top level
      super.graphAttr(lhs) + ("URL" -> s""""${scope(lhs.blk.s).fileName}.html"""")
    case lhs => super.graphAttr(lhs)
  }

}
