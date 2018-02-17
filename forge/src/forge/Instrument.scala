package forge

import forge.implicits.terminal._

import scala.collection.mutable

class NoInstrument(override val top: String = "") extends Instrument(top) {
  @inline override def apply[A](name: String)(blk: => A): A = blk

  @inline override def dump(title: => String): Unit = {}
  @inline override def reset(): Unit = {}
}

class Instrument(val top: String = "") {
  var scope: String = top
  val times = new mutable.HashMap[String,Long]()

  @inline def apply[A](name: String)(blk: => A): A = {
    val prev = scope.split('.')
    if (prev.contains(name)) { // Don't double count recursive calls
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

  private def subcats(cat: String, keys: Set[String], depth: Int): Set[String] = {
    keys.filter(key => key.startsWith(cat) && key.count(_ == '.') == depth+1)
  }

  private def dumpCategory(cat: String, keys: Set[String], tab: Int = 0): Unit = {
    val depth = cat.count(_ == '.')
    val parent = cat.split('.').dropRight(1).mkString(".")
    times.get(cat).foreach{time =>
      val parentTime = times.getOrElse(parent, time)

      Console.out.info("  "*tab + s"$cat: ${time/1000.0}s (" + "%.2f".format(time.toDouble/parentTime*100) + "%)")
    }
    subcats(cat, keys, depth).foreach(dumpCategory(_,keys,tab+1))
  }

  @inline def dump(title: => String): Unit = {
    Console.out.info(title)

    val keys = times.keySet.toSet
    if (top != "") {
      val subs = subcats(top,keys,0)
      val total = subs.toList.map{times.getOrElse(_,0L)}.sum
      times(top) = total
      dumpCategory(top, keys + top)
    }
    else {
      val minDepth = keys.map { key => key.count(_ == '.') }.min
      val starts = keys.filter { key => key.count(_ == '.') == minDepth }
      starts.foreach { cat => dumpCategory(cat, keys) }
    }
  }

  @inline def reset(): Unit = {
    scope = top
    times.clear()
  }

}
