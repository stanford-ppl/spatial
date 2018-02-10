package pcc

import forge._
import pcc.core._
import pcc.data._

/** Miscellaneous Utilities **/
package object util {
  def isPow2(x: Int): Boolean = (x & (x-1)) == 0

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

  def escapeString(raw: String): String = "\"" + raw.flatMap(escapeChar) + "\""
  def escapeChar(raw: Char): String = raw match {
    case '\b' => "\\b"
    case '\t' => "\\t"
    case '\n' => "\\n"
    case '\f' => "\\f"
    case '\r' => "\\r"
    case '"'  => "\\\""
    case '\'' => "\\\'"
    case '\\' => "\\\\"
    case c    if c.isControl => "\\0" + Integer.toOctalString(c.toInt)
    case c    => String.valueOf(c)
  }


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

  @stateful def strMeta(lhs: Sym[_]): Unit = {
    lhs.name.foreach{name => dbgs(s" - Name: $name") }
    if (lhs.prevNames.nonEmpty) {
      val aliases = lhs.prevNames.map{case (tx,alias) => s"$tx: $alias" }.mkString(", ")
      dbgs(s" - Aliases: $aliases")
    }
    dbgs(s" - Type: ${lhs.typeName}")
    dbgs(s" - SrcCtx: ${lhs.ctx}")
    metadata.all(lhs).foreach{case (k,m) => dbgss(s" - $k: $m") }
  }
}
