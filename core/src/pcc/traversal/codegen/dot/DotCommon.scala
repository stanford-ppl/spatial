package pcc.traversal
package codegen
package dot

import pcc.core._

import scala.language.implicitConversions
import scala.collection.mutable.{ListBuffer, Map, Set}
import pcc.node.pir.{Lanes, VPCU, VPMU, VectorBus}
import pcc.spade.node.{PCUModule, PMUModule}

trait DotCommon { this: Codegen =>
  private val regex = "\\[[0-9]*\\]".r


  def q(s: Any): String = regex.replaceAllIn(quoteOrRemap(s), "")

  val lang = "dot"
  def ext = s"$lang"
  def useOrtho = false

  val emittedNodes = Set[Any]()
  val edges = ListBuffer[() => Unit]()

  def rankdir = "BT"  // Direction in which graph is laid out
  override protected def preprocess[R](block: Block[R]): Block[R] = {
    emit("digraph G {")
    open
    emit(s"rankdir=$rankdir;")
    emit("labelloc=\"t\"")
    emit(s"""label="${filename}"""")
    if (useOrtho) emit(s"splines=ortho")
    block
  }

  override protected def postprocess[R](block: Block[R]): Block[R] = {
    edges.foreach {_()}
    close
    emit("}")
    block
  }

  def getNodeColor(rhs: Op[_]) = rhs match {
    case pcu: VPCU => indianred
    case pcu: PCUModule => indianred
    case pmu: VPMU => cadetblue
    case pmu: PMUModule => cadetblue
    case _ => white
  }

  def emitSubgraph(attr: DotAttr)(f: => Any): Unit = {
    emit(s"subgraph cluster_${attr.attrMap("label")._1} {")
    open
    attr.attrMap.keys.foreach { k =>
      if (attr.attrMap(k)._2) emit(s"$k=${attr.attrMap(k)._1}")
      else emit(s"$k=${attr.attrMap(k)._1}")
    }
    f
    close
    emit("}")
  }
  def emitNode(n: Any, attr: DotAttr): Unit = {
    if (!emittedNodes.contains(n)) {
      emit(src"""$n [${attr.list}];""")
      emittedNodes += n
    }
  }
  def emitNode(n: Any, label: String): Unit = emitNode(n, DotAttr().label(label))
  def emitNode(n: Any, label: Any, attr: DotAttr): Unit = emitNode(n, attr.label(label))

  def emitEdge(from: Any, to: Any, attr: DotAttr): Unit = {
    edges += (() => emit(src"""$from -> $to ${if (attr.attrMap.nonEmpty) s"[${attr.list}]" else ""}"""))
  }
  def emitEdge(from: Any, to: Any, label: String): Unit = emitEdge(from, to, DotAttr().label(label))
  def emitEdge(from: Any, to: Any): Unit = emitEdge(from, to, DotAttr())
  def emitEdge(from: Any, ffield: Any, to: Any, tfield: Any): Unit = emitEdge(s"$from:$ffield", s"$to:$tfield")

  def emitEdge(from: Any, ffield: Any, to: Any, tfield: Any, attr: DotAttr): Unit = {
    emitEdge(s"$from:$ffield", s"$to:$tfield", attr)
  }
  def emitEdge(from: Any, ffield: Any, fd: String, to: Any, tfield: Any, td: String): Unit = {
    emitEdge(s"$from:$ffield:$fd", s"$to:$tfield:$td")
  }

  /*def emitSubGraph(n: Any, label:Any)(block: =>Any):Unit = {
    emitSubGraph(n, DotAttr().label(label.toString))(block)
  }
  def emitSubGraph(n:Any, attr:DotAttr)(block: =>Any):Unit = {
    emitBlock(s"""subgraph cluster_${n}""") {
      emitln(attr.expand)
      block
    }
  }*/

  class DotAttr() {
    val attrMap: Map[String, (String, Boolean)] = Map.empty
    val graphAttrMap: Map[String, String] = Map.empty

    def append(key: String, value: String, quote: Boolean = true):DotAttr = { attrMap(key) = (value, quote); this}

    def shape(s: Shape): DotAttr = { this.append("shape", s.field); this }
    def color(s: Color): DotAttr = { this.append("color", s.field); this }
    def fill(s: Color): DotAttr = { this.append("fillcolor", s.field); this }
    def labelfontcolor(s: Color): DotAttr = { this.append("labelfontcolor", s.field); this }
    def style(ss: Style*): DotAttr = { this.append("style", ss.map(_.field).mkString(",")); this }
    def graphStyle(s: Style): DotAttr = { graphAttrMap += "style" -> s"${s.field}"; this }
    def label(s: Any, quote: Boolean = true): DotAttr = { this.append("label", s.toString, quote); this }
//    def label: Option[String] = { iattrMap.get("label")._1 }
    def dir(s: Direction): DotAttr = { this.append("dir", s.field); this }
    def pos(coord: (Double,Double)): DotAttr = { this.append("pos", s"${coord._1},${coord._2}!"); this }

    def elements:List[String] = {
      var elems = attrMap.map{case (k,v) => if (v._2) s"""$k="${v._1}"""" else s"""$k=${v._1}""" }.toList
      if (graphAttrMap.nonEmpty)
        elems = elems :+ s"graph[${graphAttrMap.map{case(k,v) => s"""$k="$v"""" }.mkString(",")}]"
      elems
    }
    def list: String = elements.mkString(",")
    def expand: String = elements.mkString(";")
  }
  object DotAttr {
    def apply(): DotAttr = new DotAttr()
    def copy(attr: DotAttr): DotAttr = {
      val newAttr = DotAttr()
      attr.attrMap.foreach { e => newAttr.append(e._1, e._2._1, e._2._2) }
      newAttr
    }
  }
  trait DotField { val field:String }
  case class Shape(field: String) extends DotField
  case class Color(field: String) extends DotField
  case class Style(field: String) extends DotField
  case class Direction(field: String) extends DotField

  val Mrecord   = Shape("Mrecord")
  val box       = Shape("box")
  val ellipse   = Shape("ellipse")
  val circle    = Shape("circle")

  val filled    = Style("filled")
  val bold      = Style("bold")
  val dashed    = Style("dashed")
  val rounded   = Style("rounded")
  val dotted    = Style("dotted")

  val white     = Color("white")
  val black     = Color("black")
  val lightgrey = Color("lightgrey")
  val grey = Color("grey")
  val hexagon   = Color("hexagon")
  val gold      = Color("gold")
  val limegreen = Color("limegreen")
  val blue      = Color("blue")
  val red       = Color("red")
  val indianred = Color("indianred1")
  val cyan      = Color("cyan4")
  val darkolivegreen = Color("darkolivegreen")
  val chocolate4 = Color("chocolate4")
  val cadetblue = Color("cadetblue")
  val chocolate = Color("chocolate")
  val chocolate1 = Color("chocolate1")
  val lemonchiffon = Color("lemonchiffon")
  val teal = Color("teal")

  val both = Direction("both")

  implicit def field_to_string(f: DotField): String = f.field

}


