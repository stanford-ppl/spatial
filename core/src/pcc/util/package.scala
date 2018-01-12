package pcc

import forge._
import pcc.core._
import pcc.data._

import org.apache.commons.lang3.StringEscapeUtils.escapeJava


/** Miscellaneous Utilities **/
package object util {

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
    metadata.all(lhs).foreach{m => dbgs(s" - ${m._1}: ${m._2}") }
  }
}
