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
import scala.collection.mutable.{ListBuffer, Map}

case class IRDotCodegen(IR: State) extends Codegen with DotCommon {
  override val name: String = "IR Dot Printer"

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
    emit(s"subgraph cluster_${getNodeName(block.result)} {")
    open
    emit(s"color=blue;")
    emit(s"""label = "Block_${getNodeName(block.result)}" """)
    val b = super.visitBlock(block)
    close
    emit(s"}")
    b
  }

  override protected def visit(lhs: Sym[_], rhs: Op[_]): Unit = {
    println(s"[IRDotCodegen] visit $lhs, $rhs")
    emitNode(getNodeName(lhs), attributes)
    val blockEdgeAttr = DotAttr()
    blockEdgeAttr + ("color", "blue")

    rhs.inputs.foreach { in =>
//      emitEdge(getNodeName(lhs), getNodeName(in))
      emitEdge(getNodeName(in), getNodeName(lhs))
    }

    rhs match {
      case pcu: VPCU =>
        visitBlock(pcu.datapath)
//        emitEdge(getBlockName(pcu.datapath), getNodeName(lhs), blockEdgeAttr.label("datapath").+ ("lhead", getBlockName(pcu.datapath)))
      case pmu: VPMU =>
        pmu.rdPath.map { b => visitBlock(b) }
        pmu.wrPath.map { b => visitBlock(b) }
      case _ =>
    }
  }
}


