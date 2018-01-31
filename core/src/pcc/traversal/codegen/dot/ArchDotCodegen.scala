package pcc.traversal
package codegen
package dot

import pcc.core._
import pcc.node._
import pcc.lang.memories.SRAM
import pcc.lang.Void
import pcc.lang.pir.{In, Out}
import pcc.node.pir.{Lanes, VectorBus}
import pcc.spade.node._

import scala.language.implicitConversions
import scala.collection.mutable.{ListBuffer, HashMap, Set}

case class ArchDotCodegen(IR: State) extends Codegen with DotCommon {
  override val name: String = "Arch Dot Printer"
  override def filename: String = s"ArchGraph.${ext}"
  override def ext = s"fdp.$lang"

  override def rankdir = "LR"
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

  def getNodeName(sym: Sym[_]) = sym match {
    case p: PCU =>
      val op = p.op.get.asInstanceOf[PCUModule]
      s"PCU_${op.x}_${op.y}"
    case p: PMU =>
      val op = p.op.get.asInstanceOf[PMUModule]
      s"PMU_${op.x}_${op.y}"
    case _ =>
      sym.op.map(o => o.productPrefix).getOrElse(sym.typeName) + s"_x${sym.id}"
  }
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
    case _ => false
  }

  private def getSubgraphAttr(lhs: Sym[_], rhs: Op[_]): DotAttr = {
    val subgraphAttr = DotAttr()
    val color = getNodeColor(rhs)

    // Default attributes
    subgraphAttr.style(filled)
              .color(black)
              .fill(color)
              .labelfontcolor(black)
              .label(getNodeName(lhs))

  }


  def wrapTag(s: String, tag: String) = s"<$tag>$s</$tag>"
  def asFields(l: List[String]) = {
//        <td>b_nw_1</td>
//        <td>b_nw_2</td>
    if (l.size == 0) List(wrapTag("", "td")) else l.map { s => wrapTag(s, "td") }
  }

  def asRows(l: List[String]) = {
//        <tr><td>b_nw_1</td></tr>
//        <tr><td>b_nw_2</td></tr>
    val atLeastOne = if (l.size == 0) List("") else l
    asFields(atLeastOne).map { f => wrapTag(f, "tr") }
  }

  private def getPMUTable(lhs: Sym[_], m: PMUModule) = {
s"""
<table  CELLBORDER="1" CELLSPACING="0">
  <tr>
    <TD colspan="1">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        ${asRows(m.vIO(NW).map{getNodeName(_)}).mkString("\n")}
      </table>
    </TD>
    <TD colspan="6">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        <tr>
        ${asFields(m.vIO(N).map{getNodeName(_)}).mkString("\n")}
        </tr>
      </table>
    </TD>
    <TD colspan="1">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        ${asRows(m.vIO(NE).map{getNodeName(_)}).mkString("\n")}
      </table>
    </TD>
  </tr>
  <tr>
    <TD colspan="1">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        ${asRows(m.vIO(W).map{getNodeName(_)}).mkString("\n")}
      </table>
    </TD>
    <TD colspan="6">
      <table BORDER="0" CELLBORDER="2" CELLSPACING="0" COLOR="white">
        <tr>
          <td COLSPAN="6" BGCOLOR="teal"><font color="white"><b>${getNodeName(lhs)}</b></font></td>
        </tr>
        <tr>
          <td HEIGHT="20" BGCOLOR="yellow">
            <table BORDER="0" CELLBORDER="0" CELLSPACING="0">
              <tr><td>x133</td></tr>
              <tr><td>*+</td></tr>
            </table>
          </td>
          <td HEIGHT="20" BGCOLOR="yellow">
            <table BORDER="0" CELLBORDER="0" CELLSPACING="0">
              <tr><td>x141</td></tr>
              <tr><td>+</td></tr>
            </table>
          </td>
          <td HEIGHT="20" BGCOLOR="yellow">
            <table BORDER="0" CELLBORDER="0" CELLSPACING="0">
              <tr><td>x133</td></tr>
              <tr><td>+</td></tr>
            </table>
          </td>
          <td HEIGHT="20" BGCOLOR="yellow">
            <table BORDER="0" CELLBORDER="0" CELLSPACING="0">
              <tr><td>x133</td></tr>
              <tr><td>+</td></tr>
            </table>
          </td>
          <td HEIGHT="20" BGCOLOR="yellow">
            <table BORDER="0" CELLBORDER="0" CELLSPACING="0">
              <tr><td>x133</td></tr>
              <tr><td>+</td></tr>
            </table>
          </td>
          <td HEIGHT="20" BGCOLOR="yellow">
            <table BORDER="0" CELLBORDER="0" CELLSPACING="0">
              <tr><td>x133</td></tr>
              <tr><td>+</td></tr>
            </table>
          </td>
        </tr>
      </table>
    </TD>
    <TD colspan="1">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        ${asRows(m.vIO(E).map{getNodeName(_)}).mkString("\n")}
      </table>
    </TD>
  </tr>
  <tr>
    <TD colspan="1">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        ${asRows(m.vIO(SW).map{getNodeName(_)}).mkString("\n")}
      </table>
    </TD>
    <TD colspan="6">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        <tr>
        ${asFields(m.vIO(S).map{getNodeName(_)}).mkString("\n")}
        </tr>
      </table>
    </TD>
    <TD colspan="1">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        ${asRows(m.vIO(SE).map{getNodeName(_)}).mkString("\n")}
      </table>
    </TD>
  </tr>
</table>
"""
  }

  private def getPCUTable(lhs: Sym[_], m: PCUModule) = {
s"""
<table  CELLBORDER="1" CELLSPACING="0">
  <tr>
    <TD colspan="1">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        ${asRows(m.vIO(NW).map{getNodeName(_)}).mkString("\n")}
      </table>
    </TD>
    <TD colspan="6">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        <tr>
        ${asFields(m.vIO(N).map{getNodeName(_)}).mkString("\n")}
        </tr>
      </table>
    </TD>
    <TD colspan="1">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        ${asRows(m.vIO(NE).map{getNodeName(_)}).mkString("\n")}
      </table>
    </TD>
  </tr>
  <tr>
    <TD colspan="1">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        ${asRows(m.vIO(W).map{getNodeName(_)}).mkString("\n")}
      </table>
    </TD>
    <TD colspan="6">
      <table BORDER="0" CELLBORDER="2" CELLSPACING="0" COLOR="white">
        <tr>
          <td COLSPAN="6" BGCOLOR="indianred"><font color="white"><b>${getNodeName(lhs)}</b></font></td>
        </tr>
        <tr>
          <td HEIGHT="20" BGCOLOR="yellow">
            <table BORDER="0" CELLBORDER="0" CELLSPACING="0">
              <tr><td>x133</td></tr>
              <tr><td>*+</td></tr>
            </table>
          </td>
          <td HEIGHT="20" BGCOLOR="yellow">
            <table BORDER="0" CELLBORDER="0" CELLSPACING="0">
              <tr><td>x141</td></tr>
              <tr><td>+</td></tr>
            </table>
          </td>
          <td HEIGHT="20" BGCOLOR="yellow">
            <table BORDER="0" CELLBORDER="0" CELLSPACING="0">
              <tr><td>x133</td></tr>
              <tr><td>+</td></tr>
            </table>
          </td>
          <td HEIGHT="20" BGCOLOR="yellow">
            <table BORDER="0" CELLBORDER="0" CELLSPACING="0">
              <tr><td>x133</td></tr>
              <tr><td>+</td></tr>
            </table>
          </td>
          <td HEIGHT="20" BGCOLOR="yellow">
            <table BORDER="0" CELLBORDER="0" CELLSPACING="0">
              <tr><td>x133</td></tr>
              <tr><td>+</td></tr>
            </table>
          </td>
          <td HEIGHT="20" BGCOLOR="yellow">
            <table BORDER="0" CELLBORDER="0" CELLSPACING="0">
              <tr><td>x133</td></tr>
              <tr><td>+</td></tr>
            </table>
          </td>
        </tr>
      </table>
    </TD>
    <TD colspan="1">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        ${asRows(m.vIO(E).map{getNodeName(_)}).mkString("\n")}
      </table>
    </TD>
  </tr>
  <tr>
    <TD colspan="1">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        ${asRows(m.vIO(SW).map{getNodeName(_)}).mkString("\n")}
      </table>
    </TD>
    <TD colspan="6">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        <tr>
        ${asFields(m.vIO(S).map{getNodeName(_)}).mkString("\n")}
        </tr>
      </table>
    </TD>
    <TD colspan="1">
      <table BORDER="0" CELLBORDER="1" CELLSPACING="0" COLOR="gray">
        ${asRows(m.vIO(SE).map{getNodeName(_)}).mkString("\n")}
      </table>
    </TD>
  </tr>
</table>
"""
  }

  def getTableLabel(tableStr: String) = {
s"""<
$tableStr
>"""
  }
  private def getNodeAttr(lhs: Sym[_]): DotAttr = {
    val nodeAttr = DotAttr()
    val color = lhs.op match {
      case Some(x) => getNodeColor(x)
      case None => lightgrey
    }

    lhs.op.map { op => op match {
      case p: PCUModule =>
        nodeAttr.append("pos", s"${p.x * 10}.0, ${p.y * 5}.0!")
        nodeAttr.label(getTableLabel(getPCUTable(lhs, p)), quote = false)
      case p: PMUModule =>
        nodeAttr.append("pos", s"${p.x * 10}.0, ${p.y * 5}.0!")
        nodeAttr.label(getTableLabel(getPMUTable(lhs, p)), quote = false)
    }}

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
      case pcu: PCUModule =>
        emitNode(getNodeName(lhs), getNodeAttr(lhs))
      case pmu: PMUModule =>
        emitNode(getNodeName(lhs), getNodeAttr(lhs))
      case vb: VectorBus[_] =>
        emitEdge(getNodeName(boundsToPUMap(vb.out)), getNodeName(boundsToPUMap(vb.in)))
      case _ =>
    }
  }

  override protected def visit(lhs: Sym[_], rhs: Op[_]): Unit = {
    println(s"[ArchDotCodegen] visit $lhs, $rhs, binds: ${rhs.binds}")

    if (needsSubgraph(rhs)) {
      emitSubgraph(getSubgraphAttr(lhs, rhs)) { visitCommon(lhs, rhs) }
    } else {
      visitCommon(lhs, rhs)
    }
  }
}


