package pcc.traversal
package codegen
package dot

import pcc.core._
import pcc.node._
import pcc.lang.memories.SRAM
import pcc.lang.Void
import pcc.lang.pir.{In, Out}
import pcc.node.pir.{Lanes, VPCU, VPMU}

import scala.language.implicitConversions
import scala.collection.mutable.{ListBuffer, Map, Set}

case class IRDotCodegen(IR: State) extends Codegen with DotCommon {
  override val name: String = "IR Dot Printer"
  override def filename: String = s"IRGraph.${ext}"
  override def ext = s"dot.$lang"

  override protected def quoteOrRemap(arg: Any): String = arg match {
    case s: SRAM[_] => s"$s"
    case s: Void => s"$s"
    case s: Lanes => s"$s"
    case s: Out[_] => s"$s"
    case s: In[_] => s"$s"
    case _ => super.quoteOrRemap(arg)
  }

  // Set the Dot attributes
  val attributes = DotAttr()
  attributes.shape(box)
            .style(filled)
            .color(black)
            .fill(white)
            .labelfontcolor(black)

  def getNodeName(sym: Sym[_]) = sym.op.map(o => o.productPrefix).getOrElse(sym.typeName) + s"_x${sym.id}"
  def getBlockName[R](block: Block[R]) = "cluster_" + getNodeName(block.result)

  override protected def visitBlock[R](block: Block[R]): Block[R] = {
    val subgraphAttr = DotAttr().style(filled)
              .color(blue)
              .fill(white)
              .label(s"Block_${getNodeName(block.result)}")
              .labelfontcolor(black)

    emitSubgraph(subgraphAttr) {
      super.visitBlock(block)
    }
    block
  }

  private def needsSubgraph(rhs: Op[_]): Boolean = rhs match {
    case pcu: VPCU => true
    case pmu: VPMU => true
    case _ => false
  }

  private def getSubgraphAttr(lhs: Sym[_], rhs: Op[_]): DotAttr = {
    val subgraphAttr = DotAttr()
    val color = getNodeColor(rhs)

    // Default attributes
    subgraphAttr.style(filled)
              .color(black)
              .fill(color)
              .label(getNodeName(lhs))
              .labelfontcolor(black)

  }

  private def getNodeAttr(lhs: Sym[_]): DotAttr = {
    val nodeAttr = DotAttr()
    val color = lhs.op match {
      case Some(x) => white
      case None => lightgrey
    }

    nodeAttr.style(filled)
              .shape(box)
              .color(black)
              .fill(color)
              .labelfontcolor(black)
  }

  private def visitCommon(lhs: Sym[_], rhs: Op[_]): Unit = {
    emitNode(getNodeName(lhs), getNodeAttr(lhs))
    rhs.binds.foreach { b =>
      emitNode(getNodeName(b), getNodeAttr(b))
      emitEdge(getNodeName(b), getNodeName(lhs))
    }
    rhs.inputs.foreach { in =>
      if (in.isBound) emitNode(getNodeName(in), getNodeAttr(in))
      emitEdge(getNodeName(in), getNodeName(lhs))
    }

    rhs match {
      case pcu: VPCU =>
        visitBlock(pcu.datapath)
      case pmu: VPMU =>
        pmu.rdPath.map { b => visitBlock(b) }
        pmu.wrPath.map { b => visitBlock(b) }
      case _ =>
    }
  }

  override protected def visit(lhs: Sym[_], rhs: Op[_]): Unit = {
    println(s"[IRDotCodegen] visit $lhs, $rhs, binds: ${rhs.binds}")

    if (needsSubgraph(rhs)) {
      emitSubgraph(getSubgraphAttr(lhs, rhs)) { visitCommon(lhs, rhs) }
    } else {
      visitCommon(lhs, rhs)
    }
  }
}


