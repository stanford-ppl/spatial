package forge

import implicits.collections._

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

  def plural(x: Int, sing: String): String = if (x == 1) sing else sing+"s"
  def plural(x: Int, sing: String, plur: String): String = if (x == 1) sing else plur
  def conj(xs: Seq[String]): String = {
    if (xs.isEmpty) ""
    else if (xs.lengthIs(1)) xs.head
    else if (xs.lengthIs(2)) xs.head + " and " + xs.last
    else xs.dropRight(1).mkString(", ") + ", and " + xs.last
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

  def isSubtype(x: java.lang.Class[_], cls: java.lang.Class[_]): Boolean = {
    if ((x == cls) || x.getInterfaces.contains(cls)) true
    else if (x.getSuperclass == null && x.getInterfaces.length == 0) false
    else {
      val superIsSub = if (x.getSuperclass != null) isSubtype(x.getSuperclass, cls) else false
      superIsSub || x.getInterfaces.exists(s=>isSubtype(s,cls))
    }
  }
}
