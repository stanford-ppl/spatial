package utils

import java.io.PrintStream

import utils.implicits.terminal._
import utils.implicits.collections._

import scala.collection.mutable

class Instrument(val top: String = "") {
  var scope: String = top
  val times = new mutable.HashMap[String,Long]()

  @inline def apply[A](name: String)(blk: => A): A = {
    val prev = scope.split('.')
    if (prev.contains(name)) { // Don't double count recursive calls (we're already timing this)
      blk
    }
    else {
      val outerScope = scope
      val fullName = if (scope == "") name else scope + "." + name
      val startTime = System.currentTimeMillis()
      scope = fullName
      val result = blk
      scope = outerScope
      val endTime = System.currentTimeMillis()
      times(fullName) = times.getOrElse(fullName, 0L) + (endTime - startTime)
      result
    }
  }

  private def subcats(cat: String, keys: Iterable[String], depth: Int): Iterable[String] = {
    keys.filter(key => key.startsWith(cat) && key.count(_ == '.') == depth+1)
        .toSeq.sortBy(key => -times.getOrElse(key,-1L) )
  }

  private def dumpCategory(cat: String, keys: Iterable[String])(implicit out: PrintStream): Unit = {
    val depth = cat.count(_ == '.')
    val parent = cat.split('.').dropRight(1).mkString(".")
    val subs = subcats(cat, keys, depth)
    val total = subs.map(times.getOrElse(_,0L)).sum
    val time = Math.max(times.getOrElse(cat,0L), total)
    times(cat) = time
    val parentTime = if (depth == 0) time else times.getOrElse(parent, time)
    out.info(/*"  "*depth +*/ s"$cat: ${time/1000.0}s (" + "%.2f".format(time.toDouble/parentTime*100) + "%)")
    subs.foreach(dumpCategory(_,keys))
  }

  private def topKeys: Iterable[String] = top match {
    case "" =>
      val keys = times.keys
      val minDepth = keys.map{key => key.count(_ == '.') }.minOrElse(0)
      keys.filter{key => key.count(_ == '.') == minDepth }

    case t => Seq(t)
  }
  def totalTime: Long = topKeys.map{times.apply}.maxOrElse(0)

  def dump(title: => String, out: PrintStream = Console.out): Unit = {
    out.info(title)
    val top = topKeys
    val keys = times.keys ++ top
    top.foreach{cat => dumpCategory(cat, keys)(out) }
  }

  @inline def reset(): Unit = {
    scope = top
    times.clear()
  }

}
