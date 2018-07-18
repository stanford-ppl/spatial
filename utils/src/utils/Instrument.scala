package utils

import java.io.PrintStream

import utils.implicits.terminal._
import utils.implicits.collections._

import scala.collection.mutable

class Instrument(val top: String = "") {
  var scope: String = top
  val times = new mutable.HashMap[String,Long]()

  def add(i2: Instrument): Unit = {
    i2.times.foreach{case (k,v) => times += k -> (v + times.getOrElse(k, 0L)) }
  }

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
      try {
        blk
      }
      finally {
        scope = outerScope
        val endTime = System.currentTimeMillis()
        times(fullName) = times.getOrElse(fullName, 0L) + (endTime - startTime)
      }
    }
  }

  def fix(keys: Iterable[String]): Unit = {
    val cats = keys.map(_.split("."))
    val max = keys.map(_.length).maxOrElse(1)
    for (i <- max until 1 by -1) {
      val cs = cats.flatMap{parts => if (parts.length >= i) Some(parts.take(i)) else None }

      cs.foreach{top =>
        val cat = top.mkString(".")
        val entries = cats.filter(_.startsWith(top)).map(_.mkString("."))
        val total = entries.map{e => times(e) }.sum

        if (!times.contains(cat) || times(cat) < total) times += cat -> total
      }
    }
  }

  private def subcats(cat: String, keys: Iterable[String], depth: Int): Iterable[String] = {
    keys.filter(key => key.startsWith(cat) && key.count(_ == '.') == depth+1)
        .toSeq.sortBy(key => -times(key) )
  }

  private def dumpCategory(cat: String, keys: Iterable[String])(implicit out: PrintStream): Unit = {
    val depth = cat.count(_ == '.')
    val parent = cat.split('.').dropRight(1).mkString(".")
    val time = times.getOrElse(cat,0L)
    val subs = subcats(cat, keys, depth)
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
    fix(times.keySet)
    out.info(title)
    val top = topKeys
    val keys = times.keys ++ top
    top.foreach{cat => dumpCategory(cat, keys)(out) }
  }

  def dumpAll(out: PrintStream = Console.out): Unit = {
    times.foreach{case (name,time) => out.println(s"$name: ${time/1000.0}s") }
  }

  @inline def reset(): Unit = {
    scope = top
    times.clear()
  }

}
