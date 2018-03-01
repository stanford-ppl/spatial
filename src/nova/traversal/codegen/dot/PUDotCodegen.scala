package nova.traversal
package codegen
package dot

import core._
import core.passes.Codegen
import spatial.lang._
import pir.lang._
import pir.node._

import scala.language.implicitConversions
import scala.collection.mutable.{HashMap, ListBuffer, Set}

case class PUDotCodegen(IR: State) extends Codegen with DotCommon {
  override def filename: String = s"PUGraph.${ext}"
  override def ext = s"dot.$lang"

  override def rankdir = "LR"
  override def useOrtho = true
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


  private def getPCUTable() = {
s"""
pcu00 [shape=plaintext, label=<
<table  CELLBORDER="1" CELLSPACING="0">
  <tr>
    <TD colspan="4">PCU00</TD>
  </tr>
  <tr>
    <TD HEIGHT="20" WIDTH="50" FIXEDSIZE="true" BGCOLOR="white" PORT="in_0">in_0</TD>
    <TD HEIGHT="20" WIDTH="50" FIXEDSIZE="true" BGCOLOR="gray" PORT="out_0">out_0</TD>
  </tr>
  <tr>
    <td HEIGHT="20" WIDTH="50" FIXEDSIZE="true" BGCOLOR="gray" PORT="in_1">in_1</td>
    <td HEIGHT="20" WIDTH="50" FIXEDSIZE="true" BGCOLOR="white" PORT="out_1">out_1</td>
  </tr>
  <tr>
    <td HEIGHT="20" WIDTH="50" FIXEDSIZE="true" BGCOLOR="gray" PORT="in_2">in_2</td>
    <td HEIGHT="20" WIDTH="50" FIXEDSIZE="true" BGCOLOR="gray" PORT="out_2">out_2</td>
  </tr>
  <tr>
    <td HEIGHT="20" WIDTH="50" FIXEDSIZE="true" BGCOLOR="gray" PORT="in_3">in_3</td>
    <td HEIGHT="20" WIDTH="50" FIXEDSIZE="true" BGCOLOR="gray" PORT="out_3">out_3</td>
  </tr>
  <tr>
    <TD colspan="4">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0">
        <tr><td HEIGHT="20" BGCOLOR="yellow">FixAdd_x133</td></tr>
        <tr><td HEIGHT="20" BGCOLOR="yellow">FixMul</td></tr>
        <tr><td HEIGHT="20" BGCOLOR="yellow">FixAdd</td></tr>
        <tr><td HEIGHT="20" BGCOLOR="yellow">FixAdd</td></tr>
        <tr><td HEIGHT="20" BGCOLOR="yellow">op1</td></tr>
        <tr><td HEIGHT="20" BGCOLOR="yellow">op2</td></tr>
        <tr><td HEIGHT="20" BGCOLOR="yellow">op2</td></tr>
      </table>
    </TD>

  </tr>
</table>
>];
"""
  }

  private def getNodeAttr(lhs: Sym[_]): DotAttr = {
    val nodeAttr = DotAttr()
    val color = lhs.op match {
      case Some(x) => getNodeColor(x)
      case None => lightgrey
    }

    nodeAttr.style(filled)
              .shape(box)
              .color(black)
              .fill(color)
              .labelfontcolor(black)
  }

  val boundsToPUMap = HashMap[Sym[_], Sym[_]]()

  private def visitCommon(lhs: Sym[_], rhs: Op[_]): Unit = {
    rhs.binds.foreach { b =>
      boundsToPUMap(b) = lhs
    }

    rhs match {
      case pcu: VPCU =>
        emitNode(getNodeName(lhs), getNodeAttr(lhs))
      case pmu: VPMU =>
        emitNode(getNodeName(lhs), getNodeAttr(lhs))
      case vb: VectorBus[_] =>
        emitEdge(getNodeName(boundsToPUMap(vb.out)), getNodeName(boundsToPUMap(vb.in)))
      case _ =>
    }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    dbgs(s"[PUDotCodegen] visit $lhs, $rhs, binds: ${rhs.binds}")

    if (needsSubgraph(rhs)) {
      emitSubgraph(getSubgraphAttr(lhs, rhs)) { visitCommon(lhs, rhs) }
    }
    else {
      visitCommon(lhs, rhs)
    }
  }
}


