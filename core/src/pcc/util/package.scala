package pcc

import forge._
import pcc.core._
import pcc.data._

import org.apache.commons.lang3.StringEscapeUtils.escapeJava

/** Miscellaneous Utilities **/
package object util {
  /**
    * Returns an iterator over the multi-dimensional space `dims`.
    * If dims is empty, trivially returns an iterator with only one element (Nil)
    */
  def multiLoop(dims: Seq[Int]): Iterator[Seq[Int]] = {
    val ndims = dims.length
    val prods = List.tabulate(ndims) { i => dims.slice(i + 1, ndims).product }
    val total = dims.product
    (0 until total).iterator.map{x => Seq.tabulate(ndims){d => (x / prods(d)) % dims(d) } }
  }
  def multiLoopWithIndex(dims: Seq[Int]): Iterator[(Seq[Int],Int)] = multiLoop(dims).zipWithIndex

  def escapeString(raw: String): String = "\"" + escapeJava(raw) + "\""
  def escapeChar(raw: Char): String = "'"+escapeJava(raw.toString)+"'"

  def escapeConst(x: Any): String = x match {
    case c: String => escapeString(c)
    case c: Char => escapeChar(c)
    case c => c.toString
  }

  def getStackTrace(start: Int, end: Int): String = {
    val curThread = Thread.currentThread()
    val trace = curThread.getStackTrace
    trace.slice(start,end).map("" + _).mkString("\n")
  }

  def getStackTrace: String = getStackTrace(1, 5)

  @stateful def strMeta(lhs: Sym[_]) {
    lhs.name.foreach{name => dbgs(s" - Name: $name") }
    if (lhs.prevNames.nonEmpty) {
      val aliases = lhs.prevNames.map{case (tx,alias) => s"$tx: $alias" }.mkString(", ")
      dbgs(s" - Aliases: $aliases")
    }
    dbgs(s" - Type: ${lhs.typeName}")
    dbgs(s" - SrcCtx: ${lhs.ctx}")
    metadata.all(lhs).foreach{case (k,m) => dbgs(s" - $k: $m") }
  }
}
